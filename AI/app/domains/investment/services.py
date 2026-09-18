"""AI 생성 뉴스 기반 투자 상품 시세 변동 로직. LLM 호출은 `app.llm.client` 를 거친다.

설계는 노션 「투자 (Investment)」와 「회사 리스트」가 원본이다.

- **1 경제요인 : 1 회사.** 회사 10개에 대표 요인이 하나씩 붙고 요인은 겹치지 않는다
  (기온 -> 시원아이스, 밀 가격 -> 고소한빵, 리튬/니켈 가격 -> 파워배터리 ...).
  그래서 뉴스 한 건이 움직이는 회사도 하나다 (`models.InvestmentNewsProduct` 주석 참고).
- **하루 뉴스 3건.** 상승 2건 + 하락 1건이라 하루 순증이 1건, 일주일에 7회 오른다.
- **주가 변동폭** = 투자에 배정된 금액 / 7. 배정 금액은 `weekly_reward_limit` 의 40% 다.
  상승·하락 폭이 같은 값이라, 위 3건 구성이 곧 주간 수익 한도를 만든다.
- **주말 청산.** 한 주가 끝나면 안 팔린 것도 전부 청산한다.

`weekly_reward_limit` 배분은 예적금 40%(노션 「예/적금 상품 표」), 투자 40% 다.
할 일은 이 배분에서 빠지고 주간 기본 용돈 대비로 책정한다 (노션 「AI 기능 정리」).
"""

import random
from datetime import datetime, time, timedelta
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path
from zoneinfo import ZoneInfo

from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.models import Family
from app.domains.investment.models import InvestmentNews, InvestmentNewsProduct, InvestmentPriceHistory, InvestmentProduct
from app.llm.client import get_client

KST = ZoneInfo("Asia/Seoul")
DAYS_PER_WEEK = Decimal(7)
# `weekly_reward_limit` 중 투자에 배정되는 몫 (예적금 40%와 합쳐 80%, 나머지는 퀴즈).
INVESTMENT_SHARE = Decimal("0.4")
# 가격 하한 = 회사 시작가(seed `current_price`)의 몇 %.
PRICE_FLOOR_RATIO = Decimal("0.1")

_IMPACT_CONTEXT_KEYS = {
    "POSITIVE": ("회사에 유리한 변화", "상승 예시"),
    "NEGATIVE": ("회사에 불리한 변화", "하락 예시"),
}
_NEWS_FORBIDDEN_TERMS_BY_COMPANY = {
    "시원아이스": ("아이스크림", "빙과"),
    "푸른들농업": ("농업", "농부", "농작물", "밀", "채소"),
    "두근여행": ("여행", "관광", "예약", "숙박"),
    "신나는월드": ("놀이공원", "놀이기구", "방문객"),
    "하늘항공": ("항공", "비행기", "운항", "승객", "항공유"),
    "번개택배": ("택배", "배송", "물류", "상자"),
    "튼튼건설": ("건설", "시공", "분양"),
    "고소한빵": ("빵", "제빵", "밀가루"),
    "씽씽자동차": ("자동차 회사", "차량 생산"),
    "파워배터리": ("배터리", "전기차"),
}

# 프롬프트 문구는 코드와 분리해서 `prompts/` 밑에 둔다 — 톤·규칙을 다듬을 때 코드를
# 안 건드리고 이 파일만 고치면 된다.
_PROMPTS_DIR = Path(__file__).parent / "prompts"
_SYSTEM_PROMPT = (_PROMPTS_DIR / "news_system_prompt.md").read_text(encoding="utf-8").strip()
_NEWS_RESPONSE_FORMAT = {
    "type": "json_schema",
    "json_schema": {
        "name": "investment_news",
        "strict": True,
        "schema": {
            "type": "object",
            "properties": {
                "title": {"type": "string"},
                "content": {"type": "string"},
            },
            "required": ["title", "content"],
            "additionalProperties": False,
        },
    },
}


