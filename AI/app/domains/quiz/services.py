"""단어장(`words`) 기반 퀴즈 생성 로직. LLM 호출은 `app.llm.client` 를 거친다.

하루 흐름은 두 단계다.

1. `generate_daily_pool()` — 난이도 상/중/하(`financial_knowledge_level` 과 동일한 축)마다
   단어를 뽑고, 전체(티어당 `ITEMS_PER_TIER` 개, 총 9개)를 **LLM 호출 한 번**으로 문제화한다
   (`generate_quiz_batch`). 단어 수만큼 호출하면 시스템 프롬프트가 매번 중복돼 낭비다.
   가족에게 실제로 배정하기 전까지는 메모리에만 있다 (저장할 "문제 풀" 테이블이 없다).
2. `assign_and_insert()` — 가족마다 `families.financial_knowledge_level` 에 해당하는 티어
   풀에서 하나를 랜덤으로 골라, 그 가족의 자녀 전원에게 **같은 문제**를 한 행씩 넣는다.
   (가족 단위로 하나를 뽑되 `quizzes.child_id` 가 NOT NULL 이라 자녀 수만큼 행이 생긴다.)

`quizzes` 에는 유니크 제약이 없어서 재실행 시 중복 삽입을 막는 책임이 이 모듈에 있다
(child_id + quiz_date 존재 여부 확인 후 INSERT). 가족 단위로 실패를 격리한다 — 한 가족의
LLM/DB 오류가 나머지 가족의 생성을 막지 않는다.

`reward_points` 산정식은 아직 노션에 없다. `REWARD_POINTS_BY_TIER` 는 확정 전까지의
임시 고정값이다.
"""

import json
import logging
import random
from collections import defaultdict
from dataclasses import dataclass
from datetime import date, datetime
from zoneinfo import ZoneInfo

from sqlalchemy import String, cast, func, select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import SessionLocal
from app.core.models import Family
from app.domains.quiz.models import FamilyMember, Quiz, User, Word
from app.llm.client import get_client

logger = logging.getLogger(__name__)

KST = ZoneInfo("Asia/Seoul")

# families.financial_knowledge_level 값과 그대로 맞춘다 — 하/중/상이 아니라 이 이름으로 다룬다.
TIER_TO_DIFFICULTY: dict[str, tuple[int, ...]] = {
    "BEGINNER": (1,),
    "INTERMEDIATE": (2, 3),
    "ADVANCED": (4,),
}
ITEMS_PER_TIER = 3

# 임시값. 노션에 산정식이 정해지면 교체한다.
REWARD_POINTS_BY_TIER: dict[str, int] = {
    "BEGINNER": 10,
    "INTERMEDIATE": 20,
    "ADVANCED": 30,
}

# quizzes.correct_answer / explanation 은 VARCHAR(255) 다.
MAX_ANSWER_LENGTH = 255
MAX_EXPLANATION_LENGTH = 255


@dataclass
class GeneratedQuizItem:
    """LLM 이 만든 문제 하나. DB 에 바로 대응하는 컬럼만 담는다."""

    word_id: int
    tier: str
    question: str
    correct_answer: str
    explanation: str | None


def pick_words_for_tier(session: Session, tier: str, count: int = ITEMS_PER_TIER) -> list[Word]:
    """해당 티어의 `difficulty_level` 범위에서 무작위로 `count`개를 뽑는다."""
    levels = TIER_TO_DIFFICULTY[tier]
    stmt = select(Word).where(Word.difficulty_level.in_(levels)).order_by(func.random()).limit(count)
    return list(session.scalars(stmt))


def _build_quiz_item(raw: dict, tier_by_word_id: dict[int, str]) -> GeneratedQuizItem:
    """LLM 응답의 원소 하나를 검증해서 `GeneratedQuizItem` 으로 만든다.

    실패하면 `KeyError`/`ValueError` 를 올린다 — 호출부가 이 항목 하나만 건너뛴다.
    """
    word_id = raw["word_id"]
    tier = tier_by_word_id[word_id]  # LLM 이 엉뚱한 word_id 를 답하면 여기서 KeyError
    question = raw["question"]
    correct_answer = raw["correct_answer"]
    explanation = raw.get("explanation")

    if len(correct_answer) > MAX_ANSWER_LENGTH:
        raise ValueError(f"correct_answer 가 {MAX_ANSWER_LENGTH}자를 넘습니다 (word_id={word_id})")
    if explanation and len(explanation) > MAX_EXPLANATION_LENGTH:
        raise ValueError(f"explanation 이 {MAX_EXPLANATION_LENGTH}자를 넘습니다 (word_id={word_id})")

    return GeneratedQuizItem(
        word_id=word_id,
        tier=tier,
        question=question,
        correct_answer=correct_answer,
        explanation=explanation,
    )


