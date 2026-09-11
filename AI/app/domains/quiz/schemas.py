"""퀴즈 생성 API의 요청/응답 스키마."""

from pydantic import BaseModel


class GenerateDailyQuizzesResponse(BaseModel):
    """오늘자 퀴즈 생성 결과 요약. `app.domains.quiz.services.DailyGenerationResult` 를 그대로 담는다."""

    pool_size: int
    inserted: int
    skipped: int
    failed_families: int
