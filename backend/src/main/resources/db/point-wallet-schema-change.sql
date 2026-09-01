/* =========================================================
   포인트 지갑 스키마 변경 (S15P21A505-49)

   기존 ERD를 유지하면서 아래 두 가지만 추가한다.
   운영/개발 DB에 수동으로 1회 적용한다.
   ========================================================= */


/* ---------------------------------------------------------
   1. 멱등성 키

   동일 요청의 중복 충전·지급·차감을 막기 위한 컬럼이다.
   부분 유니크 인덱스를 쓰는 이유:
   배치 지급처럼 멱등키가 없는 거래(NULL)는 여러 건 허용해야 하는데,
   일반 UNIQUE 제약은 NULL을 서로 다른 값으로 보므로 동작하지만
   의도를 명확히 하기 위해 WHERE 절을 명시한다.
   --------------------------------------------------------- */

ALTER TABLE "point_transactions"
	ADD COLUMN IF NOT EXISTS "idempotency_key" VARCHAR(100) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "UX_POINT_TRANSACTIONS_IDEMPOTENCY_KEY"
	ON "point_transactions" ("idempotency_key")
	WHERE "idempotency_key" IS NOT NULL;


/* ---------------------------------------------------------
   2. 지갑 1인 1개 보장

   point_wallets 에는 user_id UNIQUE 제약이 없어
   동시 요청 시 한 사용자에게 지갑이 2개 생길 수 있다.
   findByUserId 가 유일한 결과를 보장하려면 필요하다.
   --------------------------------------------------------- */

ALTER TABLE "point_wallets"
	ADD CONSTRAINT "UX_POINT_WALLETS_USER" UNIQUE ("user_id");


/* ---------------------------------------------------------
   3. 잔액 음수 방지 (최종 방어선)

   애플리케이션에서 비관적 락으로 막고 있지만,
   배치나 수동 SQL 로 잔액이 음수가 되는 것을 DB 레벨에서도 차단한다.
   --------------------------------------------------------- */

ALTER TABLE "point_wallets"
	ADD CONSTRAINT "CK_POINT_WALLETS_BALANCE_NOT_NEGATIVE" CHECK ("balance" >= 0);

ALTER TABLE "point_transactions"
	ADD CONSTRAINT "CK_POINT_TRANSACTIONS_AMOUNT_POSITIVE" CHECK ("amount" > 0);
