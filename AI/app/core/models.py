"""여러 도메인이 공유하는 읽기 전용 테이블 매핑.

`families` 는 상점·할일·퀴즈가 모두 참조한다. 그런데 같은 `Base` 에 같은
`__tablename__` 을 두 번 등록하면 SQLAlchemy 가 기동 시점에

    InvalidRequestError: Table 'families' is already defined for this MetaData instance.

를 던진다. 도메인마다 따로 매핑할 수 없어서 여기 한 곳에 둔다.

접근 범위는 `docs/db-boundary.md` 참고 — `families` 는 **읽기만** 한다.
"""

from sqlalchemy import String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class Family(Base):
    """읽기 전용. 온보딩에서 정한 값으로 각 도메인이 포인트를 역산한다.

    단위는 원이 아니라 **포인트**다. 10원 = 1p 환산은 백엔드가 가족 생성 시점에
    이미 끝냈다 (`global/util/PointExchangePolicy`). 여기서 다시 나누거나 곱하지 않는다.

    두 금액 컬럼은 `INTEGER DEFAULT 0 NOT NULL` 이다. 온보딩을 아직 채우지 않은 가족은
    비정상이 아니라 정상적으로 0 이므로, 나누기 전에 0 인지 확인해야 한다
    (`app.core.points.daily_income` 주석 참고).
    """

    __tablename__ = "families"

    family_id: Mapped[int] = mapped_column(primary_key=True)
    # 기획 문서의 B. 주간 기본 용돈. 할일 보상의 기준값이다.
    weekly_allowance: Mapped[int] = mapped_column()
    # 기획 문서의 A. 예적금·투자·퀴즈로 주당 벌 수 있는 최대 포인트 (할일 제외).
    weekly_reward_limit: Mapped[int] = mapped_column()
    # 아이의 금융 이해도. BEGINNER / INTERMEDIATE / ADVANCED.
    # 이 스키마에서 유일하게 네이티브 ENUM 이 아니라 VARCHAR(20) + CHECK 다.
    financial_knowledge_level: Mapped[str] = mapped_column(String(20))
