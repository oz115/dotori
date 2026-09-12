"""`chores` 조회와 쓰기. SQL 이 이 파일 밖으로 나가지 않는다.

접근 범위는 `docs/db-boundary.md` 를 따른다 — `chores` 는 읽기와 `value`·`reward_points`
쓰기, `families` 는 읽기만이다. 상점의 `app.domains.shopping.repository` 와 같은 구조다.
"""

import logging
from dataclasses import dataclass

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.core.models import Family
from app.domains.work.models import Chore

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class ChoreContext:
    """산정에 필요한 값만 담는다. `chores` + `families` 한 번 조회로 채운다."""

    title: str
    description: str | None
    difficulty: str
    weekly_allowance: int


def load_context(session: Session, chore_id: int) -> ChoreContext | None:
    """산정 대상을 읽는다. 산정하면 안 되는 상태면 None (사유는 로그).

    `deleted_at IS NULL` 을 WHERE 에 넣지 않는다. 넣으면 "삭제됨" 과 "없음" 이 구분되지
    않는데, 앞은 정상(부모가 지운 것)이고 뒤는 이상 상황이라 로그 수준이 달라야 한다.
    LEFT JOIN 을 쓰는 이유도 같다 — INNER JOIN 이면 0행이 "할 일 없음" 인지 "가족 없음" 인지
    알 수 없다.

    기준 금액이 상점과 다르다. 상점은 하루 소득이지만 할 일은 `weekly_allowance` 하나만
    있으면 된다 (`reward_points = value * weekly_allowance`). 그래서 `weekly_reward_limit`
    은 읽지 않는다.
    """
    row = session.execute(
        select(
            Chore.title,
            Chore.description,
            Chore.difficulty,
            Chore.value,
            Chore.deleted_at,
            Family.weekly_allowance,
        )
        .join(Family, Family.family_id == Chore.family_id, isouter=True)
        .where(Chore.chore_id == chore_id)
    ).first()

    if row is None:
        # 등록 커밋 이후에 발행되므로(AFTER_COMMIT) 정상 흐름에서는 나오지 않는다.
        logger.error("event=skipped cause=chore_not_found chore_id=%s", chore_id)
        return None
    if row.deleted_at is not None:
        logger.info("event=skipped cause=deleted chore_id=%s", chore_id)
        return None
    if row.value is not None:
        # 멱등성 가드. 메시지가 두 번 배달돼도 이미 책정된 값을 덮지 않는다
        # (`docs/db-boundary.md` 의 "value IS NOT NULL 이면 건너뛴다").
        logger.info("event=skipped cause=already_valued chore_id=%s value=%s", chore_id, row.value)
        return None
    if row.weekly_allowance is None:
        # FK 가 있어 사실상 불가능하지만, LEFT JOIN 이라 문법적으로는 가능하다.
        logger.error("event=skipped cause=family_not_found chore_id=%s", chore_id)
        return None

    return ChoreContext(
        title=row.title,
        description=row.description,
        difficulty=row.difficulty,
        weekly_allowance=row.weekly_allowance,
    )


def save_reward(session: Session, chore_id: int, value, reward_points: int) -> bool:
    """`value` 와 `reward_points` 를 쓴다. 실제로 쓰였으면 True. 커밋은 호출자가 한다.

    `AND value IS NULL` 가드가 중복 배달을 무해하게 만든다. 조회와 쓰기 사이에 LLM 호출이
    끼어 두 단계가 원자적이지 않은데, 이 가드가 그 틈을 막는다.
    `AND deleted_at IS NULL` 은 산정하는 사이 부모가 할 일을 지운 경우다.

    **`rowcount == 0` 은 에러가 아니다.** 중복 배달이거나 산정 중 삭제된 것이므로 정상
    종료로 본다.

    `updated_at` 은 건드리지 않는다. 합의된 쓰기 범위가 `value`·`reward_points` 두 개다
    (`docs/db-boundary.md`). 상점과 같은 미해결 항목 — 트리거가 없어 갱신되지 않는다.
    """
    result = session.execute(
        update(Chore)
        .where(
            Chore.chore_id == chore_id,
            Chore.value.is_(None),
            Chore.deleted_at.is_(None),
        )
        .values(value=value, reward_points=reward_points)
    )
    if result.rowcount == 0:
        logger.info("event=skipped cause=lost_race chore_id=%s", chore_id)
        return False
    return True
