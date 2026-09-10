# AGENTS.md (AI 서버)

AI 서버(`AI/`) 작업 시 참고하는 지침입니다. 저장소 공통 지침은 [../AGENTS.md](../AGENTS.md)를 따릅니다.
실행 방법과 인프라 정보는 [README.md](README.md)에 있습니다.

## 스택

- 언어/프레임워크: Python 3.11, FastAPI
- DB 접근: SQLAlchemy 2.x (동기) + psycopg 3
- 설정: pydantic-settings (루트 `../.env` → `AI/.env` → 실제 환경변수 순으로 뒤가 이김)

## 자주 쓰는 명령어

```powershell
# PowerShell / 경로: AI (venv 활성화 상태)
.\.venv\Scripts\Activate.ps1
fastapi dev app/main.py          # 로컬 실행 (http://127.0.0.1:8000/docs)
pip install -r requirements.txt  # 의존성 설치

# 노트북(notebooks/)을 돌릴 때만. requirements.txt 에는 넣지 않는다
pip install openai ipykernel
```

DB 가 필요한 작업은 다른 창에서 터널을 먼저 연다: `ssh -N A505`

## 디렉토리 구조

```
AI/
├── app/
│   ├── config.py     설정 로딩
│   ├── db.py         엔진/세션 (경계 규칙 주석 필독)
│   ├── security.py   내부 호출 토큰 검증
│   └── main.py       FastAPI 앱, 헬스체크
├── notebooks/        프롬프트·산정 로직 실험용 주피터 노트북
├── docs/db-boundary.md  테이블별 읽기/쓰기 범위, 협의 필요 항목
├── Dockerfile
└── requirements.txt
```

`notebooks/` 는 LLM 프롬프트와 산정 수식을 먼저 눌러 보는 자리다. 서버 코드로 옮기기 전
단계이므로 `app/` 을 import 하지 않고 자체적으로 완결되게 쓴다.
노트북 전용 도구(`openai`, `ipykernel`)는 **`requirements.txt` 에 넣지 않는다** —
배포 이미지에 들어가면 안 된다. 서버가 LLM 을 부르게 될 때 정식으로 추가한다.

## 코드 스타일

- PEP 8. 들여쓰기는 **스페이스 4칸** (백엔드 Java 는 탭이지만 Python 은 스페이스다)
- 최대 줄 길이 120자 (팀 Java 규칙과 맞춤)
- 타입 힌트를 붙인다
- 핸들러는 `async def` 대신 `def` 로 쓴다. FastAPI 가 스레드풀에서 돌려 주므로
  동기 SQLAlchemy 를 써도 이벤트 루프를 막지 않는다. `async def` 안에서 동기 DB 호출을
  하면 서버 전체가 멈춘다

## DB 규칙 (필독)

**이 서버는 DB 테이블을 하나도 소유하지 않는다.** 팀원들이 이미 만들어 둔 테이블을
활용하는 것이 이 서버의 역할이다. 스키마 주인은 백엔드(Spring)이고,
`ddl-auto: update` 로 Hibernate 가 엔티티에서 테이블을 만든다.

### 절대 하지 않는 것

- **테이블을 만들지 않는다.** `CREATE TABLE` 을 실행하지 않는다.
  `ai_` 접두사를 붙인 AI 전용 테이블도 만들지 않는다
- `Base.metadata.create_all()` 을 호출하지 않는다 — 남의 테이블까지 건드린다
- **Alembic 을 쓰지 않는다** — `--autogenerate` 가 모르는 테이블에 `drop_table` 을 생성한다
- 남의 테이블에 컬럼이나 제약을 직접 추가하지 않는다

### 필요한 컬럼이 없을 때

혼자 처리할 수 없다. 컬럼은 백엔드 엔티티에 선언되어야 Hibernate 가 만들기 때문이다.

1. 어느 도메인 담당자의 테이블인지 확인한다
2. 컬럼 이름·타입·의미를 정리해서 담당자에게 요청한다
3. 합의되면 담당자가 엔티티에 선언하고, DB 에 반영된 뒤에 AI 서버가 그 컬럼을 쓴다
4. 합의 내용을 [docs/db-boundary.md](docs/db-boundary.md) 에 기록한다

지금 열려 있는 요청은 [docs/db-boundary.md](docs/db-boundary.md) 의
"협의가 필요한 항목" 에 정리해 두었다.

### 읽고 쓸 때

- 테이블별 접근 범위는 [docs/db-boundary.md](docs/db-boundary.md) 를 따른다
- 남의 테이블 쓰기는 **사전 합의된 컬럼만** 건드린다 (예: `store_products.value`)
- 스키마 변경이 합의되면 [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) 갱신은 팀 규칙이다

## 규칙

- 네이밍/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- API 명세는 노션(작업 > API 명세)이 유일한 원본이다.
- 환경변수는 `.env`(gitignore 대상)로 관리하고 `.env.example`을 최신 상태로 유지한다.
- 설계 문서는 `backend/AI-docs/`에 있다 (git 추적 대상 아님).
