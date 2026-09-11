"""상점 상품 가치 평가. 하루 소득 계산과 산정 흐름을 맡는다.

AI 는 `value`(아이 하루 소득의 배수)만 답하고 `price = value * 하루 소득` 은 코드가 계산한다.
기준 금액이 하루 소득이라는 점이 할일과 다르다 — 할일은 주간 기본 용돈을 그대로 쓴다
(`app.domains.work.services`). 그래서 `daily_income()` 이 `app.core.points` 가 아니라
이 모듈에 있다.

책임을 나눈 자리

    prompt.py      LLM 에 보낼 메시지 조립
    repository.py  store_products 조회·쓰기 (SQL 이 그 파일 밖으로 안 나간다)
    app/llm/       호출(client)과 응답 처리(response) — 도메인 공통
    services.py    하루 소득 계산, 위 조각들을 잇는 흐름 (이 파일)

`evaluate_product()` 는 DB 를 안 건드리고 AI 호출과 계산만 한다 — 프롬프트를 눌러 볼 때 쓴다.
`price_product()` 가 조회부터 저장까지 전체 경로다.

**폴백이 없다.** 실패하면 `None` 을 돌려주고 로그를 남긴다. 카테고리 기본값 같은 것을 깔면
실패가 조용히 묻혀 프롬프트를 언제 고칠지 알 수 없다. 대신 `value`/`price` 가 NULL 로 남아
구매가 `PRICE_NOT_EVALUATED` 로 막힌다.

근거 문구(`reason`)는 담을 컬럼이 없어 저장하지 않는다 (`docs/db-boundary.md` 협의 3번).
로그와 API 응답에만 실린다. 실험 기록은 노션 「소비 측면 > 실험 기록」 에 있다.
"""

import json
import logging
from dataclasses import dataclass
from decimal import Decimal

from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.points import clamp_value, to_points
from app.domains.shopping.prompt import build_messages
from app.domains.shopping.repository import load_context, save_valuation
from app.llm.client import get_client
from app.llm.response import strip_fence, token_kwarg

logger = logging.getLogger(__name__)

DAYS_PER_WEEK = Decimal(7)

# 창작이 아니라 환산이다. 같은 자전거가 오늘 90, 내일 7 이 되면 부모에게 설명할 수 없다.
TEMPERATURE = 0
# 실측 출력이 40 토큰대다. 상한은 과금이 아니라 잘림 방지용이라 넉넉히 둔다.
MAX_OUTPUT_TOKENS = 300


@dataclass(frozen=True)
class Valuation:
    """산정 결과. `value` 와 `price` 는 `store_products` 의 같은 이름 컬럼에 그대로 들어간다."""

    value: Decimal
    price: int
    reason: str


def daily_income(weekly_reward_limit: int, weekly_allowance: int) -> Decimal:
    """하루 소득 = (주간 성과 한도 + 주간 기본 용돈) / 7. 단위는 포인트.

    기획 문서의 `D = (A + B) / 7` 이다. 글자 대신 컬럼명을 쓴다 — 문서를 거치는 사이 A·B 가
    가리키는 것이 바뀐 적이 있어서, 글자를 코드로 옮기면 어느 기준인지 매번 확인해야 한다.

    `float` 이 아니라 `Decimal` 을 쓴다. 3000/7 = 428.571428... 을 가중치에 곱한 뒤 정수로
    반올림하는데, float 오차가 반올림 경계에서 ±1 을 만들면 부모에게 설명할 수 없는 가격이 된다.
    """
    return (Decimal(weekly_reward_limit) + Decimal(weekly_allowance)) / DAYS_PER_WEEK


def has_income(weekly_reward_limit: int, weekly_allowance: int) -> bool:
    """산정을 시도할 가치가 있는지. 둘 다 0 이면 False.

    `families` 의 두 금액은 `INTEGER DEFAULT 0 NOT NULL` 이라, 온보딩을 아직 채우지 않은
    가족은 비정상이 아니라 정상적으로 0 이다. 하루 소득이 0 이면 정직한 가격이 없다 —
    가중치를 얼마로 매겨도 포인트가 0 이 되어 `CHECK (price > 0)` 에 걸린다.

    이때는 LLM 을 호출하지 않고 빠져나온다. 프롬프트에 "소득이 없으면 답하지 말라"고 쓰는
    것과 다르다. 부탁은 새고, 호출을 안 하는 것은 안 샌다.
    """
    return weekly_reward_limit + weekly_allowance > 0


def evaluate_product(
    name: str,
    description: str | None,
    category: str,
    *,
    weekly_reward_limit: int,
    weekly_allowance: int,
    model: str | None = None,
) -> Valuation | None:
    """AI 호출과 가격 계산만 한다. DB 를 건드리지 않는다. 실패하면 None."""
    if not has_income(weekly_reward_limit, weekly_allowance):
        logger.warning(
            "event=skipped cause=non_positive_daily_income reward_limit=%s allowance=%s",
            weekly_reward_limit,
            weekly_allowance,
        )
        return None

    model = model or get_settings().openai_model
    income = daily_income(weekly_reward_limit, weekly_allowance)

    try:
        response = get_client().chat.completions.create(
            model=model,
            temperature=TEMPERATURE,
            response_format={"type": "json_object"},
            messages=build_messages(name, description, category),
            **token_kwarg(model, MAX_OUTPUT_TOKENS),
        )
        payload = json.loads(strip_fence(response.choices[0].message.content or ""))
        raw_value = Decimal(str(payload["value"]))
    except Exception:
        # LLM 장애, JSON 깨짐, 타입 이상을 한자리에서 받는다. 갈래를 나눠도 처리가 같다.
        # bool 을 따로 막지 않는 이유: Decimal(str(True)) 가 InvalidOperation 을 던진다.
        logger.exception("event=failed cause=llm_or_parse model=%s category=%s", model, category)
        return None

    value = clamp_value(raw_value)
    if value is None:
        # 범위 밖은 깎지 않고 버린다. 아이스크림에 5000 을 답한 모델은 단위를 잘못 이해한
        # 것이고, 상한으로 깎아 저장하면 틀린 값을 그럴듯하게 만들어 준다.
        logger.warning("event=failed cause=value_out_of_range value=%s category=%s", raw_value, category)
        return None

    price = to_points(value, income)
    reason = str(payload.get("reason") or "").strip()
    logger.info(
        "event=priced category=%s value=%s price=%s daily_income=%.2f model=%s reason=%s",
        category, value, price, income, model, reason,
    )
    return Valuation(value=value, price=price, reason=reason)


def price_product(session: Session, store_product_id: int) -> Valuation | None:
    """상품 하나를 조회해 산정하고 `store_products` 에 저장한다. 실패하면 None.

    호출하는 쪽(큐 컨슈머 또는 복구 엔드포인트)은 None 이면 그냥 넘어가면 된다. 두 컬럼이
    NULL 로 남아 "가격 산정 중" 상태가 유지되고, 사유는 로그에 있다.
    """
    context = load_context(session, store_product_id)
    if context is None:
        return None

    valuation = evaluate_product(
        context.name,
        context.description,
        context.category,
        weekly_reward_limit=context.weekly_reward_limit,
        weekly_allowance=context.weekly_allowance,
    )
    if valuation is None:
        return None

    if not save_valuation(session, store_product_id, valuation.value, valuation.price):
        return None
    session.commit()

    logger.info(
        "event=saved store_product_id=%s value=%s price=%s",
        store_product_id, valuation.value, valuation.price,
    )
    return valuation
