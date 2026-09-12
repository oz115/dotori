"""RabbitMQ 컨슈머 배관. 도메인에 종속되지 않는다.

Spring 이 큐로 일을 넘기고 AI 서버가 그것을 받아 처리한다. 큐의 주인은 Spring 이고
(`StoreRabbitConfig` 등) 이 서버는 같은 속성으로 idempotent 하게 선언만 한다 — Spring 이
한 번도 뜨지 않은 환경에서도 혼자 뜰 수 있어야 하기 때문이다.

도메인별 처리는 `app/domains/*/consumer.py` 의 핸들러가 한다. 이 모듈은 연결·재연결·ack 만
맡는다 (`app/llm/client.py` 가 LLM 호출만 맡는 것과 같은 분업).

FastAPI 프로세스 안의 **백그라운드 스레드**로 돈다. `main.py` 의 lifespan 이 띄우고 내린다.
나중에 별도 워커 프로세스로 옮기려면 `run_forever()` 를 그 프로세스의 main 에서 부르면 되고,
lifespan 의 두 줄만 떼면 된다.
"""

import json
import logging
import random
import threading
from collections.abc import Callable
from dataclasses import dataclass
from enum import StrEnum

import pika
from pika.exceptions import AMQPChannelError, AMQPConnectionError, ChannelClosedByBroker

from app.core.config import get_settings

logger = logging.getLogger(__name__)

# 브로커가 끊겼을 때 재연결 대기 상한(초).
_RECONNECT_MAX_DELAY = 30
# 이 시간마다 종료 신호를 확인한다. 그동안 pika 가 하트비트도 처리한다.
_IDLE_TIMEOUT = 1.0


class Outcome(StrEnum):
    """핸들러가 돌려주는 판정.

    큐에 **DLQ 가 없다** (`StoreRabbitConfig` 에 dead-letter 설정이 없다). 그래서
    `requeue=True` 로 되돌리면 영구 실패가 무한 루프를 돈다. 기본은 ACK 이고, RETRY 는
    일시적 실패에만 쓰며 그것도 한 번만 되돌린다 (`_dispatch` 참고).
    """

    ACK = "ACK"
    """처리했거나, 재시도해도 같은 결과다. 잘못된 페이로드·없는 상품이 여기 속한다."""

    RETRY = "RETRY"
    """일시적 실패(DB 끊김 등). 첫 배달이면 되돌리고, 이미 재배달된 것이면 버린다."""


Handler = Callable[[bytes], Outcome]


@dataclass(frozen=True)
class Subscription:
    """무엇을 듣고 누가 처리할지."""

    exchange: str
    queue: str
    routing_key: str
    handler: Handler


# 종료 신호는 모든 컨슈머 스레드가 공유한다. 스레드는 구독마다 하나씩이라 리스트로 추적한다.
_stop = threading.Event()
_threads: list[threading.Thread] = []


def _connection_parameters() -> pika.ConnectionParameters:
    """`URLParameters` 를 쓰지 않는다 — 비밀번호에 특수문자가 있으면 URL 이 깨진다."""
    settings = get_settings()
    username, password = settings.rabbitmq_credentials
    return pika.ConnectionParameters(
        host=settings.rabbitmq_host,
        port=settings.rabbitmq_port,
        virtual_host=settings.rabbitmq_vhost,
        credentials=pika.PlainCredentials(username, password),
        heartbeat=settings.rabbitmq_heartbeat,
        connection_attempts=1,  # 재시도는 아래 재연결 루프가 한다
        socket_timeout=10,
    )


def _declare(channel: pika.adapters.blocking_connection.BlockingChannel, sub: Subscription) -> None:
    """Spring 과 **똑같은 속성**으로 선언한다.

    Spring 의 `new DirectExchange(name)` 은 durable·non-auto-delete·인자 없음,
    `new Queue(name, true)` 는 durable·non-exclusive·non-auto-delete·인자 없음이다.
    같은 속성이면 선언이 idempotent 하다.

    속성이 어긋나면 브로커가 406(PRECONDITION_FAILED)으로 채널을 닫는다. 나중에 백엔드가
    큐에 dead-letter 인자를 붙이면 여기서 터지므로, 그때는 선언을 빼고 `passive=True` 로
    바꾼다.
    """
    channel.exchange_declare(exchange=sub.exchange, exchange_type="direct", durable=True)
    channel.queue_declare(queue=sub.queue, durable=True)
    channel.queue_bind(queue=sub.queue, exchange=sub.exchange, routing_key=sub.routing_key)


def _dispatch(channel, method, body: bytes, sub: Subscription) -> None:
    """핸들러를 부르고 판정에 따라 ack/nack 한다.

    핸들러가 예외를 던져도 ack 한다. 그대로 두면 미확인 메시지가 쌓이고, requeue 하면
    DLQ 가 없어 같은 예외가 무한 반복된다. 스택트레이스를 남기는 편이 낫다.
    """
    try:
        outcome = sub.handler(body)
    except Exception:
        logger.exception("event=dropped cause=handler_error queue=%s", sub.queue)
        channel.basic_ack(method.delivery_tag)
        return

    if outcome is Outcome.RETRY and not method.redelivered:
        # 첫 배달이면 한 번 되돌린다. DLQ 가 없어 x-death 로 횟수를 셀 수 없으니
        # redelivered 플래그가 유일한 브레이크다.
        logger.warning("event=requeued queue=%s", sub.queue)
        channel.basic_nack(method.delivery_tag, requeue=True)
        return

    if outcome is Outcome.RETRY:
        logger.error("event=dropped cause=retry_exhausted queue=%s", sub.queue)
    channel.basic_ack(method.delivery_tag)


