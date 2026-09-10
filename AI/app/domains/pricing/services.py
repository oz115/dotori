"""가격 책정 로직.

계획(AI/CLAUDE.md 참고): 규칙 기반 계산(D = (weekly_allowance + weekly_reward_limit) / 7,
가중치 -> 포인트 환산)을 먼저 구현하고, LLM(`app.llm.client`)은 근거 문구 생성에 그 뒤에 붙인다.
"""
