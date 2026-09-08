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
| Family | 부모 한 명이 생성하는 가족 그룹. 생성자만 해체할 수 있으며 `deleted_at`으로 소프트 삭제 | FamilyMember 1:N |
| FamilyMember | 부모 한 명과 여러 자녀의 가족 소속 이력. 활성 소속은 `deleted_at IS NULL`로 조회 | Family N:1, User N:1 |
| FamilyCode | 가족 초대 코드(6자리, TTL 10분). `code`는 전역 UNIQUE — 코드만으로 가족을 특정하므로 중복이 생기면 연결 API가 죽는다. 재발급 시 기존 코드를 하드 삭제해 가족당 유효 코드 1개 유지 | Family N:1 |

- 가족 해체 시 가족과 구성원 소속만 소프트 삭제하며, 포인트 등 거래·자산 이력은 삭제하지 않는다.
- `family_codes`는 소프트 삭제 대상이 아니다(하드 삭제). 다만 해체된 가족의 코드는 정리되지 않고 남는다 — 연결 API가 가족의 `deleted_at`을 확인해 막는다.

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

ERD 상 `users.role` 과 `point_transactions.transaction_type` 은 PG 네이티브 ENUM 이지만
둘 다 `VARCHAR` 로 매핑했다. `ddl-auto: update` 가 네이티브 enum 타입을 제대로 다루지 못해
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

#### 스키마를 어디서 관리하나

원본은 **`backend/docker/initdb/01-schema.sql` 하나**다 (노션 DDL과 동일 내용).
`ddl-auto: none` 이므로 Hibernate 는 스키마를 만들지도 고치지도 않는다.
엔티티를 고쳤으면 이 파일도 같은 커밋에서 고친다.

DB 는 역할로 두 개를 나눠 쓴다.

- **로컬 도커 DB** — 테스트 전용. `docker compose -f docker-compose.local.yml up -d` 하면
  컨테이너가 처음 뜰 때 위 스크립트로 스키마가 만들어진다. 스키마를 고친 뒤에는
  `down -v` 로 볼륨을 지우고 다시 띄워야 반영된다.
- **공용 EC2 DB** — QA 전용. 자동 반영이 없다. 인프라 담당자와 협의해 수동으로 실행한다
  (스크립트 맨 위 `DROP` 이 데이터를 전부 날린다).

예전에는 `ddl-auto: update` 가 스키마를 만들고, 그게 못 붙이는 제약을
`backend/src/test/resources/schema.sql` 이 테스트마다 덧발랐다. 그 구조는 없앴다.
DDL 로 만든 스키마 위에 Hibernate 가 엔티티 모양대로 컬럼을 덧붙여
`point_wallets` 에 `child_id`(DDL)와 `user_id`(엔티티)가 함께 생기는 사고가 났고,
테스트용 파일이 지갑 범위 6개 테이블만 담은 채 enum 컬럼을 `VARCHAR` 로 선언해
실제 스키마와 어긋나 있었다.

## API 설계 원칙

-

## 인증 / 인가

-

## 외부 연동

-
