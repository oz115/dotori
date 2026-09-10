"""가격 책정 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만
매핑한다 — 테이블 전체 컬럼을 옮겨 적지 않는다.

카테고리·난이도 등 네이티브 ENUM 컬럼은 SQLAlchemy `Enum` 대신 `String` 으로 매핑한다.
문자열을 그대로 넘겨도 캐스트 없이 동작하는 것을 확인했고(db-boundary.md 참고),
SQLAlchemy 가 스스로 enum 타입을 관리하려 드는 것을 피하기 위해서다.
"""

from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class Family(Base):
    """읽기 전용. 하루 소득 D = (weekly_allowance + weekly_reward_limit) / 7 계산에 쓴다."""

    __tablename__ = "families"

    family_id: Mapped[int] = mapped_column(primary_key=True)
    weekly_allowance: Mapped[int] = mapped_column()
    weekly_reward_limit: Mapped[int] = mapped_column()


class StoreProduct(Base):
    """읽기 + `value`, `price` 쓰기."""

    __tablename__ = "store_products"

    store_product_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    name: Mapped[str] = mapped_column(String(100))
    category: Mapped[str] = mapped_column(String)
    value: Mapped[Decimal | None] = mapped_column(Numeric(10, 2))
    price: Mapped[int | None] = mapped_column()


class Chore(Base):
    """읽기 + `reward_points` 쓰기."""

    __tablename__ = "chores"

    chore_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    child_id: Mapped[int] = mapped_column()
    title: Mapped[str] = mapped_column(String(100))
    difficulty: Mapped[str] = mapped_column(String)
    value: Mapped[Decimal | None] = mapped_column(Numeric(10, 2))
    reward_points: Mapped[int | None] = mapped_column()
