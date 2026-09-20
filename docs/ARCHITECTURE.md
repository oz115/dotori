# Architecture

## 시스템 개요

> 전체 구성요소와 데이터 흐름을 요약합니다. (다이어그램 or 텍스트)

## 인프라 구조

- 배포 환경:
- 컨테이너 구성 (docker-compose 등):
- CI/CD:

## 백엔드 구조

- 프레임워크 / 언어:
- 패키지 구조:
- 모듈 경계:

## 데이터베이스

### 사용 스토리지

- RDB:
- 캐시:

### ERD 요약

> 주요 엔티티와 관계를 요약합니다.

| 엔티티 | 설명 | 주요 관계 |
| --- | --- | --- |
| Family | 부모 한 명이 생성하는 가족 그룹. 생성자만 해체·설정 수정할 수 있으며 `deleted_at`으로 소프트 삭제. 온보딩 설정 3종(`weekly_allowance`·`weekly_reward_limit`·`financial_knowledge_level`)을 "가족 기본값"으로 보관한다 | FamilyMember 1:N |
| FamilyMember | 부모 한 명과 여러 자녀의 가족 소속 이력. 활성 소속은 `deleted_at IS NULL`로 조회 | Family N:1, User N:1 |
| FamilyCode | 가족 초대 코드(6자리, TTL 10분). `code`는 전역 UNIQUE — 코드만으로 가족을 특정하므로 중복이 생기면 연결 API가 죽는다. 재발급 시 기존 코드를 하드 삭제해 가족당 유효 코드 1개 유지 | Family N:1 |
| Notification | 사용자에게 발송된 알림 이력. `ref_id`는 딥링크용 연관 리소스 ID인데 알림 타입마다 가리키는 테이블이 달라 FK를 걸지 않는다 | User N:1 |
| DeviceToken | FCM 푸시 수신용 디바이스 토큰. `fcm_token`은 전역 UNIQUE — 기기가 로그아웃 후 다른 계정으로 로그인하면 등록 API가 기존 행의 `user_id`를 갈아치우는 upsert로 동작한다 | User N:1 |
| InvestmentHolding | 아이별 현재 보유 현황. `(child_id, investment_product_id)` UNIQUE — 매수/매도 API가 거래 기록(`investment_transactions`)과 같은 트랜잭션으로 이 행을 upsert하고, 보유 조회(INV-T-03)는 이 테이블만 읽는다. `buy_price`는 샀을 때 그 시점의 종목 가격(종목당 1주 규칙이라 매수는 한 번뿐), 매도 시 행 삭제 | Family N:1, User(child) N:1, InvestmentProduct N:1 |

- 가족 해체 시 가족과 구성원 소속만 소프트 삭제하며, 포인트 등 거래·자산 이력은 삭제하지 않는다.
- `family_codes`는 소프트 삭제 대상이 아니다(하드 삭제). 다만 해체된 가족의 코드는 정리되지 않고 남는다 — 연결 API가 가족의 `deleted_at`을 확인해 막는다.
- `device_tokens.fcm_token` UNIQUE 덕분에 등록 API가 별도 조회 없이 upsert(있으면 갱신, 없으면 삽입)로 구현된다.

#### 가족 온보딩 설정 (S15P21A505-134)

- **`families`의 금액 컬럼(`weekly_allowance`, `weekly_reward_limit`)은 원(₩)이 아니라 포인트 단위로 저장한다.**
  부모가 원으로 입력하면 서버가 팀 환전 정책(현금 10원 = 포인트 1점, `global/util/PointExchangePolicy`)으로
  환산해 저장하고, 응답으로 나갈 때만 원으로 되돌린다. 이 컬럼을 원으로 읽으면 한도가 10배로 계산되니 주의.
- 금액 입력은 최소 1,000원·1,000원 단위만 허용한다 (검증은 `FamilyServiceImpl.toPoints`).
- **소비처(퀴즈·예적금·투자·집안일)는 `families` 컬럼을 직접 조회하지 않고
  `family/service/FamilySettingsReader`를 주입받아 읽는다** — 반환값은 포인트 단위다.
  자녀별 차등 설정이 생기면 이 구현체만 바뀌고 소비처 코드는 그대로 두기 위한 경계로,
  `childId`를 지금부터 파라미터로 받는다.
