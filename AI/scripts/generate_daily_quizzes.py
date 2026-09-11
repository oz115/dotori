"""하루치 퀴즈 생성을 수동으로 돌리는 진입점.

FastAPI 서버를 띄우지 않고 DB 세션과 설정만 재사용한다. 트리거 방식(cron/RabbitMQ/HTTP)은
아직 정해지지 않았다 — `app/domains/shopping/router.py` 참고. 그 결정이 나기 전까지는
이 스크립트로 사람이 직접 돌리거나, 로컬 cron 에 걸어 둔다.

실행 (AI 폴더, venv 활성화 상태, DB 는 `ssh -N A505` 터널 필요):

    python -m scripts.generate_daily_quizzes
"""

import logging

from app.domains.quiz.services import generate_daily_quizzes


def main() -> None:
    logging.basicConfig(level=logging.INFO)
    generate_daily_quizzes()


if __name__ == "__main__":
    main()
