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
|  |  |  |

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
| `point_transactions.idempotency_key` | UNIQUE | 동일 요청의 중복 충전·지급·차감 방지. PostgreSQL 은 NULL 을 서로 다른 값으로 보므로 멱등키 없는 내부 거래는 여러 건 허용된다 |
| `point_wallets.user_id` | UNIQUE | 한 사용자에게 지갑이 두 개 생기면 잔액 조회가 어느 쪽을 골라야 할지 알 수 없다 |
| `point_wallets.balance` | CHECK `>= 0` | 애플리케이션 락이 뚫리거나 수동 SQL 로 잔액이 음수가 되는 것을 차단 |
| `point_transactions.amount` | CHECK `> 0` | 음수 금액 거래가 원장에 남는 것을 차단 |

원장에는 입출금만 기록하고, 도메인별 포인트 집계는 각 도메인 테이블에서 관리한 뒤
뷰로 합치는 팀 설계 방향을 따랐다.

**정리 필요**: `point_transactions.transaction_direction` 은 컬럼명과 달리 거래 사유
(`CHARGE`/`GRANT`/`PURCHASE` 등)를 담고 있다. `transaction_type` 이 이미 입출금 방향을
갖고 있어 중복이었고, RPT-02(수입 유형별 통계)가 사유로 조회할 수 있어야 하기 때문이다.
다만 API 명세(WLT-06 거래내역 조회)는 응답에 `transactionType: "CHARGE"` 와
`transactionDirection: "IN"` 을 함께 요구하고 있어, 컬럼 의미가 명세와 반대다.
데이터가 쌓이기 전에 컬럼명을 정리하는 편이 좋다.

## API 설계 원칙

-

## 인증 / 인가

-

## 외부 연동

-