- 환율(10원=1포인트)의 원본은 `PointExchangePolicy` 하나다. 원↔포인트 환산(`toPoints`/`toWon`)을
  손으로 `* 10`, `/ 10` 하지 않는다 — 포인트 충전(지갑)과 가족 온보딩이 함께 쓴다.

### 스키마 관리 방식

현재 `ddl-auto: update` 로 Hibernate 가 엔티티에서 스키마를 만든다.
따라서 제약조건도 SQL 스크립트가 아니라 **엔티티에 선언해야** 생성된다.

주의할 점이 두 가지 있다.

- `update` 는 **이미 만들어진 테이블에 제약조건을 뒤늦게 추가하지 않는다.**
  테이블이 생긴 뒤 `@Check` 나 `@UniqueConstraint` 를 더해도 반영되지 않으므로,
  그때는 해당 테이블을 지우고 다시 만들거나 수동으로 `ALTER TABLE` 해야 한다.
- `update` 는 컬럼을 지우거나 타입을 바꾸지 않는다. 스키마가 조용히 어긋날 수 있다.

Flyway 같은 마이그레이션 도구로 옮기면 위 두 문제가 사라지고,
아래의 네이티브 enum 제약도 함께 풀린다.

#### enum 컬럼을 varchar 로 두는 이유

ERD 상 `users.role` 과 `point_transactions.transaction_type`, 그리고 `families.financial_knowledge_level`
(S15P21A505-134, `BEGINNER`/`INTERMEDIATE`/`ADVANCED`) 은 enum 값이지만
모두 `VARCHAR` 로 매핑했다. `ddl-auto: update` 가 네이티브 enum 타입을 제대로 다루지 못해
재기동 시 컬럼이 깨지기 때문이다. 스키마 관리가 스크립트나 Flyway 로 넘어가면 되돌리는 것이 맞다.

#### 포인트 지갑 (S15P21A505-49~53)

엔티티에 선언해 둔 DB 레벨 보호 장치는 다음과 같다.

| 대상 | 제약 | 이유 |
| --- | --- | --- |
| `point_transactions` (`wallet_id`, `idempotency_key`) | UNIQUE | 동일 요청의 중복 충전·지급·차감 방지. 멱등키는 클라이언트가 정하는 값이라 사용자끼리 겹칠 수 있어, 유일성 범위를 지갑 안으로 좁혔다. PostgreSQL 은 NULL 을 서로 다른 값으로 보므로 멱등키 없는 내부 거래는 여러 건 허용된다 |
| `point_wallets.user_id` | UNIQUE | 한 사용자에게 지갑이 두 개 생기면 잔액 조회가 어느 쪽을 골라야 할지 알 수 없다 |
| `point_wallets.balance` | CHECK `>= 0` | 애플리케이션 락이 뚫리거나 수동 SQL 로 잔액이 음수가 되는 것을 차단 |
| `point_transactions.amount` | CHECK `> 0` | 음수 금액 거래가 원장에 남는 것을 차단 |
| `point_transactions` (`wallet_id`, `created_at`, `point_transaction_id`) | INDEX | WLT-06 거래내역 조회(무한 스크롤). 정렬 키와 순서가 같아야 커서 지점부터 요청한 건수만 읽고 멈춘다. 없으면 지갑의 거래를 전부 읽어 정렬한 뒤 잘라낸다. 방향은 ASC 하나만 만든다 — PostgreSQL 은 역방향 스캔도 하므로 최신순 조회도 같은 인덱스를 쓴다 |

원장에는 입출금만 기록하고, 도메인별 포인트 집계는 각 도메인 테이블에서 관리한 뒤
뷰로 합치는 팀 설계 방향을 따랐다.

