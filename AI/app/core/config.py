"""설정 로딩.

AI 서버는 나중에 독립 프로세스로 뜨므로 필요한 값을 `AI/.env` 에 자체적으로 둔다 —
Spring 과 같은 DB·브로커라도 여기에 적는다.

값의 우선순위는 다음과 같다 (뒤가 이긴다).

1. 루트 `../.env`  — 모노레포로 함께 돌릴 때 먼저 읽는다
2. `AI/.env`       — AI 서버가 실제로 쓰는 값 (위를 덮는다)
3. 실제 환경변수    — 배포 시 컨테이너에 주입하는 값 (항상 최우선)
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

    # --- DB (Spring 과 같은 Postgres) ---
    postgres_host: str = "localhost"
    postgres_port: int = 5432
    postgres_db: str = "dotori_town"
    postgres_user: str = "dotori"
    postgres_password: str = ""

    # --- AI 서버 ---
    ai_port: int = 8000
    # Spring -> AI 서버 내부 호출 인증용 공유 토큰. 비어 있으면 검증을 건너뛴다(로컬 개발용).
    internal_token: str = ""

    # --- 로깅 ---
    # app.* 로거 레벨. 상점 근거(reason)와 LLM 호출 이력을 로그로만 남기므로
    # INFO 가 기본이다 (`app/core/logging_setup.py` 참고).
    log_level: str = "INFO"

    # --- RabbitMQ (Spring 과 같은 브로커) ---
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    # 자격증명은 소스에 두지 않는다. 값은 .env 에서 와야 한다 (로컬은 guest/guest).
    # 미설정이면 빈 값으로 접속을 시도하다 AMQP 403 이 나므로, .env 를 반드시 채운다.
    rabbitmq_username: str = ""
    rabbitmq_password: str = ""
    rabbitmq_vhost: str = "/"
    # LLM 호출로 핸들러가 붙잡혀 있어도 하트비트가 끊기지 않게 넉넉히 둔다.
    # BlockingConnection 은 우리가 pika 를 호출할 때만 I/O 를 처리한다.
    rabbitmq_heartbeat: int = 180
    # false 면 컨슈머를 띄우지 않는다. 공유 큐를 로컬에서 가로채고 싶지 않을 때 쓴다.
    store_pricing_consumer_enabled: bool = True

    # --- LLM (SSAFY GMS — OpenAI 호환 게이트웨이) ---
    # AI 서버만 GMS 를 호출하므로(Spring 은 안 씀) 값은 AI/.env 에 둔다. OpenAI 에 직접
    # 붙지 않고 GMS 를 거치므로 base 가 필요하다 — 실제 요청 주소는
    # `{gms_base}/api.openai.com/v1` 이다 (`app.llm.client` 참고).
    gms_key: str = ""
    gms_base: str = "https://gms.ssafy.io/gmsapi"
    # GMS 가 여러 모델을 중계한다. 모델을 바꾸면 토큰 파라미터 이름이 달라질 수 있다 —
    # gpt-4o-mini / gpt-4.1-mini 는 `max_tokens`, gpt-5.x 는 `max_completion_tokens` 다.
    openai_model: str = "gpt-4o-mini"
    # 이야기 구성과 여러 출력 규칙을 함께 지켜야 하는 금융 퀴즈 전용 모델이다.
    quiz_openai_model: str = "gpt-5.4-mini"

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
