"""로그 설정. 앱이나 워커가 시작할 때 한 번 부른다.

uvicorn 은 자기 로거(`uvicorn.*`)만 설정하고 루트 로거는 손대지 않는다. 루트 기본 레벨이
`WARNING` 이라, 설정하지 않으면 `logger.info(...)` 로 남기는 것이 전부 버려진다.
`logger.exception` 만 보이는 상태가 되는데 그게 제일 나쁘다 — 실패는 보이고 정상 처리는
안 보여서 서버가 일하고 있는지 알 수 없다.

이 서버에는 로그로만 남기기로 한 정보가 있다. 상점 가격 책정의 근거(`reason`)는 담을 컬럼이
없어서 로그가 유일한 기록이고(`docs/db-boundary.md` 협의 3번), LLM 호출 이력도 테이블을
만들지 않고 로그로 대체하기로 했다. 그래서 `INFO` 가 보이는 것이 설계의 일부다.

`main.py` 의 lifespan 이 부른다. 컨슈머를 별도 워커 프로세스로 분리하면 그쪽 진입점에서도
같이 불러야 한다 — 그래서 `main.py` 안에 두지 않고 모듈로 뺐다.
"""

import logging

from app.core.config import get_settings

_FORMAT = "%(asctime)s %(levelname)-8s %(name)s %(message)s"

_configured = False


def configure_logging() -> None:
    """루트 핸들러를 달고 `app.*` 로거 레벨을 맞춘다. 두 번 불러도 안전하다.

    루트를 `WARNING` 으로 두고 `app` 만 낮추는 이유: 라이브러리(`pika`, `httpx`, `openai`)의
    `INFO` 가 섞이면 우리 로그가 묻힌다. `pika` 는 연결마다, `httpx` 는 요청마다 한 줄씩
    남긴다.
    """
    global _configured
    if _configured:
        return

    level = getattr(logging, get_settings().log_level.upper(), logging.INFO)
    # force=True — uvicorn 이 먼저 핸들러를 달았어도 포맷을 우리 것으로 맞춘다.
    logging.basicConfig(level=logging.WARNING, format=_FORMAT, force=True)
    logging.getLogger("app").setLevel(level)
    _configured = True

    logging.getLogger(__name__).info("event=logging_configured level=%s", logging.getLevelName(level))