`point_transactions.transaction_type` 은 입출금 방향이 아니라 거래 사유
(`CHARGE`/`GRANT`/`PURCHASE` 등)를 담는다. RPT-02(수입 유형별 통계)가 사유로 조회할 수 있어야 하고,
방향은 `balance_after` 의 변화로 알 수 있어 따로 저장하지 않는다.
API 명세(WLT-06 거래내역 조회)가 요구하는 `transactionDirection` 이 필요해지면 응답에서 계산한다.

#### 환전 요청 (WLT-07, S15P21A505-115)

자녀가 포인트→현금 환전을 요청하면 그 즉시 포인트를 차감하고, 부모가 승인해야
부모 계좌→자녀 계좌로 실제 송금이 일어난다. 요청과 승인이 서로 다른 트랜잭션이라,
승인 중 송금이 실패하면 이미 커밋된 차감을 되돌릴 방법이 롤백이 아니라 별도의
환급 거래(`REFUND`)뿐이다.

`point_cashout_requests_status` (`backend/docker/initdb/01-schema.sql` 의 네이티브 ENUM)는
`PENDING`/`PROGRESS`/`CANCELLED`/`DECLINED`/`SUCCEED`/`FAILED` 여섯 값을 갖는다. 지금
구현이 실제로 쓰는 전이는 네 가지뿐이다.

- `PENDING → SUCCEED`: 부모 승인, 송금 성공
- `PENDING → FAILED`: 부모 승인 시도, 송금 실패 → 같은 트랜잭션에서 환급까지 끝내고 남김
- `PENDING → CANCELLED`: 자녀가 자기 PENDING 요청을 직접 취소 → 환급
- `PENDING → DECLINED`: 부모가 요청을 거절 → 환급. 승인과 같은 권한 규칙(그 가족의
  어느 부모나)이고, 은행 송금은 일어나지 않는다

`PROGRESS`는 스키마에는 있지만 지금은 쓰지 않는다(팀 결정, 비동기 처리로 갈 때 다시 본다).
거절 사유를 남기는 컬럼은 없다 — 상태만 바뀐다(팀 결정).

| 대상 | 제약 | 이유 |
| --- | --- | --- |
| `point_cashout_requests.child_id`/`parent_id`/`family_id` | FK → `users.user_id`/`families.family_id` | 존재하지 않는 사용자·가족을 향한 요청 방지 |

`status` 컬럼은 네이티브 ENUM이라 `PointCashoutRequestStatus` 상수 이름이 DDL 라벨과
정확히 같아야 한다(`users.role`, `point_transactions.transaction_type` 과 같은 이유).
`application.yaml` 의 `stringtype=unspecified` 덕에 `@Enumerated(EnumType.STRING)` 매핑이
그대로 통한다.

#### 예적금 (SAV, S15P21A505-227)

6개 고정 상품(보통·단리·복리예금, 소액·목표·장기적금)뿐이고 부모가 만들거나 고치지 않는다.
그래서 **카탈로그의 원본은 DB 가 아니라 코드(`savings/entity/SavingsProductType`)** 이고,
`SavingsProductSeeder` 가 앱 기동 시마다 `savings_products` 행을 그 값으로 덮어 동기화한다.
DB 행이 있어야 하는 이유는 `savings_subscriptions.savings_product_id` FK 하나뿐이다.

가족마다 달라지는 값은 "최대 납입/예치 가능 금액(X)" 하나이고, 가족의 `weekly_reward_limit`(RL)
에서 매번 계산한다. 가입 시점의 값을 `savings_subscriptions.principal` 에 스냅샷해서
(거치식=최초원금, 정액 적립식=주당 약정액, 목표적금=목표 금액) 이후 가족 설정이 바뀌어도
이미 가입된 건은 흔들리지 않는다.

**이자는 상품마다 계산 방식이 다르지만 도달점은 같다.** 목표 총이자는 모두
`0.4 × rewardRatio × RL`(`SavingsProductType.targetTotalInterest`)이고, `InterestAccrual` 네 방식이
각자의 경로로 그 값에 도달한다 — 복리예금만 원안 스펙대로 그 4배를 지급하는 예외다.

