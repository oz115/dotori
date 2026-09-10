"""할일 보상 책정 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다."""

from fastapi import APIRouter, Depends

from app.core.security import require_internal_token

router = APIRouter(
    prefix="/internal/work",
    tags=["work"],
    dependencies=[Depends(require_internal_token)],
)

# 엔드포인트는 API 명세(노션)가 정해지면 추가한다.
#
# 상점과 마찬가지로 백엔드는 이미 RabbitMQ 로 발행하고 있다
# (exchange `chore.exchange`, routing key `chore.reward-evaluation`).
