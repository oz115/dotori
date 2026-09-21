"""투자 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만 매핑한다.
"""

from datetime import datetime

from sqlalchemy import String
from sqlalchemy.dialects.postgresql import ENUM as PgEnum
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base

# INSERT하는 네이티브 ENUM 컬럼(`InvestmentNews.impact`) 전용.
# SQLAlchemy 는 `String` 매핑을 그대로 INSERT 하면 psycopg3 dialect 가 바인드 파라미터에
# `::VARCHAR` 캐스트를 붙인다. VARCHAR -> investment_news_impact 는 암시적 캐스트가 없어서
# `DatatypeMismatch` 로 실패한다 (AI/docs/db-boundary.md 의 "캐스트 없이 동작한다" 관찰은
# 비교·raw psycopg 기준이지 SQLAlchemy 컴파일 INSERT 기준이 아니었다).
# `create_type=False` 로 SQLAlchemy 가 타입을 만들거나 지우려 들지 않게 막는다 — 이 서버는
# 테이블/타입을 소유하지 않는다는 원칙은 그대로 지킨다.
# 읽기만 하는 다른 네이티브 ENUM 컬럼(`category` 등)은 비교에서 캐스트 문제가 없어
# 계속 평범한 `String` 으로 둔다 — INSERT 하는 컬럼만 이렇게 매핑한다.
_investment_news_impact = PgEnum("POSITIVE", "NEGATIVE", name="investment_news_impact", create_type=False)


class InvestmentProduct(Base):
    """읽기 전용. 회사 10개 고정.

    가격 생성의 기준가(시작가)는 이 테이블이 아니라 가족의 주간 용돈
    (`weekly_allowance` ÷ 종목 수 10)이다 — `current_price` 는 온보딩 전 가족을 위한
    폴백으로만 남아 있다.
    """

    __tablename__ = "investment_products"

    investment_product_id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100))
    # "하는 일". 아이에게 회사를 소개할 때 쓰며 뉴스 생성 프롬프트에는 넣지 않는다.
    description: Mapped[str | None] = mapped_column(String(255))
    category: Mapped[str | None] = mapped_column(String)
    # 대표 경제 요인 + 유리/불리한 변화 + 뉴스 소재. 매일 뉴스 생성 LLM 프롬프트의 참고자료.
    news_context: Mapped[str] = mapped_column()
    current_price: Mapped[int] = mapped_column()
    is_active: Mapped[bool] = mapped_column()


class InvestmentNews(Base):
    """INSERT. 매일 뉴스 3건 생성."""

    __tablename__ = "investment_news"

    investment_news_id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(255))
    content: Mapped[str] = mapped_column(String(255))
    # POSITIVE / NEGATIVE. 이 뉴스가 가리키는 회사의 가격을 어느 방향으로 움직일지.
    impact: Mapped[str] = mapped_column(_investment_news_impact)
    published_at: Mapped[datetime] = mapped_column()


class InvestmentNewsProduct(Base):
    """INSERT. 뉴스와 회사를 잇는다.

    테이블은 N:N 조인 형태지만 **실제로는 1:1 로만 쓴다.** 노션 「투자 (Investment)」가
    `1 경제요인 : 1 회사` 로 정했고 뉴스 한 건은 경제 요인 하나를 다루므로,
    뉴스 하나가 가리키는 회사도 하나다. 요인이 회사 간에 겹치지 않는다
    (기온 -> 시원아이스, 밀 가격 -> 고소한빵 처럼 회사마다 대표 요인이 하나씩이다).

    그래서 뉴스당 행을 하나만 INSERT 한다. 여러 행을 넣으면 한 뉴스가 여러 회사의
    주가를 움직여 정답이 모호해진다.

    상승/하락 방향은 이 테이블이 아니라 `InvestmentNews.impact`에 담는다
    (db-boundary.md 협의 2번). 1:1이 되면서 `evidence_grade`(설계 v3의 근거 등급)는
    필요 없어졌다 — 한 뉴스가 회사마다 다른 영향을 주는 경우가 사라졌기 때문이다.
    """

    __tablename__ = "investment_news_products"

    investment_news_product_id: Mapped[int] = mapped_column(primary_key=True)
    investment_news_id: Mapped[int] = mapped_column()
    investment_product_id: Mapped[int] = mapped_column()


class InvestmentPriceHistory(Base):
    """INSERT. 가족별 주가 변동 이력.

    `investment_products`/`investment_news`는 전역이지만 이 테이블은 가족별이다.
    변동폭이 `families.weekly_reward_limit`(가족마다 다름)의 40%/7이라, 같은 뉴스라도
    가족마다 가격이 다르게 움직인다 (db-boundary.md 협의 2번). "이 가족에게 이 회사가
    지금 얼마인가"는 `family_id` + `investment_product_id`로 가장 최근 행을 찾아서 구한다.
    """

    __tablename__ = "investment_price_history"

    investment_price_history_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    investment_product_id: Mapped[int] = mapped_column()
    price: Mapped[int] = mapped_column()
    recorded_at: Mapped[datetime] = mapped_column()