| 방식 | 상품 | 매주 하는 일 |
| --- | --- | --- |
| `WEEKLY_PAYOUT` | 보통예금 | 원금 잔액 × 이자율을 계산해 그 자리에서 지갑으로 지급 |
| `WEEKLY_STRAIGHT` | 단리예금·소액적금·장기적금 | 원금 잔액 × 이자율 ÷ 전체 주수를 쌓아 둔다 (적립식에서는 이 식이 곧 평잔 × 이자율이 된다) |
| `WEEKLY_COMPOUND` | 복리예금 | (원금 + 쌓인 이자) × 이자율을 쌓아 둔다 |
| `ON_GOAL_ACHIEVEMENT` | 목표적금 | 아무것도 하지 않는다. 목표를 채운 순간 목표 금액 × 이자율을 한 번에 얹는다 |

`savings_subscriptions` 는 **원금(`current_amount`)과 이자(`accrued_interest`)를 나눠 들고 있다.**
섞으면 복리는 맞지만 단리·적립식은 틀린 이자가 나온다. 나눠 두면 복리만 두 값을 더한 것을
기준으로 삼으면 되고, 만기 지급액은 모든 상품이 똑같이 원금 + 이자다.

상태는 `ACTIVE → MATURED → COMPLETED` 가 정상 경로이고, `ACTIVE → CANCELLED` 가 중도 해지다.
중도 해지는 납입 원금만 돌려주고 쌓인 이자는 지급하지 않는다(그 사실 자체가 학습 내용이다).
만기 지급은 자동이 아니라 아이가 수령을 눌러야 일어난다 — `MATURED` 가 그 대기 상태다.
목표적금만 예외로 달성 즉시 `ACTIVE → COMPLETED` 로 지급까지 끝낸다.

**정산일은 가입일 기준이다**(가입 시각 + n주). 모두를 월요일에 맞추면 일요일에 가입한 아이가
하루 만에 한 주치 이자를 받는다. 대신 정산 크론(`SavingsSettlementScheduler`)은 매일 00:20 에
돌면서 `next_settlement_at` 이 지난 건만 처리하고, 밀린 주가 여러 개면 한 번에 따라잡는다.
건마다 트랜잭션이 갈라져 있어 한 건이 실패해도 나머지는 커밋된다.

정액 적립식은 **한 주에 한 번만** 납입할 수 있다. 회차를 몰아 넣으면 이자 기준이 되는 잔액이
그만큼 일찍 커져서 약속한 것보다 많은 이자가 나가기 때문이다.

| 대상 | 제약 | 이유 |
| --- | --- | --- |
| `savings_subscriptions.idempotency_key` | UNIQUE (`child_id`, `idempotency_key`) | 재시도로 가입이 두 건 생기는 것을 막는다. 지갑의 멱등키는 이중 차감만 막고 가입 행은 막지 못한다 |
| `savings_subscriptions` 금액 컬럼 | CHECK (원금 > 0, 잔액·이자·회차 >= 0) | 음수 잔액·이자가 만기 지급액을 뒤틀지 않게 |
| `savings_transactions.amount` | CHECK (> 0) | 방향은 `transaction_type` 이 정한다. 금액에 음수를 허용하면 같은 사실을 두 곳에서 다르게 말하게 된다 |

`savings_transactions` 는 지갑의 `point_transactions` 와 역할이 다르다. 저쪽은 "아이 지갑의
잔액이 어떻게 변했는가"의 원장이고, 이쪽은 "이 가입 건 안에서 무슨 일이 있었는가"의 이력이다.
이자 적립처럼 지갑이 전혀 움직이지 않는 사건도 남아야 만기 지급액을 설명할 수 있다.

#### 이미 만들어진 DB 에 적용해야 하는 변경

`ddl-auto: update` 는 기존 테이블의 제약을 바꾸지 않는다.
그래서 `backend/src/test/resources/schema.sql` 아래쪽이 위 제약들을
`DROP CONSTRAINT IF EXISTS` → `ADD CONSTRAINT` 로 매번 다시 걸어 준다.
테스트가 앱과 같은 DB 를 쓰므로, **테스트를 한 번 돌리면 그 DB 에는 자동으로 반영된다.**