class _NewsDraft(BaseModel):
    title: str
    content: str


def _context_value(news_context: str, keys: tuple[str, ...]) -> str | None:
    """`news_context`의 `키: 값` 줄에서 첫 번째 일치 값을 꺼낸다."""
    for line in news_context.splitlines():
        key, separator, value = line.partition(":")
        if separator and key.strip() in keys and value.strip():
            return value.strip()
    return None


def _build_news_user_prompt(news_context: str, impact: str, forbidden_terms: tuple[str, ...] = ()) -> str:
    """회사 식별 정보 없이 선택된 경제 사건 조건만 담은 사용자 프롬프트를 만든다.

    이미 적재된 DB의 이전 형식도 읽을 수 있도록 `상승 예시`/`하락 예시`를 보조 키로
    허용한다. 새 형식은 완성된 예시 문장이 아니라 외부 경제 요인의 변화만 저장한다.
    """
    economic_factor = _context_value(news_context, ("대표 경제 요인",))
    condition = _context_value(news_context, _IMPACT_CONTEXT_KEYS[impact])
    topics = _context_value(news_context, ("뉴스 소재",))
    if economic_factor is None or condition is None:
        raise ValueError("뉴스 참고자료에 대표 경제 요인 또는 사건 조건이 없다")

    prompt_lines = [
        f"대표 경제 요인: {economic_factor}",
        f"선택된 사건 조건: {condition}",
    ]
    if topics is not None:
        prompt_lines.append(f"사용 가능한 뉴스 소재: {topics}")
    if forbidden_terms:
        prompt_lines.extend(
            [
                f"출력 금지어: {', '.join(forbidden_terms)}",
                "출력 금지어와 그 활용형을 제목과 본문에 사용하지 마세요.",
            ]
        )
    prompt_lines.extend(
        [
            "",
            "위 조건에 맞는 외부 경제 사건 하나만 작성해 주세요.",
            "회사에 미치는 영향이나 정답은 설명하지 마세요.",
        ]
    )
    return "\n".join(prompt_lines)


def week_start_kst(now: datetime) -> datetime:
    """`now` 가 속한 주의 월요일 00:00 KST.

    로테이션 주기·청산 주기 둘 다 이 경계를 쓴다 (설계 문서 "5. 주간 사이클").
    `now` 는 aware datetime 이어야 한다 — naive 를 넘기면 시스템 로컬 타임존으로
    잘못 해석될 수 있다.
    """
    kst_now = now.astimezone(KST)
    monday = kst_now.date() - timedelta(days=kst_now.weekday())
    return datetime.combine(monday, time.min, tzinfo=KST)


def select_todays_companies(session: Session, now: datetime) -> tuple[list[InvestmentProduct], InvestmentProduct]:
    """오늘 뉴스 3건이 가리킬 회사를 정한다. 셔플-백 로테이션.

    실제로 셔플한 순서를 큐에 저장해 두는 대신, 이번 주(월요일 00:00 KST~)에 이미 몇 번
    나왔는지를 `investment_news_products` 이력에서 매번 역산한다. 서버가 재시작돼도
    상태가 안 날아가고, 로테이션 전용 상태 테이블도 안 만든다.

    이번 주 등장 횟수가 적은 회사부터 우선 뽑는다 — 매주 초기화되는 셔플-백과 같은
    효과(등장 안 한 회사가 계속 먼저 뽑힘)를 내면서, 동률 안에서는 무작위로 섞는다.
    한 번의 호출 안에서 뽑으므로 하루 안 중복은 자동으로 없다.

    반환값은 (상승 2개, 하락 1개) — 뽑힌 3개 중 무작위로 1개를 하락으로 배정한다.
    """
    week_start = week_start_kst(now)

    count_stmt = (
        select(InvestmentNewsProduct.investment_product_id, func.count().label("appearances"))
        .join(InvestmentNews, InvestmentNews.investment_news_id == InvestmentNewsProduct.investment_news_id)
        .where(InvestmentNews.published_at >= week_start)
        .group_by(InvestmentNewsProduct.investment_product_id)
    )
    appearances = dict(session.execute(count_stmt).all())

    products = list(session.execute(select(InvestmentProduct).where(InvestmentProduct.is_active)).scalars())
    if len(products) < 3:
        raise ValueError(f"활성 회사가 {len(products)}개뿐이라 하루 3건을 뽑을 수 없다")

    random.shuffle(products)
    products.sort(key=lambda product: appearances.get(product.investment_product_id, 0))
    todays_companies = products[:3]

    down = random.choice(todays_companies)
    up = [product for product in todays_companies if product is not down]
    return up, down


