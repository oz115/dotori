"""할 일 보상 책정. AI 가중치를 받아 reward_points 를 계산하고 chores 에 쓴다.

AI 는 value(가중치)만 답하고 `reward_points = value * weekly_allowance` 는 코드가 계산한다.
기준 금액이 상점과 다르다 — 상점은 하루 소득, 할 일은 **주간 기본 용돈**이다
(노션 「AI 기능 정리」, 백엔드 `ChoreRewardEvaluationMessage` javadoc). 그래서 `to_points()`
에 넘기는 기준 금액이 다르고, 상점의 `daily_income()` 같은 함수가 여기엔 없다.

difficulty 밴드가 이 도메인의 핵심이다. 부모가 고른 난이도(EASY/NORMAL/HARD)가 값의 범위를
정하고, AI 는 그 안에서 구체적 할 일을 보고 값을 정한다. 난이도는 부모가 고른 신뢰할 수 있는
입력이라, AI 가 밴드를 벗어난 값을 내면 **버리지 않고 밴드 경계로 clamp** 한다 (상점은 전역
범위 밖을 버렸지만, 여기선 난이도가 곧 정답 범위다).

책임 분리는 상점과 같다.

    prompt.py      LLM 에 보낼 메시지 조립
    repository.py  chores 조회·쓰기
    app/llm/       호출(client)과 응답 처리(response) — 도메인 공통
    services.py    밴드 clamp, reward_points 계산, 흐름 (이 파일)

`evaluate_chore()` 는 DB 없이 AI 호출·계산만 한다. `reward_chore()` 가 조회부터 저장까지다.

**폴백이 없다.** LLM 실패나 파싱 실패면 None 을 돌려주고 로그를 남긴다. 근거(reason)는 담을
컬럼이 없어 저장하지 않는다 (`docs/db-boundary.md`). 로그와 API 응답에만 실린다.
"""

import json
import logging
from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal, InvalidOperation

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.points import to_points
from app.domains.work.prompt import build_messages
from app.domains.work.repository import load_context, save_reward
from app.llm.client import get_client
from app.llm.response import strip_fence, token_kwarg

logger = logging.getLogger(__name__)

TEMPERATURE = 0
MAX_OUTPUT_TOKENS = 300
VALUE_STEP = Decimal("0.01")  # chores.value 가 NUMERIC(10,2)

# 난이도별 값 밴드. 부모가 고른 난이도가 범위를 정하고 AI 는 그 안에서 값을 낸다.
# 하한은 CHECK (value > 0) 때문에 0 이 아니다. HARD 상한 5.0 은 폭주 방어 — 부모가 큰 성취에
# 크게 보상하는 것은 담되(5주치까지), value=50 같은 모델 오작동은 막는다.
_BANDS: dict[str, tuple[Decimal, Decimal]] = {
    "EASY": (Decimal("0.05"), Decimal("0.20")),
    "NORMAL": (Decimal("0.20"), Decimal("0.40")),
    "HARD": (Decimal("0.40"), Decimal("5.00")),
}


@dataclass(frozen=True)
class Reward:
    """산정 결과. `value` 와 `reward_points` 는 `chores` 의 같은 이름 컬럼에 그대로 들어간다."""

    value: Decimal
    reward_points: int
    reason: str


def clamp_to_band(raw: Decimal, difficulty: str) -> Decimal | None:
    """가중치를 난이도 밴드 안으로 clamp 하고 둘째 자리로 양자화한다. 밴드가 없으면 None.

    상점의 `clamp_value` 와 다르다 — 상점은 전역 범위 밖을 **버렸지만**, 할 일은 난이도가
    부모의 확정 입력이라 밴드가 곧 정답 범위다. AI 가 EASY 에 0.5 를 내도 버리지 않고
    0.20 으로 끌어들인다. 그래야 reward_points 가 NULL 로 남아 보상이 안 붙는 일이 없다.

    difficulty 가 밴드 표에 없으면(스키마 밖의 값) None 을 돌려준다 — 이건 clamp 로 살릴 수
    없는 이상 상황이라 호출자가 버린다.
    """
    band = _BANDS.get(difficulty)
    if band is None:
        return None
    low, high = band
    if not raw.is_finite():  # NaN / Infinity 는 JSON 모드로도 새어 들어온다
        return None
    clamped = min(max(raw, low), high)
    return clamped.quantize(VALUE_STEP, rounding=ROUND_HALF_UP)


def evaluate_chore(
    title: str,
    description: str | None,
    difficulty: str,
    *,
    weekly_allowance: int,
    model: str | None = None,
) -> Reward | None:
    """AI 호출과 보상 계산만 한다. DB 를 건드리지 않는다. 실패하면 None."""
    if weekly_allowance <= 0:
        # families.weekly_allowance 는 INTEGER DEFAULT 0. 온보딩 전 가족은 정상적으로 0 이다.
        # 기준 금액이 0 이면 정직한 보상이 없다 — value 를 얼마로 매겨도 reward_points 가 0 이
        # 되어 CHECK (reward_points > 0) 에 걸린다. LLM 을 부르지 않고 빠져나온다.
        logger.warning("event=skipped cause=non_positive_allowance weekly_allowance=%s", weekly_allowance)
        return None

    model = model or get_settings().openai_model

    try:
        response = get_client().chat.completions.create(
            model=model,
            temperature=TEMPERATURE,
            response_format={"type": "json_object"},
            messages=build_messages(title, description, difficulty),
            **token_kwarg(model, MAX_OUTPUT_TOKENS),
        )
        payload = json.loads(strip_fence(response.choices[0].message.content or ""))
        raw_value = Decimal(str(payload["value"]))
    except Exception:
        # LLM 장애, JSON 깨짐, 타입 이상을 한자리에서 받는다. bool 은 Decimal(str(True)) 가
        # InvalidOperation 을 던져 자동으로 걸린다.
        logger.exception("event=failed cause=llm_or_parse model=%s difficulty=%s", model, difficulty)
        return None

    value = clamp_to_band(raw_value, difficulty)
    if value is None:
        # 난이도가 스키마 밖(EASY/NORMAL/HARD 아님)이라 밴드를 못 찾았다. clamp 로 못 살린다.
        logger.warning("event=failed cause=unknown_difficulty difficulty=%s value=%s", difficulty, raw_value)
        return None

    reward_points = to_points(value, Decimal(weekly_allowance))
    reason = str(payload.get("reason") or "").strip()
    logger.info(
        "event=rewarded difficulty=%s value=%s reward_points=%s weekly_allowance=%s model=%s reason=%s",
        difficulty, value, reward_points, weekly_allowance, model, reason,
    )
    return Reward(value=value, reward_points=reward_points, reason=reason)


def reward_chore(session: Session, chore_id: int) -> Reward | None:
    """할 일 하나를 조회해 산정하고 `chores` 에 저장한다. 실패하면 None.

    호출하는 쪽(큐 컨슈머)은 None 이면 그냥 넘어가면 된다. 두 컬럼이
    NULL 로 남아 "보상 산정 중" 상태가 유지되고, 사유는 로그에 있다.
    """
    context = load_context(session, chore_id)
    if context is None:
        return None

    reward = evaluate_chore(
        context.title,
        context.description,
        context.difficulty,
        weekly_allowance=context.weekly_allowance,
    )
    if reward is None:
        return None

    if not save_reward(session, chore_id, reward.value, reward.reward_points):
        return None
    session.commit()

    logger.info(
        "event=saved chore_id=%s value=%s reward_points=%s",
        chore_id, reward.value, reward.reward_points,
    )
    return reward
