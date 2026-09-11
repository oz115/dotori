"""상점 상품 가치 평가 로직. LLM 호출은 `app.llm.client` 를 거친다.

기준 금액이 **하루 소득**이라는 점이 할일과 다르다. 할일은 주간 기본 용돈을 그대로 쓴다
(`app.domains.work.services` 참고). 그래서 아래 두 함수는 `app.core.points` 가 아니라
이 모듈에 있다 — 하루 소득을 쓰는 곳은 상점뿐이다.

산정 순서 (`AI/notebooks/상점_가격_책정.ipynb` 에서 프롬프트까지 눌러 봤다):

1. `has_income()` 으로 기준 금액이 0 인 가족을 먼저 걸러낸다
2. `daily_income()` 으로 하루 소득을 구한다
3. LLM 이 `value`(하루 소득의 배수)와 근거 문구를 답한다.
   **하루 소득을 프롬프트에 넣지 않는다** — 돈 계산을 모델에게 맡기면 검증할 수 없고,
   하루 소득이 가족마다 달라 few-shot 앵커가 가족별로 다른 의미가 된다
4. `clamp_value()` 로 범위를 벗어난 값을 버리고, `to_points()` 로 `price` 를 만든다
5. `value IS NULL` 조건을 건 UPDATE 로 쓴다. 중복 호출이 무해해진다

근거 문구는 저장하지 않는다. 담을 컬럼이 없다 (`docs/db-boundary.md` 협의 3번). 로그에 남긴다.
"""

from decimal import Decimal

DAYS_PER_WEEK = Decimal(7)


def daily_income(weekly_reward_limit: int, weekly_allowance: int) -> Decimal:
    """하루 소득 = (주간 성과 한도 + 주간 기본 용돈) / 7. 단위는 포인트.

    기획 문서의 `D = (A + B) / 7` 이다. 글자 대신 컬럼명을 쓴다 — 문서를 거치는 사이
    A·B 가 가리키는 것이 바뀐 적이 있어서, 글자를 코드로 옮기면 어느 기준인지 매번
    확인해야 한다. 컬럼명은 하나뿐이라 헷갈릴 여지가 없다.

    `float` 이 아니라 `Decimal` 을 쓴다. 3000/7 = 428.571428... 을 가중치에 곱한 뒤
    정수로 반올림하는데, float 오차가 반올림 경계에서 ±1 을 만들면 부모에게 설명할 수 없는
    가격이 된다.

    두 값이 모두 0 이면 0 을 돌려준다. 호출하는 쪽에서 `has_income()` 으로 먼저 걸러야 한다.
    """
    return (Decimal(weekly_reward_limit) + Decimal(weekly_allowance)) / DAYS_PER_WEEK


def has_income(weekly_reward_limit: int, weekly_allowance: int) -> bool:
    """산정을 시도할 가치가 있는지. 둘 다 0 이면 False.

    `families` 의 두 금액은 `INTEGER DEFAULT 0 NOT NULL` 이라, 온보딩을 아직 채우지 않은
    가족은 비정상이 아니라 정상적으로 0 이다. 하루 소득이 0 이면 정직한 가격이 없다 —
    가중치를 얼마로 매겨도 포인트가 0 이 되어 `CHECK (price > 0)` 에 걸린다.

    이때는 LLM 을 호출하지 않고 빠져나온다. 프롬프트에 "소득이 없으면 답하지 말라"고
    쓰는 것과 다르다. 부탁은 새고, 호출을 안 하는 것은 안 샌다.
    """
    return weekly_reward_limit + weekly_allowance > 0
