"""퀴즈 생성 내부 API. Spring 이 `X-Internal-Token` 헤더로 호출한다."""

from fastapi import APIRouter, Depends

from app.core.security import require_internal_token
from app.domains.quiz.schemas import (
    AssignFamilyQuizRequest,
    AssignFamilyQuizResponse,
    GenerateDailyQuizzesResponse,
)
from app.domains.quiz.services import assign_family_quiz, generate_daily_quizzes

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


@router.post("/assign", summary="가족 하나에 오늘자 퀴즈 배정")
def assign_family(request: AssignFamilyQuizRequest) -> AssignFamilyQuizResponse:
    """자녀가 가족에 합류했을 때 스프링이 부른다 (FamilyChildConnectedEvent).

    일일 크론(`/generate`)은 도는 시점의 자녀만 훑어서, 그 뒤에 들어온 아이는
    다음 날 자정까지 퀴즈가 없었다. 이 엔드포인트가 그 틈을 메꾼다.

    멱등하다 — 같은 가족을 여러 번 부르면 두 번째부터는 `skipped` 로만 잡히고
    LLM 호출도 나가지 않는다 (형제 문제 재사용 경로로 빠진다).
    """
    result = assign_family_quiz(request.family_id)
    return AssignFamilyQuizResponse(
        family_id=result.family_id,
        tier=result.tier,
        pool_size=result.pool_size,
        inserted=result.inserted,
        skipped=result.skipped,
        failed_families=result.failed_families,
        reused_sibling_quiz=result.reused_sibling_quiz,
    )
