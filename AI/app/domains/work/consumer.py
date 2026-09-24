"""할 일 보상 책정 큐 컨슈머. **이 도메인의 트리거다.**

부모가 할 일을 등록하면 Spring 이 value/reward_points 를 비운 채로 행을 만들고, 커밋 이후에
큐로 발행한다 (`ChoreRewardEvaluationListener`, `@TransactionalEventListener` `AFTER_COMMIT`).
AI 서버는 그 메시지를 받아 가중치를 산정하고 reward_points 를 계산해 두 컬럼을 채운다.

상점(`app.domains.shopping.consumer`)과 같은 구조다. 큐 이름·페이로드만 다르다 —
백엔드 `ChoreRabbitConfig` / `ChoreRewardEvaluationMessage` 가 원본이다.

**메시지에 주간 용돈이 담겨 오지 않는 것은 의도된 설계다.** 산정하는 사이 부모가 용돈을
바꾸면 낡은 값으로 계산되므로, familyId 가 아니라 chore_id 로 DB 를 다시 읽는다
(`services.reward_chore`). javadoc 에 같은 취지가 적혀 있다.

메시지에 difficulty 가 담겨 오지만 쓰지 않는다 — 발행과 처리 사이에 부모가 난이도를 고칠 수
있고, 어차피 DB 를 읽으러 가야 한다. DB 의 값이 최신이다.
"""

import logging

from sqlalchemy.exc import OperationalError

from app.core.database import SessionLocal
from app.domains.work.services import reward_chore
from app.messaging.consumer import Outcome, Subscription, parse_json

logger = logging.getLogger(__name__)

# 백엔드 ChoreRabbitConfig 의 상수와 같아야 한다.
EXCHANGE = "chore.exchange"
QUEUE = "chore.reward-evaluation.queue"
ROUTING_KEY = "chore.reward-evaluation"


def handle(body: bytes) -> Outcome:
    """메시지 한 건. 산정해서 저장하고 ACK 한다.

    산정하지 못한 경우도 대부분 ACK 다 — 이미 책정됐거나(멱등성 가드) 부모가 지웠거나
    온보딩이 비었거나 LLM 응답이 못 쓸 값이면 다시 받아도 같은 결과다. 큐에 DLQ 가 없어
    되돌리면 무한 루프가 되므로, 사유를 로그로 남기고 버리는 편이 낫다.

    DB 가 끊긴 경우만 RETRY 다. 그건 잠시 뒤면 되고, 되돌리지 않으면 그 할 일이 영구히
    "보상 산정 중" 으로 남는다.
    """
    payload = parse_json(body)
    if payload is None:
        return Outcome.ACK

    chore_id = payload.get("choreId")
    if not isinstance(chore_id, int):
        logger.error("event=dropped cause=invalid_payload field=choreId value=%r", chore_id)
        return Outcome.ACK

    logger.info("event=received chore_id=%s difficulty=%s", chore_id, payload.get("difficulty"))

    try:
        with SessionLocal() as session:
            reward_chore(session, chore_id)
    except OperationalError:
        logger.exception("event=db_unavailable chore_id=%s", chore_id)
        return Outcome.RETRY

    # reward_chore() 가 None 이어도 ACK 다. 사유는 그 안에서 로그로 남는다.
    return Outcome.ACK


SUBSCRIPTION = Subscription(exchange=EXCHANGE, queue=QUEUE, routing_key=ROUTING_KEY, handler=handle)