테스트가 닿지 않는 DB(배포 환경 등)에는 손으로 한 번 실행한다.
`init.sql` 최신화 때 같이 반영한다.

```sql
-- 멱등키 유일성을 (지갑, 키) 복합으로 좁힌다.
ALTER TABLE point_transactions
	DROP CONSTRAINT IF EXISTS uk_point_transactions_idempotency_key;

ALTER TABLE point_transactions
	ADD CONSTRAINT uk_point_transactions_wallet_idempotency_key
	UNIQUE (wallet_id, idempotency_key);

-- WLT-06 거래내역 조회(커서 페이징)용 인덱스.
CREATE INDEX IF NOT EXISTS ix_point_transactions_wallet_created_at
	ON point_transactions (wallet_id, created_at, point_transaction_id);
```

가족 온보딩 설정(S15P21A505-134)의 새 컬럼도 같은 방식으로 반영한다.
테이블을 DROP 후 재생성하는 일괄 최신화 전까지는 아래 ALTER 로 맞추고
(과도기에는 기존 행 때문에 NULL 허용), 재생성 시 최종 DDL 에서 NOT NULL 로 조인다.

```sql
ALTER TABLE families ADD COLUMN IF NOT EXISTS weekly_allowance          INTEGER     NULL;
ALTER TABLE families ADD COLUMN IF NOT EXISTS weekly_reward_limit       INTEGER     NULL;
ALTER TABLE families ADD COLUMN IF NOT EXISTS financial_knowledge_level VARCHAR(20) NULL;
```

이미 서로 다른 지갑에 같은 멱등키가 들어가 있으면 두 번째 문장이 실패한다.
그런 행이 있는지 먼저 확인한다.

```sql
SELECT idempotency_key, count(DISTINCT wallet_id)
FROM point_transactions
WHERE idempotency_key IS NOT NULL
GROUP BY idempotency_key
HAVING count(DISTINCT wallet_id) > 1;
```

#### 회원 아이디·이메일 유일성 — 부분 유니크 인덱스 (S15P21A505-172)

회원 탈퇴(MYPAGE-05)는 거래·가족 이력 보존 때문에 행을 지우지 않는 소프트 삭제라,
`users.login_id`·`users.email` 을 전체 UNIQUE 로 걸면 **탈퇴자가 쓰던 이메일·아이디로
다시 가입할 수 없다.** 그래서 유일성 범위를 살아있는 행으로 좁혔다.

- DB: 전체 UNIQUE 제약(`UQ_USERS_*`) 대신 **부분 유니크 인덱스**
  `UX_USERS_LOGIN_ID_ACTIVE` / `UX_USERS_EMAIL_ACTIVE` (`WHERE deleted_at IS NULL`).
- 코드: 아이디·이메일 조회는 전부 `deleted_at IS NULL` 을 거른다
  (`UserRepository` 의 `*AndDeletedAtIsNull`, `findByNameAndEmail`).
  거르지 않으면 재가입 후 같은 아이디의 행이 2개라 단건 조회가 깨진다.
- 동시 가입 경합은 그대로 인덱스 위반(23505)으로 잡혀
  `UserServiceImpl.resolveDuplicateError` 가 409 로 바꾼다.

이미 만들어진 DB(공용 EC2 등)에는 아래를 수동 실행한다.

```sql
ALTER TABLE users DROP CONSTRAINT IF EXISTS "UQ_USERS_LOGIN_ID";
ALTER TABLE users DROP CONSTRAINT IF EXISTS "UQ_USERS_EMAIL";

CREATE UNIQUE INDEX IF NOT EXISTS "UX_USERS_LOGIN_ID_ACTIVE"
	ON users (login_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS "UX_USERS_EMAIL_ACTIVE"
	ON users (email) WHERE deleted_at IS NULL;
```

## API 설계 원칙

-

## 인증 / 인가

-

## 외부 연동

