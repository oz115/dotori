"""도토리 AI 서버 (FastAPI).

가격 책정, 퀴즈 생성, 투자 회사/뉴스 생성을 담당한다.
자세한 배경은 `backend/AI-docs/` 의 설계 문서를 참고한다.

핸들러는 `async def` 가 아니라 `def` 로 쓴다. FastAPI 가 `def` 핸들러를
스레드풀에서 돌려 주므로, 동기 방식인 SQLAlchemy 를 써도 서버가 멈추지 않는다.
"""

from fastapi import APIRouter, Depends, FastAPI, HTTPException, status
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import get_session
from app.domains.investment.router import router as investment_router
from app.domains.pricing.router import router as pricing_router
from app.domains.quiz.router import router as quiz_router

app = FastAPI(
    title="도토리 AI 서버",
    description="가격 책정·퀴즈·투자 뉴스 생성을 담당하는 내부 서비스",
    version="0.1.0",
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
app.include_router(pricing_router)
app.include_router(quiz_router)
app.include_router(investment_router)
