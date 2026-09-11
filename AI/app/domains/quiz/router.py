"""퀴즈 생성 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다."""

from fastapi import APIRouter, Depends

from app.core.security import require_internal_token
from app.domains.quiz.schemas import GenerateDailyQuizzesResponse
from app.domains.quiz.services import generate_daily_quizzes

router = APIRouter(
    prefix="/internal/quiz",
    tags=["quiz"],
    dependencies=[Depends(require_internal_token)],
)


@router.post("/generate", summary="오늘자 퀴즈 생성 + 가족 배정")
def generate_today() -> GenerateDailyQuizzesResponse:
    """하루 한 번(스프링 스케줄러 -> 이 엔드포인트) 호출을 가정한다.

    같은 날 여러 번 불려도 DB 는 안전하다 — `assign_and_insert` 가 `(child_id, quiz_date)`
    존재 여부를 확인하고 건너뛴다. 다만 LLM 호출(9개 생성)은 매번 다시 나가므로
    하루 한 번만 부르는 게 전제다.
    """
    result = generate_daily_quizzes()
    return GenerateDailyQuizzesResponse(
        pool_size=result.pool_size,
        inserted=result.inserted,
        skipped=result.skipped,
        failed_families=result.failed_families,
    )