- **AWS S3** (`ap-northeast-2`, 버킷 `s15p21a505dotori`) — 집안일 인증 사진 (CHORE-13).
  - 버킷은 **비공개**다. 앱만 IAM 액세스 키로 접근하고, 화면은 서버가 발급한 presigned URL 로 본다
    (기본 10분). 앱에 키를 넣으면 그 키는 공개된 키가 되므로 클라이언트 직접 업로드는 하지 않는다.
  - DB 에는 URL 이 아니라 **키**를 저장한다: `chores/{familyId}/{childId}/{uuid}.{jpg|png}`.
    presigned URL 은 500자가 넘고 몇 분 뒤 만료돼 저장할 값이 못 된다.
    경로에 가족·아이를 넣은 덕에 클라이언트가 보낸 키가 자기 것인지 검증할 수 있다.
  - 코드는 `global/storage/PhotoStorage` 인터페이스로 감쌌다. 테스트에서는
    `mock/storage/MockPhotoStorage`(`test` 프로파일)가 받으므로 버킷도 자격증명도 필요 없다.
  - 업로드만 하고 완료 요청을 안 보내면 사진이 고아로 남는다. 정리는 버킷 라이프사이클 규칙 몫이다
    (아직 안 걸었음).

### 아이 투자 표시·복습 (S15P21A505-284)

- 시세 원장은 변동 종목만 저장한다. 차트 조회는 KST 날짜별 마지막 가격을 구성하며,
  기록 없는 날은 직전 가격을 이어 쓰고 최초 기록 이전에는 상품 시작가를 쓴다.
  조회 시작일 이전의 마지막 기록도 확인한다. 가족·상품 생성 이전과 미래 날짜는 제외한다.
  차트 응답의 recordedAt은 실제 갱신 시각이 아닌 **가격 기준일(KST 00:00)** 이다.
- 상품의 기존 created_at, news_context 컬럼을 조회용으로 매핑한다. DB 스키마 변경은 없다.
- 매도 결과에는 buyPrice와 realizedProfit을 함께 반환한다. 보유 행을 잠근 뒤 원가를 읽고
  삭제·포인트 지급·거래 기록을 같은 트랜잭션에서 수행한다.
- 거래 조회는 종목 ID별 매수·매도를 시간·ID순으로 연결해 확정 손익을 계산한다.
  기간·유형 필터 이전의 매수도 포함한다. 매수 기록이 없으면 손익은 null이고,
  화면은 이를 0으로 확정하거나 불완전한 누적 손익을 표시하지 않는다.
- 뉴스 복습은 해당 가족의 오늘 가격 반영 기록이 확인된 어제 뉴스만 회사·가격·해설을 공개한다.
  미반영 항목은 대기 상태이며 정답 필드는 null이다. 해설은 현재 10개 회사의 경제 요인에
  맞춘 규칙 기반 문구로 제공하고, AI 뉴스 생성이나 시세 산식은 변경하지 않는다.
- 오늘 뉴스 화면은 회사명·업종·회사별 그림·등락 정답을 표시하지 않는다.
  투자 등락은 상승 파랑·하락 빨강·보합 중립색과 부호/문구를 함께 사용한다.

- 공용 서버 구버전 응답에는 손익 필드가 없으므로 프론트는 **전체 거래 내역**에서
  회사별 매수·매도를 연결해 원가를 복원한다. 새 서버의 손익 0과 명시적 null은 그대로 유지한다.
  현재 고정 카탈로그는 회사명이 중복되지 않으며, 구버전 거래 응답에 종목 ID가 없어 이름을 사용한다.
  페이지 분할·회사명 중복을 도입할 때는 종목 ID와 서버 계산으로 전환해야 한다.
- 메인에는 보유 수익(현재 보유 주식의 가치 - 구매금액), 거래 내역에는 판매 수익을 표시한다.
  뉴스는 제목 목록에서 별도 상세 페이지로 이동한다. 지난 뉴스 해설은 별도 페이지이며
  구버전 서버의 미지원 응답은 재시도 가능한 연결 오류와 구분한다.
- 구버전 시세 응답도 최초 실제 기록부터 오늘까지 직전 가격으로 빈 날짜를 채운다.
  최초 기록 이전 값은 추정하지 않는다. 새 서버의 생성일 기준 보합 데이터도 같은 방식으로 처리한다.
