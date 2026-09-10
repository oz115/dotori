"""상점 상품 가격 책정 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다."""

from fastapi import APIRouter, Depends

from app.core.security import require_internal_token

router = APIRouter(
    prefix="/internal/shopping",
    tags=["shopping"],
    dependencies=[Depends(require_internal_token)],
)

# 엔드포인트는 API 명세(노션)가 정해지면 추가한다.
#
# 참고: 노션 「상품 생성」 구현 메모는 이 경로를 HTTP 가 아니라 **RabbitMQ** 로 잡고 있다
# (exchange `store.product.exchange`, routing key `store.product.value-evaluation`).
# 백엔드에 AI 서버로 나가는 HTTP 클라이언트가 없고 발행 코드만 머지되어 있으므로,
# 어느 쪽으로 갈지 정리가 필요하다. 재산정을 사람이 직접 돌리는 용도로는 이 라우터가 유용하다.