def generate_quiz_batch(words: list[Word], tier_by_word_id: dict[int, str]) -> list[GeneratedQuizItem]:
    """단어 여러 개를 **LLM 호출 한 번**으로 처리한다.

    단어마다 따로 호출하면 개수만큼 왕복이 늘어난다 — 시스템 프롬프트도 매번 새로 보내야
    해서 비용·지연 모두 낭비다. 대신 한 번에 다 물어보고, 응답 배열의 원소 하나가
    이상해도 그 항목만 건너뛴다(`_build_quiz_item`). 호출 자체(인증/네트워크/최상위 JSON
    파싱)가 실패하면 이 함수 전체가 예외를 올린다 — 그건 9개 전부를 잃는 대신 상위에서
    막지 않고 그대로 드러낸다.
    """
    settings = get_settings()
    client = get_client()
    word_lines = "\n".join(
        f"- word_id={word.word_id}, 단어={word.word}, 뜻={word.description}, 난이도={word.difficulty_level}"
        for word in words
    )
    response = client.chat.completions.create(
        model=settings.openai_model,
        response_format={"type": "json_object"},
        messages=[
            {
                "role": "system",
                "content": (
                    "너는 초등학생에게 경제 개념을 가르치는 퀴즈 출제자다. "
                    "아래 목록의 경제 용어마다 하나씩, 그 용어를 소재로 한 짧은 가상 이야기와 "
                    "이야기가 가리키는 용어를 맞히는 문제를 만든다. "
                    "JSON 객체 하나로만 답한다: "
                    '{"items": [{"word_id": 입력에 준 word_id 그대로, '
                    '"question": "이야기와 질문을 합친 문장", '
                    '"correct_answer": "정답 단어", '
                    '"explanation": "정답 해설"}, ...]}. '
                    "items 배열은 입력 단어 개수와 정확히 같은 수만큼 만든다. "
                    f"correct_answer 는 {MAX_ANSWER_LENGTH}자, "
                    f"explanation 은 {MAX_EXPLANATION_LENGTH}자를 넘지 않는다."
                ),
            },
            {"role": "user", "content": word_lines},
        ],
    )
    content = response.choices[0].message.content
    if content is None:
        raise ValueError("LLM 응답이 비었습니다")

    items_raw = json.loads(content)["items"]

    results: list[GeneratedQuizItem] = []
    for raw in items_raw:
        try:
            results.append(_build_quiz_item(raw, tier_by_word_id))
        except (KeyError, ValueError):
            logger.exception("퀴즈 항목 하나를 건너뜁니다 (raw=%s)", raw)
    return results


def generate_daily_pool(session: Session) -> dict[str, list[GeneratedQuizItem]]:
    """티어마다 단어를 뽑고, 전체를 LLM 호출 한 번으로 문제화한다."""
    words_by_tier = {tier: pick_words_for_tier(session, tier) for tier in TIER_TO_DIFFICULTY}
    tier_by_word_id = {word.word_id: tier for tier, words in words_by_tier.items() for word in words}
    all_words = [word for words in words_by_tier.values() for word in words]

    items = generate_quiz_batch(all_words, tier_by_word_id)

    pool: dict[str, list[GeneratedQuizItem]] = {tier: [] for tier in TIER_TO_DIFFICULTY}
    for item in items:
        pool[item.tier].append(item)
    return pool


