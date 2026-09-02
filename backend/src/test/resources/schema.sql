/* =========================================================
   테스트용 스키마 (포인트 지갑 범위)

   지갑 기능이 실제로 건드리는 테이블만 담았다.

   테스트는 별도 DB 를 만들지 않고 애플리케이션과 같은 DB 를 쓴다
   (application-test.yaml 이 datasource 를 물려받는다).
   그래서 이 스크립트는 절대 테이블을 지우지 않는다.
   - CREATE TABLE IF NOT EXISTS: 이미 있으면 그대로 둔다
   - 맨 아래 제약조건 보정: ddl-auto:update 가 기존 테이블에 붙여주지 못하는
     UNIQUE·CHECK 를 매 실행마다 엔티티 선언과 맞춰 준다

   경고: WalletTestFixture.cleanUp() 은 users·families 까지 DELETE 한다.
   즉 테스트를 돌리면 이 DB 의 회원·가족 데이터가 비워진다.
   보존해야 할 데이터가 있는 DB 를 가리키게 두면 안 된다.

   컬럼 타입은 운영에서 Hibernate(ddl-auto:update)가 만들어 내는 것과 맞춰야 한다.
   특히 enum 컬럼은 PG 네이티브 ENUM 이 아니라 VARCHAR 다.
   ddl-auto:update 가 네이티브 enum 타입을 제대로 다루지 못해
   users.role 과 point_transactions.transaction_type 모두 varchar 로 두기로 했다.

   스키마 관리가 스크립트나 Flyway 로 넘어가면 이 파일 대신 그 스크립트를 쓴다.
   ========================================================= */

CREATE TABLE IF NOT EXISTS "users" (
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


CREATE TABLE IF NOT EXISTS "families" (
	"family_id"	SERIAL	NOT NULL,
	"name"	VARCHAR(100)	NOT NULL,
	"created_by"	INTEGER	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_FAMILIES" PRIMARY KEY ("family_id"),
	CONSTRAINT "FK_FAMILIES_CREATED_BY" FOREIGN KEY ("created_by") REFERENCES "users" ("user_id")
);


CREATE TABLE IF NOT EXISTS "family_members" (
	"family_member_id"	SERIAL	NOT NULL,
	"family_id"	INTEGER	NOT NULL,
	"user_id"	INTEGER	NOT NULL,
	"joined_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_FAMILY_MEMBERS" PRIMARY KEY ("family_member_id"),
	CONSTRAINT "FK_FAMILY_MEMBERS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id"),
	CONSTRAINT "FK_FAMILY_MEMBERS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id")
);


CREATE TABLE IF NOT EXISTS "point_wallets" (
	"wallet_id"	SERIAL	NOT NULL,
	"user_id"	INTEGER	NOT NULL,
	"balance"	INTEGER	DEFAULT 0	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_POINT_WALLETS" PRIMARY KEY ("wallet_id"),
	CONSTRAINT "FK_POINT_WALLETS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id")
);


CREATE TABLE IF NOT EXISTS "point_transactions" (
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
		REFERENCES "point_wallets" ("wallet_id")
);


/* ---------------------------------------------------------
   제약조건 보정

   ddl-auto:update 는 이미 만들어진 테이블에 UNIQUE·CHECK 를 뒤늦게 붙이지 않는다.
   그래서 테이블이 언제 만들어졌든 엔티티 선언과 같은 상태가 되도록
   DROP IF EXISTS → ADD 순서로 매번 다시 걸어 준다.

   PostgreSQL 에는 ADD CONSTRAINT IF NOT EXISTS 가 없어서 이렇게 쓴다.
   DO 블록을 쓰지 않는 이유는 Spring 의 스크립트 실행기가 세미콜론으로
   문장을 쪼개기 때문에 블록 안의 세미콜론에서 깨지기 때문이다.

   기존 데이터가 제약을 위반하면 여기서 시끄럽게 실패한다. 의도한 동작이다.
   --------------------------------------------------------- */

ALTER TABLE "point_wallets"
	DROP CONSTRAINT IF EXISTS "uk_point_wallets_user_id";
ALTER TABLE "point_wallets"
	ADD CONSTRAINT "uk_point_wallets_user_id" UNIQUE ("user_id");

ALTER TABLE "point_wallets"
	DROP CONSTRAINT IF EXISTS "ck_point_wallets_balance_not_negative";
ALTER TABLE "point_wallets"
	ADD CONSTRAINT "ck_point_wallets_balance_not_negative" CHECK ("balance" >= 0);

ALTER TABLE "point_transactions"
	DROP CONSTRAINT IF EXISTS "ck_point_transactions_amount_positive";
ALTER TABLE "point_transactions"
	ADD CONSTRAINT "ck_point_transactions_amount_positive" CHECK ("amount" > 0);

/* 멱등키는 전역이 아니라 (지갑, 키) 단위로 유일해야 한다.
   전역 유일이던 구버전 제약이 남아 있으면 다른 사용자가 같은 키를 쓸 때 막힌다. */
ALTER TABLE "point_transactions"
	DROP CONSTRAINT IF EXISTS "uk_point_transactions_idempotency_key";
ALTER TABLE "point_transactions"
	DROP CONSTRAINT IF EXISTS "uk_point_transactions_wallet_idempotency_key";
ALTER TABLE "point_transactions"
	ADD CONSTRAINT "uk_point_transactions_wallet_idempotency_key"
	UNIQUE ("wallet_id", "idempotency_key");
