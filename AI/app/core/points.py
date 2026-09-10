"""가중치 -> 포인트 환산 공통 규칙.

상점 가격과 할일 보상은 **기준 금액이 서로 다르다**. 상점은 하루 소득을 쓰고
(`price = value * daily_income`, `app.domains.shopping.services`), 할일은 주간 기본
용돈을 쓴다 (`reward_points = value * weekly_allowance`, 백엔드
`ChoreRewardEvaluationMessage` javadoc 기준).

그래서 이 모듈에는 **기준 금액을 구하는 코드를 두지 않는다.** 두 도메인이 공유할 수 있는
것은 기준 금액이 아니라 가중치를 정수 포인트로 떨어뜨리는 방법뿐이고, 그 부분만 여기 있다.
기준 금액은 각 도메인이 정해서 `to_points()` 에 넘긴다.

DB 제약이 이 모듈의 상수를 결정한다 (`backend/docker/initdb/01-schema.sql`).

    CK_STORE_PRODUCTS_VALUE_POSITIVE  CHECK ("value" IS NULL OR "value" > 0)
    CK_STORE_PRODUCTS_PRICE_POSITIVE  CHECK ("price" IS NULL OR "price" > 0)

"0 이상"이 아니라 "0 초과"다. 0 은 DB 가 거부하므로 하한이 0 이 아니고, 포인트도
최소 1 이다.
"""

from decimal import ROUND_HALF_UP, Decimal

# CHECK (value > 0). 둘째 자리로 반올림해도 0 이 되지 않는 최소값.
MIN_VALUE = Decimal("0.05")
# 1년치. 기획 최대 예시(자전거 = 3달 = 90)의 4배 여유를 뒀다.
MAX_VALUE = Decimal("365")
# store_products.value / chores.value 가 NUMERIC(10,2) 다.
VALUE_STEP = Decimal("0.01")
# INTEGER 상한(2,147,483,647) 오버플로 방어.
MAX_POINTS = 1_000_000_000


def clamp_value(raw: Decimal) -> Decimal | None:
    """가중치를 검증하고 둘째 자리로 양자화한다. 쓸 수 없으면 None.

    범위를 벗어난 값을 잘라내지 않고 버리는 이유: 아이스크림에 5000 을 답한 모델은
    단위 자체를 잘못 이해한 것이다. 365 로 깎아서 저장하면 틀린 값을 그럴듯하게 만들어 준다.
    버리고 로그를 남기는 편이 낫다.
    """
    if not raw.is_finite():          # NaN / Infinity 는 JSON 모드로도 새어 들어온다
        return None
    if not (MIN_VALUE <= raw <= MAX_VALUE):
        return None
    return raw.quantize(VALUE_STEP, rounding=ROUND_HALF_UP)


def to_points(value: Decimal, unit_amount: Decimal) -> int:
    """가중치 * 기준 금액 -> 정수 포인트.

    `unit_amount` 는 도메인이 정한다 — 상점은 `daily_income(...)`, 할일은
    `Decimal(weekly_allowance)` 를 넘긴다.

    `ROUND_HALF_UP` 을 쓴다. 파이썬 내장 `round()` 는 은행가 반올림이라
    `round(0.5) == 0`, `round(2.5) == 2` 가 되고, 작은 상품에서 포인트가 0 이 되어
    `CHECK (price > 0)` 을 위반한다. 여기서 가장 쉽게 나오는 버그다.

    `max(1, ...)` 도 같은 제약 때문이다. 곱한 값이 0.5 미만이면 0 으로 반올림된다.

    **양자화된 가중치를 넘겨야 한다.** 부모 화면이 가중치와 포인트를 같이 보여주는데
    백엔드에는 다시 계산할 수식이 없다. `포인트 == round(저장된 가중치 * 기준 금액)` 이
    정확히 성립해야 한다.
    """
    raw = (value * unit_amount).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
    return max(1, min(int(raw), MAX_POINTS))
