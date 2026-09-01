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

### 스키마 변경 이력

#### 포인트 지갑 (S15P21A505-49)

적용 스크립트: [backend/src/main/resources/db/point-wallet-schema-change.sql](../backend/src/main/resources/db/point-wallet-schema-change.sql)

| 대상 | 변경 | 이유 |
| --- | --- | --- |
| `point_transactions` | `idempotency_key VARCHAR(100)` 컬럼 + 부분 유니크 인덱스 추가 | 동일 요청의 중복 충전·지급·차감 방지. NULL 은 인덱스 대상에서 제외해 멱등키 없는 내부 거래를 허용 |
| `point_wallets` | `user_id` UNIQUE 제약 추가 | 제약이 없어 동시 요청 시 한 사용자에게 지갑이 2개 생길 수 있었음 |
| `point_wallets` | `balance >= 0` CHECK 추가 | 애플리케이션 락이 뚫리거나 수동 SQL 로 잔액이 음수가 되는 것을 DB 에서도 차단 |
| `point_transactions` | `amount > 0` CHECK 추가 | 음수 금액 거래가 원장에 남는 것을 차단 |

컬럼을 늘리는 대신 기존 ERD 를 최대한 유지했다. 원장에는 입출금만 기록하고,
도메인별 포인트 집계는 각 도메인 테이블에서 관리한 뒤 뷰로 합치는 팀 설계 방향을 따른 것이다.

**정리 필요**: `point_transactions.transaction_direction` 은 컬럼명과 달리 거래 사유
(`CHARGE`/`GRANT`/`PURCHASE` 등)를 담고 있다. `transaction_type` 이 이미 입출금 방향을
갖고 있어 중복이었고, RPT-02(수입 유형별 통계)가 사유로 조회할 수 있어야 하기 때문이다.
데이터가 쌓이기 전에 `reason` 으로 renaming 하는 것이 좋다.

## API 설계 원칙

-

## 인증 / 인가

-

## 외부 연동

-
