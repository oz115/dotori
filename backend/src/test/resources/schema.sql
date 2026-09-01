/* =========================================================
   테스트용 스키마 (포인트 지갑 범위)

   팀 전체 DDL 중 지갑 기능이 실제로 건드리는 테이블만 옮겨 왔다.
   전체 DDL 을 그대로 쓰지 않는 이유는 investment_products.category 등
   일부 컬럼이 아직 정책 미확정으로 비어 있어 그대로 실행되지 않기 때문이다.

   운영 스키마와 어긋나면 테스트가 의미를 잃으므로,
   지갑 관련 테이블이 바뀌면 이 파일도 반드시 함께 고친다.

   ── 문장 구분자가 ";;" 인 이유 ──
   아래 DO 블록 안에 세미콜론이 들어 있는데, 스프링의 ScriptUtils 는
   PostgreSQL 의 달러 인용($$)을 이해하지 못해 그 세미콜론에서 문장을 잘라 버린다.
   그래서 application-test.yaml 에서 spring.sql.init.separator 를 ";;" 로 지정했다.
   새 문장을 추가할 때도 반드시 ";;" 로 끝내야 한다.
   ========================================================= */

DROP TABLE IF EXISTS
	"point_transactions",
	"point_wallets",
	"family_members",
	"families",
	"users"
CASCADE;;


/* ---------------------------------------------------------
   ENUM 타입은 절대 DROP 하지 않는다.

   이 스크립트는 스프링 테스트 컨텍스트마다 다시 실행된다.
   그때마다 DROP TYPE / CREATE TYPE 을 하면 이름이 같아도 OID 가 새로 발급되는데,
   PostgreSQL JDBC 드라이버는 enum 의 OID 를 커넥션에 캐시한다.
   그 결과 먼저 만들어진 컨텍스트의 커넥션 풀이 사라진 OID 를 들고 있다가
   insert 시점에 "cache lookup failed for type NNNNN" 으로 실패한다.

   그래서 타입은 없을 때만 만들고, 한 번 만들어지면 그대로 둔다.
   --------------------------------------------------------- */

DO $$ BEGIN
	CREATE TYPE users_role AS ENUM ('PARENT', 'CHILD');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END $$;;

DO $$ BEGIN
	CREATE TYPE point_transactions_transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END $$;;


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
);;


CREATE TABLE "families" (
	"family_id"	SERIAL	NOT NULL,
	"name"	VARCHAR(100)	NOT NULL,
	"created_by"	INTEGER	NOT NULL,
	"created_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	"updated_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_FAMILIES" PRIMARY KEY ("family_id"),
	CONSTRAINT "FK_FAMILIES_CREATED_BY" FOREIGN KEY ("created_by") REFERENCES "users" ("user_id")
);;


CREATE TABLE "family_members" (
	"family_member_id"	SERIAL	NOT NULL,
	"family_id"	INTEGER	NOT NULL,
	"user_id"	INTEGER	NOT NULL,
	"joined_at"	TIMESTAMPTZ	DEFAULT CURRENT_TIMESTAMP	NOT NULL,
	CONSTRAINT "PK_FAMILY_MEMBERS" PRIMARY KEY ("family_member_id"),
	CONSTRAINT "FK_FAMILY_MEMBERS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id"),
	CONSTRAINT "FK_FAMILY_MEMBERS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id")
);;


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
);;


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
);;

CREATE UNIQUE INDEX "UX_POINT_TRANSACTIONS_IDEMPOTENCY_KEY"
	ON "point_transactions" ("idempotency_key")
	WHERE "idempotency_key" IS NOT NULL;;
