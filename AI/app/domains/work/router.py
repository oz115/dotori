"""할 일 보상 책정 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다.

이 도메인의 트리거는 라우터가 아니라 **큐 컨슈머**(`consumer.py`)다. 여기 있는 엔드포인트는
큐 메시지를 놓친 할 일을 손으로 되살리는 복구 창구다.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.database import get_session
from app.core.security import require_internal_token
from app.domains.work.schemas import ChoreRewardResponse
from app.domains.work.services import reward_chore

router = APIRouter(
    prefix="/internal/work",
    tags=["work"],
    dependencies=[Depends(require_internal_token)],
)


@router.post("/chores/{chore_id}/reward", summary="할 일 보상 수동 재산정")
def reward_chore_endpoint(
    chore_id: int,
    session: Session = Depends(get_session),
) -> ChoreRewardResponse:
    """**트리거가 아니다.** 평소 산정은 큐 컨슈머가 한다 (`consumer.py`).

    상점과 같은 복구 창구다. Spring 은 발행에 publisher confirms 를 쓰지 않으므로, 발행
    시점에 브로커가 죽어 있으면 그 메시지는 큐에 들어간 적이 없어 재배달도 되지 않는다.
    할 일 행은 남고 value/reward_points 가 영구히 NULL 이 되는데, 그때 이 엔드포인트로 다시
    산정한다.

    **멱등이다.** 이미 책정된 할 일(value IS NOT NULL)은 다시 산정하지 않고 rewarded=false 를
    돌려준다. 산정하지 않은 경우도 200 과 rewarded=false 다 — 사유는 로그에 있다.
    """
    reward = reward_chore(session, chore_id)
    if reward is None:
        return ChoreRewardResponse(chore_id=chore_id, rewarded=False)
    return ChoreRewardResponse(
        chore_id=chore_id,
        rewarded=True,
        value=reward.value,
        reward_points=reward.reward_points,
        reason=reward.reason,
    )