def iter_children_with_tier(session: Session) -> list[tuple[int, int, str]]:
    """(family_id, child_id, financial_knowledge_level) 목록.

    `users` 에는 `family_id` 가 없어 `family_members` 를 거쳐야 한다
    (`docs/db-boundary.md` 읽기 권한 참고).

    `role` 은 네이티브 ENUM(`users_role`) 이라 그냥 문자열과 비교하면
    `operator does not exist: users_role = character varying` 로 실패한다.
    `String` 매핑이 바인드 파라미터에 `::VARCHAR` 캐스트를 붙이기 때문이다 — 컬럼 쪽을
    `cast(..., String)` 으로 텍스트화해서 비교한다.
    """
    stmt = (
        select(FamilyMember.family_id, User.user_id, Family.financial_knowledge_level)
        .join(User, User.user_id == FamilyMember.user_id)
        .join(Family, Family.family_id == FamilyMember.family_id)
        .where(cast(User.role, String) == "CHILD", User.deleted_at.is_(None), FamilyMember.deleted_at.is_(None))
    )
    return list(session.execute(stmt).all())


def _quiz_already_exists(session: Session, child_id: int, today: date) -> bool:
    stmt = select(Quiz.quiz_id).where(Quiz.child_id == child_id, Quiz.quiz_date == today)
    return session.execute(stmt).first() is not None


@dataclass
class AssignResult:
    """`assign_and_insert` 결과 요약. 엔드포인트 응답에 그대로 쓴다."""

    inserted: int = 0
    skipped: int = 0
    failed_families: int = 0


@dataclass
class DailyGenerationResult:
    """`generate_daily_quizzes` 결과 요약."""

    pool_size: int
    inserted: int
    skipped: int
    failed_families: int


def assign_and_insert(session: Session, pool: dict[str, list[GeneratedQuizItem]], today: date) -> AssignResult:
    """가족마다 자기 티어 풀에서 1개를 뽑아 그 가족의 자녀 전원에게 넣는다.

    가족 단위로 커밋/롤백한다 — 한 가족에서 예외가 나도 이미 처리한 다른 가족의 결과는
    남는다.
    """
    result = AssignResult()

    family_children: dict[int, list[int]] = defaultdict(list)
    family_tier: dict[int, str] = {}
    for family_id, child_id, tier in iter_children_with_tier(session):
        family_children[family_id].append(child_id)
        family_tier[family_id] = tier

    for family_id, child_ids in family_children.items():
        tier = family_tier[family_id]
        candidates = pool.get(tier) or []
        if not candidates:
            logger.warning("가족 %s: %s 티어에 배정할 문제가 없어 건너뜁니다", family_id, tier)
            continue

        chosen = random.choice(candidates)
        try:
            for child_id in child_ids:
                if _quiz_already_exists(session, child_id, today):
                    logger.info("이미 오늘자 퀴즈가 있어 건너뜁니다 (child_id=%s)", child_id)
                    result.skipped += 1
                    continue
                session.add(
                    Quiz(
                        family_id=family_id,
                        child_id=child_id,
                        word_id=chosen.word_id,
                        question=chosen.question,
                        correct_answer=chosen.correct_answer,
                        explanation=chosen.explanation,
                        reward_points=REWARD_POINTS_BY_TIER[tier],
                        quiz_date=today,
                    )
                )
                result.inserted += 1
            session.commit()
        except Exception:
            session.rollback()
            result.failed_families += 1
            logger.exception("가족 %s 퀴즈 삽입 실패, 다음 가족으로 넘어갑니다", family_id)

    return result


def generate_daily_quizzes() -> DailyGenerationResult:
    """하루치 퀴즈 생성 + 배정 진입점.

    `scripts/generate_daily_quizzes.py`(수동 실행)와 `router.generate_today()`(내부 API)가
    둘 다 이 함수 하나를 호출한다 — 트리거가 늘어나도 실제 로직은 여기 하나뿐이다.
    """
    today = datetime.now(KST).date()
    session = SessionLocal()
    try:
        pool = generate_daily_pool(session)
        pool_size = sum(len(items) for items in pool.values())
        logger.info("오늘(%s)의 문제 풀 생성 완료: %d개", today, pool_size)
        assign_result = assign_and_insert(session, pool, today)
        return DailyGenerationResult(
            pool_size=pool_size,
            inserted=assign_result.inserted,
            skipped=assign_result.skipped,
            failed_families=assign_result.failed_families,
        )
    finally:
        session.close()
