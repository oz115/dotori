# AI 서버의 DB 접근 범위

**이 서버는 테이블을 하나도 소유하지 않는다.** 팀원들이 이미 만들어 둔 테이블을 활용하는 것이
이 서버의 역할이다. 스키마 주인은 백엔드(Spring, `ddl-auto: update`)다.

필요한 컬럼이 없으면 직접 추가하지 않는다. 컬럼은 백엔드 엔티티에 선언되어야 Hibernate 가
만들기 때문에 애초에 혼자 처리할 수 없다. 아래 "협의가 필요한 항목" 절차를 따른다.

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
| `families`, `users` | **읽기만** | 온보딩 A·B 값과 아이 정보 조회 |
| 그 밖의 모든 테이블 | 접근 안 함 | 포인트 지갑, 예적금, 계좌 연동 등은 백엔드 담당 |

`store_products.value`(NUMERIC)는 가중치, `price`(INTEGER)는 포인트다.
둘 다 nullable 이라 AI 가 채우기 전에는 NULL 이다.

## 협의가 필요한 항목

절차는 다음과 같다.

1. 어느 도메인 담당자의 테이블인지 확인한다
2. 컬럼 이름·타입·의미를 정리해서 담당자에게 요청한다
3. 합의되면 담당자가 백엔드 엔티티에 선언하고, DB 에 반영된 뒤에 AI 서버가 그 컬럼을 쓴다
4. 합의 결과를 이 문서에 기록한다

### 1. 온보딩 A·B 값을 저장할 컬럼이 없다 (블로커)

기획에는 "family 나 user 테이블에 A, B 값이 저장됨"으로 되어 있지만,
`families` 는 컬럼이 6개(`family_id`, `name`, `created_by`, `created_at`, `updated_at`,
`deleted_at`)뿐이고 `users` 에도 없다.
**A·B 없이는 하루 소득 `D = (A + B) / 7` 을 구할 수 없어 가격 책정 자체가 불가능하다.**

요청안 (`families`):

| 컬럼 | 타입 | 의미 |
| --- | --- | --- |
| `weekly_max_earning` | INTEGER | A — 포인트 시스템 안에서 예적금, 투자, 퀴즈로 주당 벌 수 있는 최대 포인트 (할 일 제외)|
| `weekly_allowance` | INTEGER | B — 주간 기본 용돈(포인트 환산) |

담당: 온보딩 · 가족 도메인 / 상태: **요청함 (2026-09-08)** — 반영 확인 후 컬럼 이름을 이 표와 맞춘다

그 전까지는 A·B 를 요청 파라미터로 받아 계산 로직을 완성해 둔다.

### 2. `investment_news_products` 에 영향 방향이 없다

컬럼이 `investment_news_product_id`, `investment_news_id`, `investment_product_id` 3개뿐이다.
그런데 투자 설계(v3)의 핵심은 **하나의 뉴스가 회사마다 서로 다른 영향을 준다**는 것이다.
지금 구조로는 "이 뉴스가 이 회사에 호재인지 악재인지"를 저장할 수 없다.

요청안 (`investment_news_products`):

| 컬럼 | 타입 | 의미 |
| --- | --- | --- |
| `impact` | ENUM/VARCHAR | `POSITIVE` / `NEGATIVE` / `NEUTRAL` |
| `evidence_grade` | VARCHAR | 설계 v3 의 근거 등급 `A` / `B` |

담당: 투자 도메인 / 상태: **요청 전**

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
