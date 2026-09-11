"""도토리 AI 서버 (FastAPI).

상점 가격 책정, 할일 보상 책정, 퀴즈 생성, 투자 회사/뉴스 생성을 담당한다.
자세한 배경은 노션 「AI 기능 정리」와 그 하위 페이지를 참고한다 (설계의 유일한 원본).

핸들러는 `async def` 가 아니라 `def` 로 쓴다. FastAPI 가 `def` 핸들러를
스레드풀에서 돌려 주므로, 동기 방식인 SQLAlchemy 를 써도 서버가 멈추지 않는다.

큐 컨슈머는 lifespan 에서 백그라운드 스레드로 띄운다 (`app/messaging/consumer.py`).
Spring AMQP 의 `SimpleMessageListenerContainer` 가 하던 일을 직접 하는 셈이라,
빈 생명주기에 해당하는 자리가 이 lifespan 이다.
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import anyio
from fastapi import APIRouter, Depends, FastAPI, HTTPException, status
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import get_session
from app.domains.investment.router import router as investment_router
from app.domains.quiz.router import router as quiz_router
from app.domains.shopping.consumer import SUBSCRIPTION as SHOPPING_SUBSCRIPTION
from app.domains.shopping.router import router as shopping_router
from app.domains.work.router import router as work_router
from app.messaging import consumer


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    """상점 가격 책정 큐 컨슈머를 띄우고 내린다.

    `store_pricing_consumer_enabled` 가 false 면 띄우지 않는다 — 공유 큐를 로컬에서
    가로채고 싶지 않을 때 쓴다.

    종료 시 `consumer.stop()` 은 블로킹이라 스레드로 넘긴다. 이벤트 루프에서 직접 join 하면
    그동안 다른 종료 작업이 멈춘다.

    DB 나 브로커가 없어도 서버는 뜬다. 컨슈머 스레드가 알아서 재연결을 반복하므로
    로컬에서 터널이나 docker compose 없이도 `/docs` 를 열어 볼 수 있다.
    """
    if get_settings().store_pricing_consumer_enabled:
        consumer.start(SHOPPING_SUBSCRIPTION)
    try:
        yield
    finally:
        await anyio.to_thread.run_sync(consumer.stop)


app = FastAPI(
    title="도토리 AI 서버",
    description="상점·할일 가격 책정, 퀴즈, 투자 뉴스 생성을 담당하는 내부 서비스",
    version="0.1.0",
    lifespan=lifespan,
)

health_router = APIRouter(tags=["health"])


@health_router.get("/health", summary="서버 생존 확인")
def health() -> dict[str, str]:
    """DB 를 건드리지 않는다. 컨테이너 헬스체크와 로드밸런서가 쓴다."""
    return {"status": "ok"}


@health_router.get("/health/db", summary="DB 연결 확인")
def health_db(session: Session = Depends(get_session)) -> dict[str, str]:
    """DB 까지 실제로 왕복한다. 로컬에서는 `ssh -N A505` 터널이 열려 있어야 한다."""
    try:
        session.execute(text("SELECT 1"))
    except SQLAlchemyError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"DB 에 연결할 수 없습니다: {exc.__class__.__name__}",
        ) from exc
    settings = get_settings()
    return {
        "status": "ok",
        "host": settings.postgres_host,
        "database": settings.postgres_db,
    }


app.include_router(health_router)
app.include_router(shopping_router)
app.include_router(work_router)
app.include_router(quiz_router)
app.include_router(investment_router)
