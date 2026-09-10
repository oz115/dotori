"""OpenAI 클라이언트.

가격 책정 근거 생성, 퀴즈 생성, 투자 뉴스 생성이 공통으로 이 클라이언트를 쓴다.
도메인 서비스(`app/domains/*/service.py`)는 OpenAI SDK를 직접 import 하지 않고
이 모듈을 거친다 — 나중에 재시도/로깅 정책을 바꿀 때 한 곳만 고치면 되게 하기 위해서다.
"""

from functools import lru_cache

from openai import OpenAI

from app.core.config import get_settings


@lru_cache
def get_client() -> OpenAI:
    return OpenAI(api_key=get_settings().openai_api_key)
