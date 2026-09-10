"""설정 로딩.

값의 우선순위는 다음과 같다 (뒤가 이긴다).

1. 루트 `../.env`  — DB 접속 정보처럼 팀이 공유하는 값
2. `AI/.env`       — AI 서버에만 필요한 값 (LLM 키 등)
3. 실제 환경변수    — 배포 시 컨테이너에 주입하는 값

백엔드(`application.yaml`)도 루트 `.env`를 읽으므로 DB 정보를 두 번 관리하지 않는다.
"""

from functools import lru_cache
from urllib.parse import quote_plus

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        # 상대 경로는 실행 위치(AI/) 기준이다.
        env_file=("../.env", ".env"),
        env_file_encoding="utf-8",
        # 루트 .env 에는 JWT_SECRET 처럼 여기서 쓰지 않는 값도 있으므로 무시한다.
        extra="ignore",
    )

    # --- DB (루트 .env 재사용) ---
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "dotori_town"
    postgres_user: str = "dotori"
    postgres_password: str = ""

    # --- AI 서버 ---
    ai_port: int = 8000
    # Spring -> AI 서버 내부 호출 인증용 공유 토큰. 비어 있으면 검증을 건너뛴다(로컬 개발용).
    internal_token: str = ""

    # --- LLM (OpenAI) ---
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"

    @property
    def database_url(self) -> str:
        # 비밀번호에 @ : / 같은 문자가 있으면 URL 이 깨지므로 인코딩한다.
        password = quote_plus(self.postgres_password)
        return (
            f"postgresql+psycopg://{self.postgres_user}:{password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()
