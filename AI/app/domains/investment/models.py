"""투자 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만 매핑한다.
"""

from datetime import datetime

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class InvestmentProduct(Base):
    """읽기 + `current_price` 쓰기. 회사 10개 고정."""

    __tablename__ = "investment_products"

    investment_product_id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100))
    category: Mapped[str | None] = mapped_column(String)
    current_price: Mapped[int] = mapped_column()
    is_active: Mapped[bool] = mapped_column()


class InvestmentNews(Base):
    """INSERT. 매일 뉴스 3건 생성."""

    __tablename__ = "investment_news"

    investment_news_id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(255))
    content: Mapped[str] = mapped_column(String(255))
    published_at: Mapped[datetime] = mapped_column()


class InvestmentNewsProduct(Base):
    """INSERT. 뉴스-회사 N:N 연결.

    TODO(db-boundary.md #2): 뉴스가 회사마다 호재/악재를 다르게 주는 설계(v3)에 필요한
    `impact`/`evidence_grade` 컬럼이 아직 없다. 투자 도메인 담당자와 협의 후 컬럼이
    생기면 여기 추가한다.
    """

    __tablename__ = "investment_news_products"

    investment_news_product_id: Mapped[int] = mapped_column(primary_key=True)
    investment_news_id: Mapped[int] = mapped_column()
    investment_product_id: Mapped[int] = mapped_column()


class InvestmentPriceHistory(Base):
    """INSERT. 주가 변동 이력."""

    __tablename__ = "investment_price_history"

    investment_price_history_id: Mapped[int] = mapped_column(primary_key=True)
    investment_product_id: Mapped[int] = mapped_column()
    price: Mapped[int] = mapped_column()

