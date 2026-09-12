"""할 일 보상 책정 API의 요청/응답 스키마."""

from decimal import Decimal

from pydantic import BaseModel


class ChoreRewardResponse(BaseModel):
    chore_id: int
    # 실제로 chores 에 썼는지. 이미 책정됐거나 삭제됐거나 산정에 실패하면 False 다.
    # 그 경우들이 다 정상 흐름이라 에러로 올리지 않는다 — 사유는 AI 서버 로그에 있다.
    rewarded: bool
    # 주간 기본 용돈의 배수. rewarded 가 False 면 None.
    value: Decimal | None = None
    reward_points: int | None = None
    # AI 가 낸 근거. 담을 컬럼이 없어 저장하지 않으므로(docs/db-boundary.md 협의 3번)
    # 부모 화면에 보여주려면 이 응답으로 받아 Spring 이 전달해야 한다.
    reason: str | None = None