def _consume_once(sub: Subscription) -> None:
    """연결해서 종료 신호가 올 때까지 소비한다. 끊기면 예외를 올려 재연결을 유발한다.

    `start_consuming()` 대신 제너레이터 형태를 쓴다. `BlockingConnection` 은 스레드 안전하지
    않아서, 다른 스레드에서 부를 수 있는 것은 `add_callback_threadsafe` 하나뿐이다.
    `inactivity_timeout` 을 두면 **메인 스레드가 pika 를 아예 건드리지 않아도** 되고
    (Event 만 세운다), 유휴 중에도 하트비트가 흐른다.
    """
    connection = pika.BlockingConnection(_connection_parameters())
    try:
        channel = connection.channel()
        channel.basic_qos(prefetch_count=1)
        try:
            _declare(channel, sub)
        except ChannelClosedByBroker:
            logger.exception("event=topology_mismatch queue=%s — 선언 속성이 어긋난다", sub.queue)
            raise

        logger.info("event=consumer_started queue=%s host=%s", sub.queue, get_settings().rabbitmq_host)
        for method, _properties, body in channel.consume(
            sub.queue, auto_ack=False, inactivity_timeout=_IDLE_TIMEOUT
        ):
            if method is None:  # 유휴. 종료 신호를 확인할 틈
                if _stop.is_set():
                    break
                continue
            _dispatch(channel, method, body, sub)
            if _stop.is_set():
                break
        channel.cancel()  # 선반출된 메시지를 브로커로 되돌린다
    finally:
        try:
            connection.close()
        except Exception:  # 이미 끊겼으면 무의미하다
            pass


def run_forever(sub: Subscription) -> None:
    """끊기면 백오프를 두고 다시 붙는다. 별도 워커 프로세스는 이것을 main 에서 부른다."""
    delay = 1
    while not _stop.is_set():
        try:
            _consume_once(sub)
            delay = 1  # 성공적으로 붙었으면 리셋
        except (AMQPConnectionError, AMQPChannelError, OSError) as exc:
            logger.warning("event=reconnect in=%ss error=%s", delay, type(exc).__name__)
            # sleep 이 아니라 wait — 백오프 중에도 즉시 종료된다
            _stop.wait(delay + random.uniform(0, 0.5))
            delay = min(delay * 2, _RECONNECT_MAX_DELAY)
    logger.info("event=consumer_stopped queue=%s", sub.queue)


def start(sub: Subscription) -> None:
    """구독 하나를 백그라운드 스레드로 띄운다. 여러 번 불러 여러 큐를 구독할 수 있다.

    각 구독이 자기 스레드·자기 연결을 갖는다. `BlockingConnection` 이 스레드 안전하지 않아
    하나의 연결로 두 큐를 돌릴 수 없기 때문이다 — 큐가 몇 개 안 되므로 스레드 하나씩이 단순하다.

    같은 큐를 두 번 start 하지 않는다 (중복 구독 방지). `daemon=True` 는 안전망이라 스레드가
    물려도 프로세스 종료를 막지 않는다. 정상 종료는 `stop()` 이 담당한다.
    """
    if any(t.name == f"mq-{sub.queue}" and t.is_alive() for t in _threads):
        logger.warning("event=already_started queue=%s", sub.queue)
        return
    _stop.clear()
    thread = threading.Thread(target=run_forever, args=(sub,), name=f"mq-{sub.queue}", daemon=True)
    thread.start()
    _threads.append(thread)


def stop(timeout: float = 10.0) -> None:
    """모든 컨슈머에 종료 신호를 세우고 스레드를 기다린다. 메인 스레드는 pika 를 건드리지 않는다.

    `_stop` 이 공유 신호라 한 번 세우면 스레드 전부가 다음 유휴 틈(최대 `_IDLE_TIMEOUT`)에
    빠져나온다. 그래서 timeout 은 스레드마다가 아니라 전체에 한 번 준다 — 동시에 멈추므로.
    """
    _stop.set()
    for thread in _threads:
        thread.join(timeout)
        if thread.is_alive():
            logger.error("event=consumer_join_timeout queue=%s", thread.name)
    _threads.clear()


def parse_json(body: bytes) -> dict | None:
    """메시지 본문을 dict 로. 못 읽으면 None (호출자가 ACK 하고 버린다).

    Spring 의 `JacksonJsonMessageConverter` 가 보내므로 JSON 객체가 오는 것이 정상이다.
    """
    try:
        payload = json.loads(body)
    except json.JSONDecodeError:
        logger.error("event=dropped cause=malformed_json body_len=%d", len(body))
        return None
    if not isinstance(payload, dict):
        logger.error("event=dropped cause=not_an_object")
        return None
    return payload
