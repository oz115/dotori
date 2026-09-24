"""퀴즈 생성 API의 요청/응답 스키마."""

from pydantic import BaseModel


class GenerateDailyQuizzesResponse(BaseModel):
    """오늘자 퀴즈 생성 결과 요약. `app.domains.quiz.services.DailyGenerationResult` 를 그대로 담는다."""

    pool_size: int
    inserted: int
    skipped: int
    failed_families: int


class AssignFamilyQuizRequest(BaseModel):
    """가족 하나에 오늘자 퀴즈를 배정해 달라는 요청."""

    family_id: int


class AssignFamilyQuizResponse(BaseModel):
    """가족 하나 배정 결과. `app.domains.quiz.services.FamilyAssignResult` 를 그대로 담는다."""

    family_id: int
    # 배정할 자녀가 없으면 티어를 정할 근거도 없어 None 이다
    tier: str | None
    pool_size: int
    inserted: int
    skipped: int
    failed_families: int
    # 형제가 이미 받은 오늘 문제를 그대로 쓴 경우 True (LLM 호출 없음)
    reused_sibling_quiz: bool
