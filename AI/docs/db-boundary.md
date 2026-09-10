# AI 서버의 DB 접근 범위

**이 서버는 테이블을 하나도 소유하지 않는다.** 팀원들이 이미 만들어 둔 테이블을 활용하는 것이
이 서버의 역할이다. 스키마 주인은 백엔드(Spring)이고 `ddl-auto: none` 이다 —
스키마 원본은 `backend/docker/initdb/01-schema.sql` 이고 Hibernate 는 손대지 않는다.

필요한 컬럼이 없으면 직접 추가하지 않는다. 담당자가 **엔티티와 위 SQL 파일을 함께** 고쳐야
하고, 공유 EC2 DB 에는 자동 반영되지 않아 인프라 담당자와 맞춰야 한다.
아래 "협의가 필요한 항목" 절차를 따른다.

기준 시점: 2026-09-08, EC2 `dotori_town` (public 스키마 테이블 28개, 전부 0행)

## 접근 범위

| 테이블 | AI 서버의 권한 | 무엇을 하는가 |
| --- | --- | --- |
| `store_products` | 읽기 + `value`, `price` 쓰기 | 물품 가치를 평가해 채운다 |
| `chores` | 읽기 + `reward_points` 쓰기 | 할 일 보상 포인트 책정 |
| `quizzes` | INSERT | 생성한 퀴즈를 아이·날짜별로 넣는다 |
| `words` | 읽기 (초기 적재 시 INSERT) | 경제용어 사전. 퀴즈 생성의 재료 |
| `investment_products` | 읽기 + `current_price` 쓰기 | 회사 10개 고정, 주가 갱신 |
| `investment_news` | INSERT | 매일 뉴스 3건 생성 |
| `investment_news_products` | INSERT | 뉴스와 회사의 N:N 연결 |
| `investment_price_history` | INSERT | 주가 변동 이력 |
| `families`, `users` | **읽기만** | 온보딩 A(`weekly_reward_limit`)·B(`weekly_allowance`) 값, `financial_knowledge_level`, 아이 정보 조회 |
| 그 밖의 모든 테이블 | 접근 안 함 | 포인트 지갑, 예적금, 계좌 연동 등은 백엔드 담당 |

`store_products.value`(NUMERIC)는 가중치, `price`(INTEGER)는 포인트다.
둘 다 nullable 이라 AI 가 채우기 전에는 NULL 이다.

## 협의가 필요한 항목

절차는 다음과 같다.

1. 어느 도메인 담당자의 테이블인지 확인한다
2. 컬럼 이름·타입·의미를 정리해서 담당자에게 요청한다
3. 합의되면 담당자가 백엔드 엔티티에 선언하고, DB 에 반영된 뒤에 AI 서버가 그 컬럼을 쓴다
4. 합의 결과를 이 문서에 기록한다

### 1. 온보딩 A·B 값을 저장할 컬럼 (해결됨, 2026-09-10)

`families` 에 반영됐다. 요청 시 이름(`weekly_max_earning`)과 실제 컬럼명(`weekly_reward_limit`)이
다르니 코드에서 실제 이름을 쓴다. 덤으로 요청하지 않았던 `financial_knowledge_level`
(아이 금융 이해도, `VARCHAR(20)`, CHECK 로 값 제한 — 퀴즈 난이도·보상 산정에 참고)도 추가됐다.

| 컬럼 | 타입 | 의미 |
| --- | --- | --- |
| `weekly_reward_limit` | INTEGER | A — 예적금·투자·퀴즈로 주당 벌 수 있는 최대 포인트 (할 일 제외) |
| `weekly_allowance` | INTEGER | B — 주간 기본 용돈(포인트 환산) |
| `financial_knowledge_level` | VARCHAR(20) | 아이의 금융 이해도 (`BEGINNER` 등). AI 가 보상·퀴즈 난이도 참고 |

담당: 온보딩 · 가족 도메인 / 상태: **DB 반영 완료** — 세 컬럼 모두 `NOT NULL DEFAULT` 라
백엔드 `Family` 엔티티가 아직 이 필드들을 매핑하지 않아도 INSERT 는 깨지지 않는다.
단, 엔티티 매핑 전까지는 온보딩 화면에서 실제로 이 값을 받아 저장하는지 백엔드와 확인 필요.

이제 `D = (A + B) / 7` 계산에 요청 파라미터가 아니라 DB 값을 바로 쓸 수 있다.

### 2. 뉴스의 상승/하락 방향을 담을 곳이 없다

