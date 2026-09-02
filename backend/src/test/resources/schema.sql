/* =========================================================
   테스트용 스키마 (포인트 지갑 범위)

   지갑 기능이 실제로 건드리는 테이블만 담았다.

   컬럼 타입은 운영에서 Hibernate(ddl-auto:update)가 만들어 내는 것과 맞춰야 한다.
   특히 enum 컬럼은 PG 네이티브 ENUM 이 아니라 VARCHAR 다.
   ddl-auto:update 가 네이티브 enum 타입을 제대로 다루지 못해
   users.role 과 point_transactions.transaction_type 모두 varchar 로 두기로 했다.

   스키마 관리가 스크립트나 Flyway 로 넘어가면 이 파일 대신 그 스크립트를 쓰고,
   네이티브 enum 과 함께 되돌리는 것이 맞다.
   ========================================================= */

DROP TABLE IF EXISTS
	"point_transactions",
	"point_wallets",
	"family_members",
	"families",
	"users"
CASCADE;


CREATE TABLE "users" (
	"user_id"	SERIAL	NOT NULL,
	"login_id"	VARCHAR(50)	NOT NULL,
	"password"	VARCHAR(255)	NOT NULL,
	"name"	VARCHAR(50)	NOT NULL,
	"email"	VARCHAR(255)	NOT NULL,
	"role"	VARCHAR(20)	NOT NULL,
	"email_verified"	BOOLEAN	DEFAULT FALSE	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"deleted_at"	TIMESTAMPTZ	NULL,
	CONSTRAINT "PK_USERS" PRIMARY KEY ("user_id"),
	CONSTRAINT "uk_users_login_id" UNIQUE ("login_id"),
	CONSTRAINT "uk_users_email" UNIQUE ("email")
);


CREATE TABLE "families" (
	"family_id"	SERIAL	NOT NULL,
	"name"	VARCHAR(100)	NOT NULL,
	"created_by"	INTEGER	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_FAMILIES" PRIMARY KEY ("family_id"),
	CONSTRAINT "FK_FAMILIES_CREATED_BY" FOREIGN KEY ("created_by") REFERENCES "users" ("user_id")
);


CREATE TABLE "family_members" (
	"family_member_id"	SERIAL	NOT NULL,
	"family_id"	INTEGER	NOT NULL,
	"user_id"	INTEGER	NOT NULL,
	"joined_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_FAMILY_MEMBERS" PRIMARY KEY ("family_member_id"),
	CONSTRAINT "FK_FAMILY_MEMBERS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id"),
	CONSTRAINT "FK_FAMILY_MEMBERS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id")
);


CREATE TABLE "point_wallets" (
	"wallet_id"	SERIAL	NOT NULL,
	"user_id"	INTEGER	NOT NULL,
	"balance"	INTEGER	DEFAULT 0	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_POINT_WALLETS" PRIMARY KEY ("wallet_id"),
	CONSTRAINT "FK_POINT_WALLETS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id"),
	CONSTRAINT "uk_point_wallets_user_id" UNIQUE ("user_id"),
	CONSTRAINT "ck_point_wallets_balance_not_negative" CHECK ("balance" >= 0)
);


CREATE TABLE "point_transactions" (
	"point_transaction_id"	SERIAL	NOT NULL,
	"wallet_id"	INTEGER	NOT NULL,
	"transaction_type"	VARCHAR(20)	NOT NULL,
	"amount"	INTEGER	NOT NULL,
	"balance_after"	INTEGER	NOT NULL,
	"description"	VARCHAR(255)	NULL,
	"idempotency_key"	VARCHAR(100)	NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_POINT_TRANSACTIONS" PRIMARY KEY ("point_transaction_id"),
	CONSTRAINT "FK_POINT_TRANSACTIONS_WALLET" FOREIGN KEY ("wallet_id")
		REFERENCES "point_wallets" ("wallet_id"),
	CONSTRAINT "uk_point_transactions_wallet_idempotency_key" UNIQUE ("wallet_id", "idempotency_key"),
	CONSTRAINT "ck_point_transactions_amount_positive" CHECK ("amount" > 0)
);
