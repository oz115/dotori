"""투자 뉴스/시세 내부 API. Spring 이 스케줄러에서 `X-Internal-Token` 헤더로 호출한다."""

from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.database import get_session
from app.core.security import require_internal_token
from app.domains.investment.schemas import DailyInvestmentResponse, InvestmentNewsItem
from app.domains.investment.services import (
    generate_todays_news,
    todays_news_with_companies,
    update_prices_for_todays_news,
    yesterdays_news,
)

router = APIRouter(
    prefix="/internal/investment",
    tags=["investment"],
    dependencies=[Depends(require_internal_token)],
)


@router.post("/daily", summary="오늘의 투자 뉴스 생성 + 가족별 가격 갱신")
def run_daily_update(session: Session = Depends(get_session)) -> DailyInvestmentResponse:
    """Spring 스케줄러가 하루 한 번 호출한다.

    뉴스 생성(`generate_todays_news`)·가격 갱신(`update_prices_for_todays_news`) 둘 다
    멱등이라 재시도해도 안전하다 — 오늘 이미 처리됐으면 뉴스는 그대로 돌려주고
    `price_updates_written` 은 0이 된다.

    **가격은 어제 뉴스로 오늘 움직인다.** 오늘 만든 뉴스가 오늘 가격을 바로 움직이면
    원인과 결과가 하루 안에서 동시에 일어나 버린다 (`yesterdays_news` 참고). 그래서
    순서가 중요하다 — 어제 뉴스로 먼저 갱신하고, 그다음 오늘 뉴스를 만든다(그건 내일 쓴다).

    회사 로테이션이 "이번 주 등장 횟수"를 기준으로 삼으므로, 이 엔드포인트가 매일
    한 번씩은 반드시 불려야 로테이션과 주간 수익 한도 계산이 어긋나지 않는다.
    """
    now = datetime.now(timezone.utc)
    written = update_prices_for_todays_news(session, yesterdays_news(session, now), now)
    news = generate_todays_news(session, now)

    items = [
        InvestmentNewsItem(
            investment_news_id=news_row.investment_news_id,
            investment_product_id=product.investment_product_id,
            company_name=product.name,
            title=news_row.title,
            content=news_row.content,
            impact=news_row.impact,
        )
        for news_row, product in todays_news_with_companies(session, news)
    ]
    return DailyInvestmentResponse(news=items, price_updates_written=written)
