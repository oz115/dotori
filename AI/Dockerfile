# 도토리 AI 서버 (FastAPI)
# 빌드 (경로: 레포 루트/AI)
#   docker build -t dotori-ai .
# 실행 (환경변수는 파일이 아니라 주입으로 받는다)
#   docker run --rm -p 8000:8000 --env-file ../.env -e INTERNAL_TOKEN=... dotori-ai

FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    TZ=Asia/Seoul

WORKDIR /app

# 의존성을 먼저 설치해 소스 변경 시 캐시를 재사용한다.
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# root 로 돌리지 않는다.
RUN useradd --create-home --uid 1000 dotori && chown -R dotori:dotori /app
USER dotori

EXPOSE 8000

# slim 이미지에는 curl 이 없으므로 python 으로 확인한다.
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request, sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health').status == 200 else 1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
