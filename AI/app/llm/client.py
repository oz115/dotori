"""GMS 경유 OpenAI 클라이언트.

가격 책정 근거 생성, 퀴즈 생성, 투자 뉴스 생성이 공통으로 이 클라이언트를 쓴다.
도메인 서비스(`app/domains/*/service.py`)는 OpenAI SDK를 직접 import 하지 않고
이 모듈을 거친다 — 나중에 재시도/로깅 정책을 바꿀 때 한 곳만 고치면 되게 하기 위해서다.
"""

from functools import lru_cache

from openai import OpenAI

from app.core.config import get_settings


@lru_cache
def get_client() -> OpenAI:
    """SSAFY GMS 를 거치는 OpenAI 호환 클라이언트.

    `base_url` 을 빼면 SDK 가 api.openai.com 으로 직접 가서 GMS 키로는 401 이 난다.
    경로 끝의 `/v1` 도 필수다 — 빼면 404 다.
    """
    settings = get_settings()
    return OpenAI(
        api_key=settings.gms_key,
        base_url=f"{settings.gms_base}/api.openai.com/v1",
    )
