"""할일 가치 평가 로직. LLM 호출은 `app.llm.client` 를 거친다.

상점과 흐름은 같지만 기준 금액이 다르다 — `reward_points = value * weekly_allowance`
(백엔드 `ChoreRewardEvaluationMessage` javadoc). 그래서 `app.core.points.to_points()` 에
상점의 `daily_income(...)` 이 아니라 `Decimal(weekly_allowance)` 를 넘긴다.
기준 금액을 구하는 코드는 `app.core.points` 에 없다 — 도메인마다 다르기 때문이다.

노션 「AI 기능 정리」가 "설거지 1회 = 0.1 * 주 기본급" 처럼 주간 기본 용돈 대비 비율로
제시하기로 정했다. 할 일이 `weekly_reward_limit` 배분에서 빠진 이유는 그 한도로 묶으면
할 일을 자유롭게 등록할 수 없기 때문이다.
"""
