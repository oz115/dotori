"""할일 도메인이 쓰는 테이블 매핑.

접근 범위는 `docs/db-boundary.md` 참고. 이 서버가 실제로 읽거나 쓰는 컬럼만 매핑한다.

`families` 는 `app.core.models.Family` 를 쓴다 (상점·퀴즈와 공유하므로 중복 매핑 금지).
"""

from datetime import datetime
from decimal import Decimal

from sqlalchemy import Numeric, String
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class Chore(Base):
    """읽기 + `value`, `reward_points` 쓰기.

    기준 금액이 상점과 다르다. 백엔드 `ChoreRewardEvaluationMessage` javadoc 은
    `reward_points = value * families.weekly_allowance` 로 적고 있다 — 하루 소득이 아니라
    **주간 기본 용돈**이다. 상점(`price = value * 하루 소득`)과 수식이 갈리므로
    `app.core.points.to_points()` 에 넘기는 기준 금액이 서로 다르다.

    노션 「AI 기능 정리」가 할 일을 `weekly_reward_limit` 배분에서 제외하고 주간 기본 용돈
    대비 비율로 제시하기로 정했다 (예: 설거지 1회 = 0.1 * 주간 기본 용돈). javadoc 의
    수식이 그 결정과 같다.
    """

    __tablename__ = "chores"

    chore_id: Mapped[int] = mapped_column(primary_key=True)
    family_id: Mapped[int] = mapped_column()
    child_id: Mapped[int] = mapped_column()
    title: Mapped[str] = mapped_column(String(100))
    description: Mapped[str | None] = mapped_column(String(255))
    # 부모가 등록 시 고르는 난이도. 가치 평가의 입력이다.
    difficulty: Mapped[str] = mapped_column(String)
    # DAY / WEEK / MONTH
    repeat_type: Mapped[str] = mapped_column(String)
    value: Mapped[Decimal | None] = mapped_column(Numeric(10, 2))
    reward_points: Mapped[int | None] = mapped_column()
    deleted_at: Mapped[datetime | None] = mapped_column()
