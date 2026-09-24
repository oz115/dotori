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

# ../AI-docs 의 실험 노트북을 돌릴 때만. 배포 이미지에 들어가면 안 되므로
# requirements.txt 에는 넣지 않는다
pip install ipykernel
```

DB 가 필요한 작업은 다른 창에서 터널을 먼저 연다: `ssh -N A505`

## 디렉토리 구조

```
AI/
├── app/
│   ├── core/         도메인에 종속되지 않는 공통 코드
│   │   ├── config.py     설정 로딩
│   │   ├── database.py   엔진/세션, ORM Base (경계 규칙 주석 필독)
│   │   ├── logging_setup.py 로그 설정. uvicorn 이 루트 로거를 안 건드려서 필요하다
│   │   ├── models.py     여러 도메인이 공유하는 읽기 전용 매핑 (families)
│   │   ├── points.py     가중치 -> 포인트 환산 규칙 (반올림, CHECK > 0 대응)
│   │   └── security.py   내부 호출 토큰 검증
│   ├── main.py       FastAPI 앱, 헬스체크, 라우터 등록, 컨슈머 기동(lifespan)
│   ├── llm/          LLM 호출 공통 (도메인 서비스가 SDK 를 직접 import 하지 않는다)
│   │   ├── client.py   어디로 부르는가 (GMS 경유 클라이언트)
│   │   └── response.py 응답을 어떻게 받는가 (코드펜스 제거, 모델별 토큰 파라미터)
│   ├── messaging/
│   │   └── consumer.py RabbitMQ 배관. 연결·재연결·ack 만 맡는다 (도메인 무관)
│   └── domains/      업무 영역별 패키지
│       └── {shopping,work,quiz,investment}/
│           ├── router.py     내부 API. `require_internal_token` 의존성 필수
│           ├── consumer.py   큐 핸들러 (큐로 일을 받는 도메인만)
│           ├── services.py   도메인 계산과 산정 흐름
│           ├── repository.py DB 조회·쓰기. SQL 이 이 파일 밖으로 안 나간다
│           ├── prompt.py     LLM 메시지 조립
│           ├── schemas.py    요청/응답 Pydantic 모델
│           ├── models.py     기존 테이블 매핑 (도메인이 쓰는 컬럼만)
│           └── prompts/      문구·few-shot 데이터 (조립 코드는 prompt.py)
├── docs/db-boundary.md  테이블별 읽기/쓰기 범위, 협의 필요 항목
├── Dockerfile
└── requirements.txt
```

도메인은 **기능이 아니라 업무 영역**으로 가른다. `shopping`(상점 상품 가격)과
`work`(할일 보상)를 하나의 `pricing` 으로 묶지 않는 이유는 두 가지다.

- 기준 금액이 다르다. 상점은 `value * 하루 소득`, 할일은 `value * 주간 기본 용돈` 이다
- 같은 `Base` 에 같은 `__tablename__` 을 두 번 등록하면 기동이 실패한다.
  여러 도메인이 공유하는 `families` 는 `core/models.py` 한 곳에만 둔다

공유해도 되는 것은 기준 금액이 아니라 반올림 규칙이라, `core/points.py` 에는 그 부분만 둔다.
기준 금액을 구하는 코드는 그것을 쓰는 도메인에 둔다 (하루 소득 -> `shopping/services.py`).

예적금은 도메인이 없다. 노션 「예/적금 상품 표 (Savings)」가 **이율·만기를 고정하고 가정별로
다른 것은 상품당 최대 납입 가능 금액뿐**이라고 정했고, 그 역산은 규칙 기반이라 계산 주체가
백엔드다. AI 는 그 범위 안에서 상품·납입 계획을 추천하고 계산 결과를 설명하는 몫만 갖는다.
`docs/db-boundary.md` 도 예적금 테이블을 "접근 안 함" 으로 두고 있다.

도메인 안에서도 책임을 나눈다. 파일 하나가 커지는 것 자체보다, **한 파일에서 DB·프롬프트·
LLM 호출·흐름 제어를 같이 고치게 되는 것**이 문제다. diff 에서 원인이 섞이고 한쪽을 바꿀 때
다른 쪽을 깨뜨린다. `services.py` 는 도메인 계산과 흐름만 갖고, SQL 은 `repository.py`,
메시지 조립은 `prompt.py` 로 뺀다.

여러 도메인이 같은 문제를 만나면 `app/` 공통으로 올린다 — 코드펜스 제거나 모델별 토큰
파라미터는 상점 전용이 아니라서 `app/llm/response.py` 에 있다. 도메인마다 따로 풀면 같은
함정에 두 번 빠진다.

프롬프트 문구와 few-shot 예시는 도메인의 `prompts/` 밑에 파일로 둔다. 방대한 문자열을
코드에 박으면 기준을 다듬을 때마다 서비스 코드를 건드리게 되고, diff 에서 로직 변경과
문구 변경이 섞인다. `investment/prompts/news_system_prompt.md` 가 그 예다.

프롬프트를 서버에 넣기 전에 눌러 보는 실험 노트북은 **`../AI-docs/` 에 둔다** (gitignore
대상인 개인 작업공간). `AI/` 안에 두면 배포 이미지에 노트북이 섞이고, `ipykernel` 같은
실험 전용 도구가 `requirements.txt` 로 새어 들어온다.

## 코드 스타일

- PEP 8. 들여쓰기는 **스페이스 4칸** (백엔드 Java 는 탭이지만 Python 은 스페이스다)
- 최대 줄 길이 120자 (팀 Java 규칙과 맞춤)
- 타입 힌트를 붙인다
- 핸들러는 `async def` 대신 `def` 로 쓴다. FastAPI 가 스레드풀에서 돌려 주므로
  동기 SQLAlchemy 를 써도 이벤트 루프를 막지 않는다. `async def` 안에서 동기 DB 호출을
  하면 서버 전체가 멈춘다

## DB 규칙 (필독)

**이 서버는 DB 테이블을 하나도 소유하지 않는다.** 팀원들이 이미 만들어 둔 테이블을
활용하는 것이 이 서버의 역할이다. 스키마 주인은 백엔드(Spring)이고, `ddl-auto: none` 이라
Hibernate 가 스키마를 만들지도 바꾸지도 않는다 — 원본은
`backend/docker/initdb/01-schema.sql` 이다.

### 절대 하지 않는 것

- **테이블을 만들지 않는다.** `CREATE TABLE` 을 실행하지 않는다.
  `ai_` 접두사를 붙인 AI 전용 테이블도 만들지 않는다
- `Base.metadata.create_all()` 을 호출하지 않는다 — 남의 테이블까지 건드린다
- **Alembic 을 쓰지 않는다** — `--autogenerate` 가 모르는 테이블에 `drop_table` 을 생성한다
- 남의 테이블에 컬럼이나 제약을 직접 추가하지 않는다

### 필요한 컬럼이 없을 때

혼자 처리할 수 없다. `ddl-auto: none` 이라 Hibernate 가 컬럼을 만들어 주지 않고,
스키마 원본인 `backend/docker/initdb/01-schema.sql` 을 고쳐야 하기 때문이다.

1. 어느 도메인 담당자의 테이블인지 확인한다
2. 컬럼 이름·타입·의미를 정리해서 담당자에게 요청한다
3. 합의되면 담당자가 **엔티티와 `01-schema.sql` 을 함께** 고친다.
   공유 EC2 DB 에는 자동 반영되지 않으므로 인프라 담당자와 반영 시점을 맞춘다
4. DB 에 실제로 반영된 뒤에 AI 서버가 그 컬럼을 쓴다
5. 합의 내용을 [docs/db-boundary.md](docs/db-boundary.md) 에 기록한다

지금 열려 있는 요청은 [docs/db-boundary.md](docs/db-boundary.md) 의
"협의가 필요한 항목" 에 정리해 두었다.

### 읽고 쓸 때

- 테이블별 접근 범위는 [docs/db-boundary.md](docs/db-boundary.md) 를 따른다
- 남의 테이블 쓰기는 **사전 합의된 컬럼만** 건드린다 (예: `store_products.value`)
- 스키마 변경이 합의되면 [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) 갱신은 팀 규칙이다

## 규칙

- 네이밍/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- API 명세는 노션(작업 > API 명세)이 유일한 원본이다.
- **기능 설계도 노션이 원본이다** (「AI 기능 정리」와 그 하위 페이지 — 가격 설정, 예/적금 상품 표,
  투자, 회사 리스트). 배분 비율·이율·수식이 필요하면 거기서 확인한다.
- 루트 `AI-docs/`는 gitignore 대상인 **개인 작업공간의 초안**이다. 노션에 옮겨지기 전 메모라
  이미 폐기된 수치가 남아 있다. **코드 주석에서 근거로 인용하지 않는다.**
- 환경변수는 `.env`(gitignore 대상)로 관리하고 `.env.example`을 최신 상태로 유지한다.
