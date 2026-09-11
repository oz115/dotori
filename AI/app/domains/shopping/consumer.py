"""상점 가격 책정 큐 컨슈머. **이 도메인의 트리거다.**

부모가 상품을 등록하면 Spring 이 `value`/`price` 를 비운 채로 행을 만들고, 커밋 이후에
큐로 발행한다 (`StoreProductValueEvaluationListener`, `@TransactionalEventListener`
`AFTER_COMMIT`). AI 서버는 그 메시지를 받아 가중치를 산정하고 `price` 를 계산해 두 컬럼을
채운다.

큐 이름과 페이로드는 백엔드 `StoreRabbitConfig` / `StoreProductValueEvaluationMessage` 가
원본이다. 발행 측만 머지되어 있고 컨슈머가 이 파일이다.

**메시지에 하루 소득이 담겨 오지 않는 것은 의도된 설계다.** 산정하는 사이 부모가 용돈을
바꾸면 낡은 값으로 계산되므로, `familyId` 가 아니라 `store_product_id` 로 DB 를 다시 읽는다
(`services.load_context`). 백엔드 `ChoreRewardEvaluationMessage` javadoc 에 같은 취지가 적혀
있다.

연결·재연결·ack 는 `app.messaging.consumer` 가 맡는다. 이 파일은 페이로드를 읽고 판정만 한다.
"""

import logging

from sqlalchemy.exc import OperationalError

from app.core.database import SessionLocal
from app.domains.shopping.services import price_product
from app.messaging.consumer import Outcome, Subscription, parse_json

logger = logging.getLogger(__name__)

# 백엔드 StoreRabbitConfig 의 상수와 같아야 한다.
EXCHANGE = "store.product.exchange"
QUEUE = "store.product.value-evaluation.queue"
ROUTING_KEY = "store.product.value-evaluation"


def handle(body: bytes) -> Outcome:
    """메시지 한 건. 산정해서 저장하고 ACK 한다.

    산정하지 못한 경우도 대부분 ACK 다 — 이미 책정됐거나(멱등성 가드) 부모가 지웠거나
    온보딩이 비었거나 LLM 응답이 못 쓸 값이면 다시 받아도 같은 결과다. 큐에 DLQ 가 없어
    되돌리면 무한 루프가 되므로, 사유를 로그로 남기고 버리는 편이 낫다.

    DB 가 끊긴 경우만 RETRY 다 (터널이 내려간 상황 등). 그건 잠시 뒤면 되고, 되돌리지 않으면
    그 상품이 영구히 "가격 산정 중" 으로 남는다.
    """
    payload = parse_json(body)
    if payload is None:
        return Outcome.ACK

    store_product_id = payload.get("storeProductId")
    if not isinstance(store_product_id, int):
        logger.error("event=dropped cause=invalid_payload field=storeProductId value=%r", store_product_id)
        return Outcome.ACK

    logger.info("event=received store_product_id=%s category=%s", store_product_id, payload.get("category"))

    # 페이로드의 name/description/category 를 쓰지 않고 DB 를 다시 읽는다 — 발행과 처리
    # 사이에 부모가 상품을 수정했을 수 있고, 어차피 온보딩 값을 읽으러 DB 에 가야 한다.
    try:
        with SessionLocal() as session:
            price_product(session, store_product_id)
    except OperationalError:
        logger.exception("event=db_unavailable store_product_id=%s", store_product_id)
        return Outcome.RETRY

    # price_product() 가 None 이어도 ACK 다. 사유는 그 안에서 로그로 남는다.
    return Outcome.ACK


SUBSCRIPTION = Subscription(exchange=EXCHANGE, queue=QUEUE, routing_key=ROUTING_KEY, handler=handle)
