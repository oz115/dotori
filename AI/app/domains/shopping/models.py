"""상점 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만
매핑한다 — 테이블 전체 컬럼을 옮겨 적지 않는다.

`families` 는 여기 두지 않는다. 할일·퀴즈도 같은 테이블을 참조하는데, 같은 `Base` 에
같은 `__tablename__` 을 두 번 등록하면 기동이 실패한다. `app.core.models.Family` 를 쓴다.

카테고리 같은 네이티브 ENUM 컬럼은 SQLAlchemy `Enum` 대신 `String` 으로 매핑한다.
문자열을 그대로 넘겨도 캐스트 없이 동작하는 것을 확인했고(db-boundary.md 참고),
SQLAlchemy 가 스스로 enum 타입을 관리하려 드는 것을 피하기 위해서다.
"""

from datetime import datetime
from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class StoreProduct(Base):
    """읽기 + `value`, `price` 쓰기.

    `value` 는 아이 하루 소득의 배수(가중치), `price` 는 실제로 내는 포인트다.
    둘 다 nullable 이고 AI 가 채우기 전에는 NULL = "가격 산정 중" 이다.
    NULL 인 동안 백엔드 구매 API 는 `PRICE_NOT_EVALUATED` 로 막는다.

    `deleted_at` 을 매핑하는 이유: 산정하는 사이에 부모가 상품을 지울 수 있다.
    쓰기 전에 확인해야 지워진 행을 되살리지 않는다.
    """

    __tablename__ = "store_products"

    store_product_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    name: Mapped[str] = mapped_column(String(100))
    # 부모가 직접 넣는 자유 텍스트다. 프롬프트에 넣을 때 지시문으로 읽히지 않게 감싼다.
    description: Mapped[str | None] = mapped_column(String(255))
    # GAME / SNACK / TOY / OUTING / PRIVILEGE / ETC
    category: Mapped[str] = mapped_column(String)
    value: Mapped[Decimal | None] = mapped_column(Numeric(10, 2))
    price: Mapped[int | None] = mapped_column()
    deleted_at: Mapped[datetime | None] = mapped_column()
