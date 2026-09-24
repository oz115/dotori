"""투자 뉴스/시세 API의 요청/응답 스키마."""

from pydantic import BaseModel


class InvestmentNewsItem(BaseModel):
    investment_news_id: int
    investment_product_id: int
    company_name: str
    title: str
    content: str
    # POSITIVE / NEGATIVE
    impact: str


class DailyInvestmentResponse(BaseModel):
    news: list[InvestmentNewsItem]
    # 오늘 새로 쓴 investment_price_history 행 수. 재시도로 이미 처리된 날이면 0이다.
    price_updates_written: int


class PriceBackfillResponse(BaseModel):
    # 새로 쓴 investment_price_history 행 수. 이미 이력이 있는 가족이면 0이다.
    price_updates_written: int
