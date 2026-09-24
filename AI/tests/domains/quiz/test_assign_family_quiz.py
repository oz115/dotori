"""일일 크론이 돈 뒤에 합류한 자녀의 퀴즈 배정(`assign_family_quiz`) 테스트."""

from contextlib import contextmanager
from datetime import date
from types import SimpleNamespace
from unittest.mock import patch

import pytest

from app.core.models import Family
from app.domains.quiz import services
from app.domains.quiz.models import FamilyMember, Quiz, User, Word

TODAY = date(2026, 9, 22)
FAMILY_ID = 1
CHILD_IDS = (10, 11)


@pytest.fixture
def family_with_children(db_session):
    """BEGINNER 가족 하나 + 자녀 둘. 단어장은 그 티어(difficulty_level 1)로 채운다."""
    db_session.add(
        Family(
            family_id=FAMILY_ID,
            weekly_allowance=7000,
            weekly_reward_limit=3000,
            financial_knowledge_level="BEGINNER",
        )
    )
    for child_id in CHILD_IDS:
        db_session.add(User(user_id=child_id, role="CHILD", deleted_at=None))
        db_session.add(
            FamilyMember(
                family_member_id=child_id,
                family_id=FAMILY_ID,
                user_id=child_id,
                deleted_at=None,
            )
        )
    for word_id in (1, 2, 3):
        db_session.add(
            Word(
                word_id=word_id,
                word=f"단어{word_id}",
                description="설명입니다.",
                difficulty_level=1,
                category=None,
            )
        )
    db_session.commit()
    return db_session


def _item(word_id: int = 1) -> services.GeneratedQuizItem:
    return services.GeneratedQuizItem(
        word_id=word_id,
        tier="BEGINNER",
        question="도토리 숲에서 여울이가 산 것은 무엇일까요?",
        correct_answer=f"단어{word_id}",
        explanation="설명입니다.",
    )


@contextmanager
def _running(session, pool_fn):
    """테스트 세션과 고정 날짜로 `assign_family_quiz` 를 돌릴 수 있게 갈아끼운다."""
    fixed_now = SimpleNamespace(date=lambda: TODAY)
    with (
        patch.object(services, "SessionLocal", lambda: session),
        patch.object(services, "datetime", SimpleNamespace(now=lambda tz: fixed_now)),
        # 세션은 테스트가 이어서 봐야 하므로 서비스가 닫지 못하게 한다
        patch.object(session, "close", lambda: None),
        patch.object(services, "generate_daily_pool", pool_fn),
    ):
        yield


def _quiz_rows(session) -> list[Quiz]:
    return list(session.query(Quiz).order_by(Quiz.child_id).all())


def test_generates_only_the_family_tier(family_with_children):
    """크론은 9개를 만들지만 여기서는 그 가족 티어 3개만 — 쓰지도 않을 문제에 토큰을 쓰지 않는다."""
    captured: dict = {}

    def pool(session, tiers=None):
        captured["tiers"] = tiers
        return {"BEGINNER": [_item()]}

    with _running(family_with_children, pool):
        result = services.assign_family_quiz(FAMILY_ID)

    assert captured["tiers"] == ("BEGINNER",)
    assert result.tier == "BEGINNER"
    assert result.inserted == 2
    assert result.reused_sibling_quiz is False

    rows = _quiz_rows(family_with_children)
    assert [row.child_id for row in rows] == list(CHILD_IDS)
    # 한 가족은 같은 문제를 푼다 (크론의 배정 규칙과 같다)
    assert len({row.question for row in rows}) == 1


def test_reuses_sibling_quiz_without_calling_llm(family_with_children):
    """형이 이미 오늘 퀴즈를 받았으면 동생은 같은 문제를 받는다 — LLM 을 다시 부르지 않는다."""
    family_with_children.add(
        Quiz(
            family_id=FAMILY_ID,
            child_id=CHILD_IDS[0],
            word_id=2,
            question="형이 이미 받은 문제일까요?",
            correct_answer="단어2",
            explanation="설명입니다.",
            reward_points=10,
            quiz_date=TODAY,
        )
    )
    family_with_children.commit()

    def pool(session, tiers=None):
        raise AssertionError("형제 문제가 있으면 LLM 을 부르면 안 된다")

    with _running(family_with_children, pool):
        result = services.assign_family_quiz(FAMILY_ID)

    assert result.reused_sibling_quiz is True
    assert result.inserted == 1
    assert result.skipped == 1

    rows = _quiz_rows(family_with_children)
    assert [row.child_id for row in rows] == list(CHILD_IDS)
    assert {row.question for row in rows} == {"형이 이미 받은 문제일까요?"}


def test_is_idempotent_when_everyone_already_has_one(family_with_children):
    """같은 가족을 두 번 불러도 행이 늘지 않는다 — 이벤트가 중복 발행돼도 안전하다."""

    def pool(session, tiers=None):
        return {"BEGINNER": [_item()]}

    with _running(family_with_children, pool):
        services.assign_family_quiz(FAMILY_ID)
        second = services.assign_family_quiz(FAMILY_ID)

    assert second.inserted == 0
    assert second.skipped == 2
    assert second.reused_sibling_quiz is True
    assert len(_quiz_rows(family_with_children)) == 2


def test_family_without_children_is_skipped(db_session):
    """부모만 가족을 만든 직후 — 배정할 자녀가 없으니 아무것도 하지 않는다."""
    db_session.add(
        Family(
            family_id=FAMILY_ID,
            weekly_allowance=7000,
            weekly_reward_limit=3000,
            financial_knowledge_level="BEGINNER",
        )
    )
    db_session.commit()

    def pool(session, tiers=None):
        raise AssertionError("자녀가 없으면 LLM 을 부르면 안 된다")

    with _running(db_session, pool):
        result = services.assign_family_quiz(FAMILY_ID)

    assert result.tier is None
    assert result.inserted == 0
    assert _quiz_rows(db_session) == []