`investment_news` 는 `title`, `content`, `published_at` 뿐이고
`investment_news_products` 는 `investment_news_product_id`, `investment_news_id`,
`investment_product_id` 3개뿐이다. **뉴스가 호재인지 악재인지 저장할 컬럼이 없다.**

설계상 방향은 부가 정보가 아니라 핵심이다. 노션 「투자 (Investment)」는 하루 뉴스 3건을
**상승 2건 + 하락 1건**으로 정해 두었고, 그 구성이 곧 주간 수익 한도를 만든다.

요청안 (`investment_news`):

| 컬럼 | 타입 | 의미 |
| --- | --- | --- |
| `impact` | ENUM/VARCHAR | `POSITIVE` / `NEGATIVE` |

`investment_news_products` 가 아니라 `investment_news` 에 두기를 요청한다.
노션이 **`1 경제요인 : 1 회사`** 로 정해서 뉴스 하나가 가리키는 회사도 하나이기 때문이다.
조인 테이블에 두면 "뉴스 A 는 회사 X 에 호재, 회사 Y 에 악재" 같은 상태를 표현할 수 있게
되는데, 그건 지금 설계에 없는 경우다.

설계 v3 의 `evidence_grade`(근거 등급 A/B)는 **더 이상 필요하지 않다.** 한 뉴스가 회사마다
다른 영향을 주는 구조가 사라졌기 때문이다. 이전 요청안에서 철회한다.

담당: 투자 도메인 / 상태: **요청 전** (1:1 확정에 맞춰 2026-09-10 재작성)

### 3. AI 가 책정한 근거(reason)를 담을 곳이 없다

부모에게 "왜 이 가격인지"를 보여줘야 하는데 `store_products` 에는 `description` 뿐이고,
부모가 수정하기 전의 AI 제안값도 남지 않는다.

두 가지 방법이 있다.

- **저장하지 않는다 (v1 권장).** 근거를 API 응답에만 담아 Spring 이 부모 화면에 전달한다.
  스키마 변경이 필요 없어 지금 바로 진행할 수 있다.
- **컬럼을 추가한다.** 근거를 나중에 다시 보여줘야 한다면 `store_products` 에
  `ai_reason`(TEXT), `ai_suggested_price`(INTEGER) 추가를 상점 도메인 담당자와 협의한다.

담당: 상점 도메인 / 상태: **v1 은 저장 없이 진행**

## 테이블을 만들지 않고 해결하는 것들

| 필요 | 해결 방법 |
| --- | --- |
| LLM 호출 기록 (프롬프트·응답·토큰·지연) | 애플리케이션 로그로 남긴다. 컨테이너 표준출력 -> Docker/Jenkins 로그에서 조회 |
| 중복 책정 방지 (메시지큐가 같은 메시지를 두 번 전달) | 대상 행의 상태로 판단한다. `store_products.value IS NOT NULL` 이면 이미 책정된 것으로 보고 건너뛴다 |
| 가격 책정 이력 | 위 협의 3번이 합의될 때까지는 로그로만 남긴다 |

로그에 아이 이름처럼 개인정보를 필요 이상으로 남기지 않는다.

## 네이티브 ENUM 처리

팀 DB 는 네이티브 ENUM 을 쓴다. psycopg 로 확인한 결과:

- 파라미터에 문자열을 그대로 넘겨도 **캐스트 없이 동작한다** (비교·INSERT 모두 확인)
- 잘못된 라벨은 DB 가 거부한다: `invalid input value for enum ... "NOPE"`.
  즉 오타는 런타임에 바로 드러난다

| 타입 | 값 |
| --- | --- |
| `store_products_category` | `GAME`, `SNACK`, `TOY`, `OUTING`, `PRIVILEGE`, `ETC` |
| `words_category` | `INCOME`, `CONSUMPTION`, `SAVINGS`, `INVESTMENT`, `BANKING`, `ECONOMY`, `OTHER` |
| `investment_products_category` | `STOCK`, `BOND`, `ETF`, `FUND`, `OTHER` |
| `chores_repeat_type` | `DAY`, `WEEK`, `MONTH` |
| `users_role` | `PARENT`, `CHILD` |

## 팀에 공유할 사항

[`../../docs/ARCHITECTURE.md`](../../docs/ARCHITECTURE.md) 에는 `users.role` 과
`point_transactions.transaction_type` 을 VARCHAR 로 매핑했다고 적혀 있는데,
**실제 EC2 DB 의 두 컬럼은 네이티브 ENUM 이다.** 이 DB 가 Hibernate 가 아니라
ERD 에서 생성한 스크립트로 만들어졌기 때문으로 보인다. 백엔드 쪽 확인이 필요하다
(AI 서버 동작에는 영향 없음).
