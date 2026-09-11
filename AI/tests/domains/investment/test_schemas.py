"""investment/schemas.py (DTO) 검증."""

import pytest
from pydantic import ValidationError

from app.domains.investment.schemas import DailyInvestmentResponse, InvestmentNewsItem


def _news_item(**overrides):
    fields = {
        "investment_news_id": 1,
        "investment_product_id": 2,
        "company_name": "시원아이스",
        "title": "제목",
        "content": "본문",
        "impact": "POSITIVE",
    }
    fields.update(overrides)
    return InvestmentNewsItem(**fields)


def test_investment_news_item_accepts_valid_fields():
    item = _news_item()
    assert item.company_name == "시원아이스"
    assert item.impact == "POSITIVE"


@pytest.mark.parametrize("missing", ["investment_news_id", "investment_product_id", "company_name", "title", "content", "impact"])
def test_investment_news_item_requires_every_field(missing):
    fields = {
        "investment_news_id": 1,
        "investment_product_id": 2,
        "company_name": "시원아이스",
        "title": "제목",
        "content": "본문",
        "impact": "POSITIVE",
    }
    del fields[missing]
    with pytest.raises(ValidationError):
        InvestmentNewsItem(**fields)


def test_daily_investment_response_serializes_nested_news():
    response = DailyInvestmentResponse(news=[_news_item()], price_updates_written=3)

    dumped = response.model_dump()

    assert dumped["price_updates_written"] == 3
    assert dumped["news"][0]["company_name"] == "시원아이스"


def test_daily_investment_response_allows_empty_news():
    # 오늘 뉴스가 이미 있고 가격 갱신도 이미 끝난 재시도 케이스
    response = DailyInvestmentResponse(news=[], price_updates_written=0)

    assert response.news == []
    assert response.price_updates_written == 0
