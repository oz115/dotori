"""공용 테스트 픽스처.

실제 Postgres/GMS 는 테스트에서 건드리지 않는다 — DB 는 in-memory SQLite,
LLM 은 각 테스트에서 `unittest.mock.patch`로 대체한다.
"""

from datetime import datetime, timezone

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.database import Base
from app.domains.investment.models import InvestmentProduct


@pytest.fixture
def db_session():
    # StaticPool + check_same_thread=False 가 필요하다 — 기본 설정이면 SQLite
    # `:memory:` 는 커넥션(스레드)마다 별개의 빈 DB 라, 라우터 테스트가 TestClient 를 거쳐
    # FastAPI 의 threadpool(다른 스레드)에서 세션을 쓰면 테이블이 안 보인다.
    # (SQLAlchemy 공식 SQLite in-memory 테스트 가이드가 권장하는 설정)
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    TestSessionLocal = sessionmaker(bind=engine)
    session = TestSessionLocal()
    try:
        yield session
    finally:
        session.close()
        engine.dispose()


@pytest.fixture
def ten_companies(db_session):
    """노션 「회사 리스트」와 같은 개수(10개)를 심는다. 이름 자체은 테스트에 안 쓰인다."""
    companies = [
        InvestmentProduct(
            investment_product_id=i + 1,
            name=f"회사{i}",
            description="설명",
            category="STOCK",
            news_context=(
                "대표 경제 요인: 원자재 가격\n"
                "회사에 유리한 변화: 원자재 가격 하락\n"
                "회사에 불리한 변화: 원자재 가격 상승\n"
                "뉴스 소재: 생산량, 수입량, 재고량"
            ),
            current_price=10000,
            is_active=True,
        )
        for i in range(10)
    ]
    db_session.add_all(companies)
    db_session.commit()
    return companies


@pytest.fixture
def now_kst_afternoon():
    """KST 오후 2시에 해당하는 aware datetime (UTC 로 표현). 날짜 경계 테스트를 피한다."""
    return datetime(2026, 9, 11, 5, 0, tzinfo=timezone.utc)
