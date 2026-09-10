"""가격 책정 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다."""

from fastapi import APIRouter, Depends

from app.core.security import require_internal_token

router = APIRouter(
    prefix="/internal/price",
    tags=["pricing"],
    dependencies=[Depends(require_internal_token)],
)

# 엔드포인트는 API 명세(노션)가 정해지면 추가한다.
