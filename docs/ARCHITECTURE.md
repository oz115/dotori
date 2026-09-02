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
| `point_transactions` (`wallet_id`, `idempotency_key`) | UNIQUE | 동일 요청의 중복 충전·지급·차감 방지. 멱등키는 클라이언트가 정하는 값이라 사용자끼리 겹칠 수 있어, 유일성 범위를 지갑 안으로 좁혔다. PostgreSQL 은 NULL 을 서로 다른 값으로 보므로 멱등키 없는 내부 거래는 여러 건 허용된다 |
| `point_wallets.user_id` | UNIQUE | 한 사용자에게 지갑이 두 개 생기면 잔액 조회가 어느 쪽을 골라야 할지 알 수 없다 |
| `point_wallets.balance` | CHECK `>= 0` | 애플리케이션 락이 뚫리거나 수동 SQL 로 잔액이 음수가 되는 것을 차단 |
| `point_transactions.amount` | CHECK `> 0` | 음수 금액 거래가 원장에 남는 것을 차단 |

원장에는 입출금만 기록하고, 도메인별 포인트 집계는 각 도메인 테이블에서 관리한 뒤
뷰로 합치는 팀 설계 방향을 따랐다.

`point_transactions.transaction_type` 은 입출금 방향이 아니라 거래 사유
(`CHARGE`/`GRANT`/`PURCHASE` 등)를 담는다. RPT-02(수입 유형별 통계)가 사유로 조회할 수 있어야 하고,
방향은 `balance_after` 의 변화로 알 수 있어 따로 저장하지 않는다.
API 명세(WLT-06 거래내역 조회)가 요구하는 `transactionDirection` 이 필요해지면 응답에서 계산한다.

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

## API 설계 원칙

-

## 인증 / 인가

-

## 외부 연동

-