def _day_start_kst(now: datetime) -> datetime:
    """`now` 가 속한 날짜의 00:00 KST. 오늘 뉴스가 이미 생성됐는지 확인하는 데 쓴다."""
    kst_now = now.astimezone(KST)
    return datetime.combine(kst_now.date(), time.min, tzinfo=KST)


def yesterdays_news(session: Session, now: datetime) -> list[InvestmentNews]:
    """전날(어제, KST) 발행된 뉴스.

    가격은 **뉴스가 난 다음 날** 움직인다 — 오늘 만든 뉴스가 오늘 가격을 그 자리에서
    바로 움직이면, "뉴스 3건이 하루치 순증을 만든다"는 설계(모듈 docstring)가 하루
    안에서 원인과 결과가 동시에 일어나는 셀프 루프가 된다. 그래서 라우터는 이 함수로
    어제 뉴스를 가져와 `update_prices_for_todays_news` 에 넘기고, `generate_todays_news`
    로 만드는 오늘 뉴스는 내일 가격을 움직이는 데 쓴다.

    아직 하루도 안 지난 첫날은 빈 리스트를 돌려준다 — 그날은 가격이 안 움직인다.
    """
    day_start = _day_start_kst(now)
    yesterday_start = day_start - timedelta(days=1)
    return list(
        session.execute(
            select(InvestmentNews).where(
                InvestmentNews.published_at >= yesterday_start,
                InvestmentNews.published_at < day_start,
            )
        ).scalars()
    )


