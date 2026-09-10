# 도토리 AI 서버

가격 책정, 퀴즈 생성, 투자 회사/뉴스 생성을 담당하는 **내부 서비스**다.
백엔드(Spring Boot)와 같은 PostgreSQL 을 쓰고, 별도 프로세스로 뜬다.

- 언어/프레임워크: Python 3.11, FastAPI
- 설계 문서: 노션 「AI 기능 정리」와 그 하위 페이지 (유일한 원본)
  - 루트 `AI-docs/`는 노션에 옮겨지기 전 개인 메모다. 폐기된 수치가 섞여 있어 근거로 쓰지 않는다

## 경계

```
React Native ──> Spring Boot ──(HTTP, 내부망)──> AI 서버 ──> PostgreSQL
                     │                                          ▲
                     └──────────────────────────────────────────┘
                        스키마 주인은 Spring (ddl-auto: none, 원본은 01-schema.sql)
```

- **프론트는 이 서버를 직접 부르지 않는다.** 사용자 인증(JWT)은 Spring 이 처리하고,
  Spring 이 이 서버를 부를 때 `X-Internal-Token` 헤더로 내부 호출임을 증명한다.
- **이 서버는 테이블을 하나도 소유하지 않는다.** 스키마 주인은 Spring 이고,
  팀이 이미 만들어 둔 테이블을 합의된 범위에서만 읽고 쓴다.
  필요한 컬럼이 없으면 직접 추가하지 않고 도메인 담당자와 협의한다.
  테이블별 접근 범위는 [`docs/db-boundary.md`](docs/db-boundary.md) 참고.

## 로컬 실행

### 최초 1회

```powershell
# PowerShell / 경로: c:\Users\SSAFY\dev\S15P21A505\AI
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
Copy-Item .env.example .env    # 필요한 값만 채운다 (DB 정보는 루트 .env 재사용)
```

`Activate.ps1` 이 실행 정책 때문에 막히면 한 번만:
`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

### 매번

```powershell
# PowerShell 창 A — EC2 DB 로 가는 터널 (열어 둔다)
ssh -N A505
```

```powershell
# PowerShell 창 B / 경로: c:\Users\SSAFY\dev\S15P21A505\AI
.\.venv\Scripts\Activate.ps1
fastapi dev app/main.py
```

- API 문서: http://127.0.0.1:8000/docs
- 터널 없이도 서버는 뜬다. `GET /health` 는 통과하고 `GET /health/db` 만 503 이 된다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| GET | `/health` | 없음 | 서버 생존 확인. DB 를 건드리지 않는다. 컨테이너 헬스체크용 |
| GET | `/health/db` | 없음 | DB 까지 왕복. 실패 시 503 |
| GET | `/docs` | 없음 | 자동 생성된 API 문서 (Swagger UI) |

업무 엔드포인트는 아직 없다. 추가할 때는 `dependencies=[Depends(require_internal_token)]` 를 붙인다.

## 환경변수

값의 우선순위는 **루트 `../.env` → `AI/.env` → 실제 환경변수** 순으로 뒤가 이긴다.
DB 정보를 두 번 관리하지 않으려고 백엔드와 같은 루트 `.env` 를 읽는다.

| 이름 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `POSTGRES_HOST` | | `localhost` | 루트 `.env` 재사용 |
| `POSTGRES_PORT` | | `5432` | 루트 `.env` 재사용 |
| `POSTGRES_DB` | | `dotori_town` | 루트 `.env` 재사용 |
| `POSTGRES_USER` | ✅ | `dotori` | 루트 `.env` 재사용 |
| `POSTGRES_PASSWORD` | ✅ | | 루트 `.env` 재사용 |
| `AI_PORT` | | `8000` | 서버 포트 |
| `INTERNAL_TOKEN` | 배포 시 ✅ | (빈 값) | Spring 과 공유하는 내부 호출 토큰. **빈 값이면 토큰 검증을 건너뛴다** |

## 인프라 담당자 확인 사항

배포에 필요한 정보를 아래에 모았다. 컨테이너 하나가 추가되는 형태다.

**컨테이너 스펙**

| 항목 | 값 |
| --- | --- |
| 빌드 컨텍스트 | `AI/` (Dockerfile 은 [`AI/Dockerfile`](Dockerfile)) |
| 노출 포트 | `8000` |
| 헬스체크 | `GET /health` (200 기대). Dockerfile 에 `HEALTHCHECK` 내장 |
| 실행 유저 | non-root (`dotori`, uid 1000) |
| 베이스 이미지 | `python:3.11-slim` |
| 상태 | **stateless** — 볼륨 불필요. 상태는 전부 PostgreSQL 에 있다 |

**확인/결정이 필요한 것**

- [ ] **8000 포트를 외부에 열지 말 것.** Spring 만 내부에서 호출한다.
      Nginx 에도 이 서버로 가는 경로를 만들지 않는다.
- [ ] `INTERNAL_TOKEN` 을 Jenkins Credentials 에 등록하고 **Spring 과 AI 서버에 같은 값**을 주입.
      생성: `openssl rand -hex 32`
- [ ] DB 계정: AI 서버용 role 을 따로 줄지, `dotori` 공용으로 쓸지 결정.
      따로 준다면 [`docs/db-boundary.md`](docs/db-boundary.md) 의 범위
      (대부분 `SELECT`, 일부 테이블에 `INSERT`/`UPDATE`)면 충분하다.
- [ ] Jenkins job 추가 (백엔드 파이프라인과 같은 방식이면 그대로 복제하면 된다).
- [ ] 컨테이너에서 DB 로 붙는 호스트 값 확인. 로컬은 `ssh -N A505` 터널로 `localhost` 지만
      EC2 안에서는 다른 값일 수 있다.
- [ ] Python 의존성은 `requirements.txt` 로 고정되어 있다. 별도 빌드 도구 불필요.

pgvector 는 **쓰지 않기로 했다.** DB 이미지 교체나 확장(`CREATE EXTENSION vector`) 설치는 필요 없다.

## 주의사항

- **테이블을 만들지 않는다.** `CREATE TABLE` 도, `Base.metadata.create_all()` 도 쓰지 않는다.
- **Alembic 을 쓰지 않는다.** `--autogenerate` 가 자기가 모르는 테이블(`users` 등)에 대해
  `drop_table` 을 만들어 낸다. 공유 DB 에서는 사고로 직결된다.
- 남의 테이블에 컬럼을 직접 추가하지 않는다. 컬럼은 백엔드 엔티티에 선언되어야 생긴다.
- EC2 DB 는 팀이 공유한다. 커넥션 풀을 키우지 않는다 (현재 `pool_size=5`).
