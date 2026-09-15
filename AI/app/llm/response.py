"""LLM 응답을 다루는 공통 유틸. 도메인에 종속되지 않는다.

`client.py` 가 "어디로 부르는가" 를 맡고 이 모듈이 "응답을 어떻게 받는가" 를 맡는다.
어느 도메인이든 JSON 을 받거나 토큰 상한을 넘길 때 같은 문제를 만나므로 여기 둔다 —
도메인마다 따로 풀면 같은 함정에 두 번 빠진다.
"""

import re

_FENCE = re.compile(r"^```(?:json)?\s*|\s*```$")


def strip_fence(text: str) -> str:
    """앞뒤 코드블록 표시를 벗긴다.

    프롬프트로 "코드블록 표시를 붙이지 않는다" 고 지시해도 모델이 감쌀 때가 있다.
    `response_format={"type": "json_object"}` 를 줘도 게이트웨이·모델에 따라 새어 나온다.
    부탁으로 막지 말고 벗겨내는 편이 확실하다.
    """
    return _FENCE.sub("", text.strip())


def token_kwarg(model: str, max_output_tokens: int) -> dict[str, int]:
    """모델별 출력 토큰 상한 파라미터. 이름이 틀리면 요청 자체가 400 으로 거부된다.

    `gpt-5.x` 는 추론 모델이라 `max_completion_tokens` 를 쓰고, 그 외(`gpt-4o-mini`,
    `gpt-4.1-mini` 등)는 `max_tokens` 를 쓴다. 반대로 주면 실패한다.

    모델 이름을 표에 하나하나 등록하는 대신 접두사로 가른다 — 표로 두면 새 모델을 쓸 때마다
    등록을 잊고 400 을 만난다.

    상한은 과금이 아니라 잘림 방지용이다. 추론 모델에서는 추론 토큰도 이 상한을 같이 먹으므로
    넉넉히 주는 편이 안전하다.
    """
    key = "max_completion_tokens" if model.startswith("gpt-5") else "max_tokens"
    return {key: max_output_tokens}
