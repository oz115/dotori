"""상점 가격 책정 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다.

이 도메인의 트리거는 라우터가 아니라 **큐 컨슈머**(`consumer.py`)다. 여기 있는 엔드포인트는
큐 메시지를 놓친 상품을 손으로 되살리는 복구 창구다.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.database import get_session
from app.core.security import require_internal_token
from app.domains.shopping.schemas import StoreProductValuationResponse
from app.domains.shopping.services import price_product

router = APIRouter(
    prefix="/internal/shopping",
    tags=["shopping"],
    dependencies=[Depends(require_internal_token)],
)


@router.post("/products/{store_product_id}/valuation", summary="상품 가격 수동 재산정")
def valuate_product(
    store_product_id: int,
    session: Session = Depends(get_session),
) -> StoreProductValuationResponse:
    """**트리거가 아니다.** 평소 산정은 큐 컨슈머가 한다 (`consumer.py`).

    이건 큐 메시지를 놓친 상품을 손으로 되살리는 창구다. Spring 은 발행에 publisher
    confirms 를 쓰지 않으므로(`StoreProductValueEvaluationListener` 는 `convertAndSend` 만
    부른다), 발행 시점에 브로커가 죽어 있으면 그 메시지는 **큐에 들어간 적이 없어 재배달도
    되지 않는다.** 상품 행은 남고 `value`/`price` 는 영구히 NULL 이라 그 상품은 계속
    `PRICE_NOT_EVALUATED` 로 구매가 막힌다. 그때 이 엔드포인트로 다시 산정한다.

    상품이 들어오는 경로가 따로 있는 것은 아니다 — 행은 항상 Spring 등록에서 생기고
    (`StoreProductServiceImpl.createProduct`), 이 엔드포인트는 이미 있는 행의 두 컬럼만
    채운다. 출처는 같고 트리거만 다르다.

    **멱등이다.** 이미 책정된 상품(`value IS NOT NULL`)은 다시 산정하지 않고 `valued=false`
    를 돌려준다. 여러 번 불러도 값이 덮이지 않는다.

    산정하지 않은 경우도 200 과 `valued=false` 다. 이미 책정됐거나 부모가 지웠거나 온보딩이
    비었거나 LLM 이 실패한 것 모두 호출자가 할 수 있는 일이 없어서, 에러로 올리면 의미 없는
    예외 처리만 생긴다. 사유는 로그에 있다 (`event=skipped cause=...` /
    `event=failed cause=...`).

    `reason` 은 이 응답에만 실린다. `store_products` 에 담을 컬럼이 없어서
    (`docs/db-boundary.md` 협의 3번) 부모 화면에 보여주려면 Spring 이 받아 전달해야 한다.

    놓친 상품을 한 번에 처리해야 하면 `value IS NULL` 인 행을 훑는 엔드포인트가 더 낫다.
    지금은 건당 호출만 있다.
    """
    valuation = price_product(session, store_product_id)
    if valuation is None:
        return StoreProductValuationResponse(store_product_id=store_product_id, valued=False)
    return StoreProductValuationResponse(
        store_product_id=store_product_id,
        valued=True,
        value=valuation.value,
        price=valuation.price,
        reason=valuation.reason,
    )
