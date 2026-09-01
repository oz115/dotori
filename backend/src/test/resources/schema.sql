/* =========================================================
   테스트용 스키마 (포인트 지갑 범위)

   팀 전체 DDL 중 지갑 기능이 실제로 건드리는 테이블만 옮겨 왔다.
   전체 DDL 을 그대로 쓰지 않는 이유는 investment_products.category 등
   일부 컬럼이 PostgreSQL 에 없는 ENUM 타입으로 선언돼 있어 실행되지 않기 때문이다.

   운영 스키마와 어긋나면 테스트가 의미를 잃으므로,
   지갑 관련 테이블이 바뀌면 이 파일도 반드시 함께 고친다.
   ========================================================= */

DROP TABLE IF EXISTS
	"point_transactions",
	"point_wallets",
	"users"
CASCADE;

DROP TYPE IF EXISTS
	point_transactions_transaction_type,
	users_role
CASCADE;


CREATE TYPE users_role AS ENUM ('PARENT', 'CHILD');

CREATE TYPE point_transactions_transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL');


CREATE TABLE "users" (
	"user_id"	SERIAL	NOT NULL,
	"login_id"	VARCHAR(50)	NOT NULL,
	"password"	VARCHAR(255)	NOT NULL,
	"name"	VARCHAR(50)	NOT NULL,
	"email"	VARCHAR(255)	NOT NULL,
	"role"	users_role	NOT NULL,
	"email_verified"	BOOLEAN	DEFAULT FALSE	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"deleted_at"	TIMESTAMPTZ	NULL,
	CONSTRAINT "PK_USERS" PRIMARY KEY ("user_id")
);


CREATE TABLE "point_wallets" (
	"wallet_id"	SERIAL	NOT NULL,
	"user_id"	INTEGER	NOT NULL,
	"balance"	INTEGER	DEFAULT 0	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_POINT_WALLETS" PRIMARY KEY ("wallet_id"),
	CONSTRAINT "FK_POINT_WALLETS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id"),
	CONSTRAINT "UX_POINT_WALLETS_USER" UNIQUE ("user_id"),
	CONSTRAINT "CK_POINT_WALLETS_BALANCE_NOT_NEGATIVE" CHECK ("balance" >= 0)
);


CREATE TABLE "point_transactions" (
	"point_transaction_id"	SERIAL	NOT NULL,
	"wallet_id"	INTEGER	NOT NULL,
	"transaction_type"	point_transactions_transaction_type	NOT NULL,
	"transaction_direction"	VARCHAR(10)	NOT NULL,
	"amount"	INTEGER	NOT NULL,
	"balance_after"	INTEGER	NOT NULL,
	"description"	VARCHAR(255)	NULL,
	"idempotency_key"	VARCHAR(100)	NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_POINT_TRANSACTIONS" PRIMARY KEY ("point_transaction_id"),
	CONSTRAINT "FK_POINT_TRANSACTIONS_WALLET" FOREIGN KEY ("wallet_id")
		REFERENCES "point_wallets" ("wallet_id"),
	CONSTRAINT "CK_POINT_TRANSACTIONS_AMOUNT_POSITIVE" CHECK ("amount" > 0)
);

CREATE UNIQUE INDEX "UX_POINT_TRANSACTIONS_IDEMPOTENCY_KEY"
	ON "point_transactions" ("idempotency_key")
	WHERE "idempotency_key" IS NOT NULL;
