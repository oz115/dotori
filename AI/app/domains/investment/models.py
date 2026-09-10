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
    """INSERT. 뉴스와 회사를 잇는다.

    테이블은 N:N 조인 형태지만 **실제로는 1:1 로만 쓴다.** 노션 「투자 (Investment)」가
    `1 경제요인 : 1 회사` 로 정했고 뉴스 한 건은 경제 요인 하나를 다루므로,
    뉴스 하나가 가리키는 회사도 하나다. 요인이 회사 간에 겹치지 않는다
    (기온 -> 시원아이스, 밀 가격 -> 고소한빵 처럼 회사마다 대표 요인이 하나씩이다).

    그래서 뉴스당 행을 하나만 INSERT 한다. 여러 행을 넣으면 한 뉴스가 여러 회사의
    주가를 움직여 정답이 모호해진다.

    TODO(db-boundary.md #2): 상승/하락 방향을 담을 컬럼이 없다. 하루 3건 중 2건이
    상승, 1건이 하락이라 방향이 뉴스의 핵심 정보인데 지금은 저장할 곳이 없다.
    1:1 이 되면서 `evidence_grade`(설계 v3 의 근거 등급)는 필요 없어졌다 —
    한 뉴스가 회사마다 다른 영향을 주는 경우가 사라졌기 때문이다.
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