def _generate_news_text(product: InvestmentProduct, impact: str) -> tuple[str, str]:
    """회사 하나의 오늘자 뉴스 문구를 LLM으로 생성한다. (title, content) 반환.

    회사명과 회사 설명은 모델에 보내지 않는다. `product.news_context` 에 있는 대표 경제
    요인과 사건 조건만으로 외부 경제 사건을 쓰게 한다. 회사별 출력 금지어만 추가해 관련
    회사와 영향을 아이가 직접 추론할 수 있게 한다.

    `max_tokens`/`max_completion_tokens` 는 일부러 안 넘긴다. 모델(gpt-4o-mini 계열 /
    gpt-5.x 계열)마다 파라미터 이름이 달라서(`app.core.config` 참고) 잘못 넘기면 그
    자체로 요청이 실패한다. 뉴스 문구가 짧아 기본값으로도 충분하다.
    """
    client = get_client()
    forbidden_terms = _NEWS_FORBIDDEN_TERMS_BY_COMPANY.get(product.name, ())
    user_prompt = _build_news_user_prompt(product.news_context, impact, forbidden_terms)
    response = client.chat.completions.create(
        model=get_settings().openai_model,
        messages=[
            {"role": "system", "content": _SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        response_format=_NEWS_RESPONSE_FORMAT,
    )
    content = response.choices[0].message.content
    if content is None:
        raise ValueError(f"LLM이 빈 응답을 반환했다 (company={product.name})")
    draft = _NewsDraft.model_validate_json(content)
    # DB 컬럼이 VARCHAR(255)라 초과분은 잘라낸다 — 프롬프트로 길이를 지시해도
    # LLM이 넘길 수 있다. 잘리는 것이 INSERT 실패보다 낫다.
    return draft.title[:255], draft.content[:255]


def generate_todays_news(session: Session, now: datetime) -> list[InvestmentNews]:
    """오늘 뉴스 3건을 만들어 저장한다.

    `select_todays_companies` 로 회사를 고르고, 회사마다 LLM으로 문구를 생성해
    `investment_news` + `investment_news_products` 에 INSERT 한다.

    오늘 뉴스가 이미 있으면 다시 만들지 않고 그대로 돌려준다 — Spring 스케줄러가
    같은 날 두 번 호출해도(재시도 등) 뉴스가 중복 생성되지 않는다.
    """
    day_start = _day_start_kst(now)
    existing = list(
        session.execute(select(InvestmentNews).where(InvestmentNews.published_at >= day_start)).scalars()
    )
    if existing:
        return existing

    up, down = select_todays_companies(session, now)

    created: list[InvestmentNews] = []
    for product, impact in [(p, "POSITIVE") for p in up] + [(down, "NEGATIVE")]:
        title, content = _generate_news_text(product, impact)
        news = InvestmentNews(title=title, content=content, impact=impact, published_at=now)
        session.add(news)
        session.flush()  # investment_news_id 를 확보해야 아래 join 행을 만들 수 있다
        session.add(
            InvestmentNewsProduct(
                investment_news_id=news.investment_news_id,
                investment_product_id=product.investment_product_id,
            )
        )
        created.append(news)

    session.commit()
    return created


def _products_by_news_id(session: Session, news_items: list[InvestmentNews]) -> dict[int, InvestmentProduct]:
    """뉴스 ID -> 그 뉴스가 가리키는 회사. `InvestmentNewsProduct` 가 1:1 로만 쓰이므로
    (`models.InvestmentNewsProduct` 주석 참고) 뉴스당 회사가 정확히 하나다.
    """
    news_ids = [news.investment_news_id for news in news_items]
    rows = session.execute(
        select(InvestmentNewsProduct.investment_news_id, InvestmentProduct)
        .join(
            InvestmentProduct,
            InvestmentProduct.investment_product_id == InvestmentNewsProduct.investment_product_id,
        )
        .where(InvestmentNewsProduct.investment_news_id.in_(news_ids))
    ).all()
    return {news_id: product for news_id, product in rows}


def todays_news_with_companies(
    session: Session, news_items: list[InvestmentNews]
) -> list[tuple[InvestmentNews, InvestmentProduct]]:
    """뉴스마다 그 뉴스가 가리키는 회사를 붙여서 돌려준다. 라우터가 응답을 만들 때 쓴다."""
    product_by_news_id = _products_by_news_id(session, news_items)
    return [(news, product_by_news_id[news.investment_news_id]) for news in news_items]


def _price_delta(weekly_reward_limit: int) -> int:
    """주가 변동폭 = `weekly_reward_limit` 의 40% / 7. 상승·하락 폭이 같은 값이다.

    `core/points.py` 와 같은 반올림 규칙(`ROUND_HALF_UP`, 최소 1)을 따르되, 계산식
    자체가 "가중치 * 기준금액"이 아니라 "한도 * 비율 / 7"이라 `to_points()`를
    재사용하지 않고 여기 따로 둔다.
    """
    raw = (Decimal(weekly_reward_limit) * INVESTMENT_SHARE / DAYS_PER_WEEK).quantize(
        Decimal("1"), rounding=ROUND_HALF_UP
    )
    return max(1, int(raw))


def _price_floor(seed_price: int) -> int:
    """이 회사의 가격 하한 = 시작가(seed `current_price`)의 10%.

    가격이 리셋되지 않고 계속 이어지므로(설계 문서 "6. 가격 하한"), 여러 주에 걸쳐
    하락이 몰리면 이론상 0 이하로 갈 수 있다. 더 못 내려가게 여기서 막는다.
    """
    raw = (Decimal(seed_price) * PRICE_FLOOR_RATIO).quantize(Decimal("1"), rounding=ROUND_HALF_UP)
    return max(1, int(raw))


def update_prices_for_todays_news(session: Session, news_items: list[InvestmentNews], now: datetime) -> int:
    """`news_items`를 바탕으로 가족마다 오늘 가격을 갱신한다.

    **호출자가 `yesterdays_news()`로 가져온 "어제 뉴스"를 넘겨야 한다** — 이 함수 자체는
    받은 뉴스가 어제 것인지 오늘 것인지 모른다. 라우터가 순서를 지킨다:
    어제 뉴스로 오늘 가격을 갱신 -> 오늘 뉴스를 새로 생성(내일 가격에 쓸 것).
    같은 날 만든 뉴스로 그날 가격을 바로 움직이면 원인과 결과가 하루 안에서 동시에
    일어나는 셀프 루프가 된다.

    `investment_products`/`investment_news`는 전역이지만 가격 변동폭은 가족마다 다른
    `weekly_reward_limit`에서 나온다. 그래서 뉴스 하나당 가족 수만큼
    `investment_price_history` 행을 쓴다.

    `generate_todays_news`가 뉴스는 멱등으로 만들어 줘도, 이 함수를 또 부르면 가족마다
    가격이 두 번 움직여 버린다. 그래서 오늘 이미 가격을 갱신한 적이 있으면 다시 안 하고
    0을 반환한다.

    온보딩 전이라 `weekly_reward_limit` 이 0인 가족은 건너뛴다. 변동폭에 최소 1을
    강제하는데, 그걸 그대로 적용하면 온보딩 안 한 가족에게 억지로 최소 변동을 주게 된다
    (`app.domains.shopping.services.has_income` 과 같은 이유).

    반환값은 새로 쓴 `investment_price_history` 행 수.
    """
    day_start = _day_start_kst(now)
    already_updated = session.execute(
        select(InvestmentPriceHistory.investment_price_history_id)
        .where(InvestmentPriceHistory.recorded_at >= day_start)
        .limit(1)
    ).first()
    if already_updated:
        return 0

    product_by_news_id = _products_by_news_id(session, news_items)
    todays_moves = [(product_by_news_id[news.investment_news_id], news.impact) for news in news_items]
    target_product_ids = [product.investment_product_id for product, _ in todays_moves]

    # 각 (가족, 회사)의 최신가. 이력이 없으면 아래에서 시작가(seed)로 대체한다.
    history_rows = session.execute(
        select(
            InvestmentPriceHistory.family_id,
            InvestmentPriceHistory.investment_product_id,
            InvestmentPriceHistory.price,
        )
        .where(InvestmentPriceHistory.investment_product_id.in_(target_product_ids))
        .order_by(InvestmentPriceHistory.recorded_at.desc())
    ).all()
    latest_price: dict[tuple[int, int], int] = {}
    for family_id, product_id, price in history_rows:
        latest_price.setdefault((family_id, product_id), price)

    families = list(session.execute(select(Family)).scalars())

    written = 0
    for family in families:
        if family.weekly_reward_limit <= 0:
            continue
        delta = _price_delta(family.weekly_reward_limit)
        for product, impact in todays_moves:
            current = latest_price.get((family.family_id, product.investment_product_id), product.current_price)
            if impact == "POSITIVE":
                new_price = current + delta
            else:
                new_price = max(_price_floor(product.current_price), current - delta)
            session.add(
                InvestmentPriceHistory(
                    family_id=family.family_id,
                    investment_product_id=product.investment_product_id,
                    price=new_price,
                    recorded_at=now,
                )
            )
            written += 1

    session.commit()
    return written
