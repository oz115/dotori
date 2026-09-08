/* =========================================================
   도토리 스키마 원본 (single source of truth)

   노션 "작업 > DB 설계 > DDL" 과 이 파일이 같은 내용이어야 한다.
   노션에서 DDL 을 고쳤으면 이 파일도 같은 커밋에서 고친다.

   [언제 실행되나]
   docker-compose.local.yml 이 이 폴더를 postgres 컨테이너의
   /docker-entrypoint-initdb.d 로 물려 준다. postgres 공식 이미지는
   데이터 볼륨이 "비어 있을 때만" 이 폴더의 *.sql 을 이름순으로 한 번 실행한다.

   즉 스키마를 고친 뒤에는 볼륨을 지우고 다시 띄워야 반영된다:
       docker compose -f docker-compose.local.yml down -v
       docker compose -f docker-compose.local.yml up -d

   [공용 EC2 DB]
   공용 DB 는 QA 용이라 여기서 자동으로 돌지 않는다.
   반영이 필요하면 인프라 담당자와 협의해 수동으로 실행한다.
   맨 위 DROP 문이 데이터를 전부 날리므로 그냥 붙여넣지 않는다.

   [주의]
   ddl-auto 는 none 이다. Hibernate 는 스키마를 만들지도 고치지도 않는다.
   엔티티에 컬럼을 추가했으면 이 파일에도 추가해야 한다.
   ========================================================= */

DROP TABLE IF EXISTS
"account_transactions",
"point_transactions",
"point_allowance_policies",
"point_cashout_requests",
"investment_transactions",
"savings_transactions",
"chore_completion_requests",
"investment_price_history",
"investment_news_products",
"quiz_attempts",
"purchases",
"quizzes",
"savings_subscriptions",
"chores",
"store_products",
"family_codes",
"family_members",
"linked_accounts",
"point_wallets",
"password_reset_tokens",
"refresh_tokens",
"email_verifications",
"investment_news",
"investment_products",
"savings_products",
"words",
"families",
"users"
CASCADE;

DROP TYPE IF EXISTS
account_transactions_transaction_type,
point_transactions_transaction_type,
point_allowance_policies_period_type,
point_cashout_requests_status,
users_role,
investment_transactions_transaction_type,
investment_products_category,
savings_products_product_type,
savings_subscriptions_status,
savings_transactions_transaction_type,
chores_repeat_type,
chore_completion_requests_status,
store_products_category,
words_category
CASCADE;

CREATE TYPE account_transactions_transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL');

CREATE TYPE point_transactions_transaction_type AS ENUM (
    'CHARGE', 'GRANT', 'ALLOWANCE', 'CHORE_REWARD', 'QUIZ_REWARD', 'PURCHASE',
    'SAVINGS_DEPOSIT', 'SAVINGS_WITHDRAWAL', 'INVESTMENT_BUY', 'INVESTMENT_SELL',
    'CASHOUT', 'REFUND');

CREATE TYPE point_cashout_requests_status AS ENUM ('PENDING', 'PROGRESS', 'CANCELLED', 'SUCCEED', 'FAILED');

CREATE TYPE users_role AS ENUM ('PARENT', 'CHILD');

CREATE TYPE investment_transactions_transaction_type AS ENUM ('BUY', 'SELL');

CREATE TYPE investment_products_category AS ENUM ('STOCK', 'BOND', 'ETF', 'FUND', 'OTHER');

CREATE TYPE savings_products_product_type AS ENUM ('INSTALLMENT', 'DEPOSIT');

CREATE TYPE savings_subscriptions_status AS ENUM ('ACTIVE', 'PROGRESS', 'EXPIRED', 'CANCELLED');

CREATE TYPE savings_transactions_transaction_type AS ENUM (
    'INITIAL_DEPOSIT', 'CONTRIBUTION', 'INTEREST', 'EARLY_TERMINATION_PAYOUT', 'MATURITY_PAYOUT');

CREATE TYPE chores_repeat_type AS ENUM ('DAY', 'WEEK', 'MONTH');

CREATE TYPE chore_completion_requests_status AS ENUM ('PENDING', 'PROGRESS', 'CANCELLED', 'SUCCEED', 'REJECTED');

CREATE TYPE store_products_category AS ENUM ('GAME', 'SNACK', 'TOY', 'OUTING', 'PRIVILEGE', 'ETC');

CREATE TYPE words_category AS ENUM (
    'INCOME', 'CONSUMPTION', 'SAVINGS', 'INVESTMENT', 'BANKING', 'ECONOMY', 'OTHER');

CREATE TABLE "savings_products" (
    "savings_product_id"    SERIAL          NOT NULL,
    "name"                  VARCHAR(100)    NOT NULL,
    "product_type"          savings_products_product_type NOT NULL,
    "description"           VARCHAR(255)    NULL,
    "interest"              INTEGER         NOT NULL,
    "term_months"           INTEGER         NOT NULL,
    "minimum_amount"        INTEGER         DEFAULT 0 NOT NULL,
    "due_term"              INTEGER         NULL,
    "is_active"             BOOLEAN         DEFAULT TRUE NOT NULL,
    "created_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "linked_accounts" (
    "account_id"            SERIAL          NOT NULL,
    "user_id"               INTEGER         NOT NULL,
    "bank_code"             VARCHAR(50)     NOT NULL,
    "bank_name"             VARCHAR(100)    NOT NULL,
    "account_number"        VARCHAR(100)    NOT NULL,
    "account_name"          VARCHAR(100)    NULL,
    "finance_user_key"      VARCHAR(100)    NOT NULL,
    "linked_at"             TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "users" (
    "user_id"               SERIAL          NOT NULL,
    "login_id"              VARCHAR(50)     NOT NULL,
    "password"              VARCHAR(255)    NOT NULL,
    "name"                  VARCHAR(50)     NOT NULL,
    "email"                 VARCHAR(255)    NOT NULL,
    "role"                  users_role      NOT NULL,
    "email_verified"        BOOLEAN         DEFAULT FALSE NOT NULL,
    "created_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "deleted_at"            TIMESTAMPTZ     NULL
);

CREATE TABLE "point_transactions" (
    "point_transaction_id"  SERIAL          NOT NULL,
    "child_id"              INTEGER         NOT NULL,
    "idempotency_key"       VARCHAR(100)    NULL,
    "transaction_type"      point_transactions_transaction_type NOT NULL,
    "amount"                INTEGER         NOT NULL,
    "balance_after"         INTEGER         NOT NULL,
    "description"           VARCHAR(255)    NULL,
    "created_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "families" (
    "family_id"             SERIAL          NOT NULL,
    "name"                  VARCHAR(100)    NOT NULL,
    "created_by"            INTEGER         NOT NULL,
    "created_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "deleted_at"            TIMESTAMPTZ     NULL
);

CREATE TABLE "savings_subscriptions" (
    "savings_subscription_id"   SERIAL      NOT NULL,
    "family_id"                 INTEGER     NOT NULL,
    "child_id"                  INTEGER     NOT NULL,
    "savings_product_id"        INTEGER     NOT NULL,
    "principal"                 INTEGER     NOT NULL,
    "current_amount"            INTEGER     NOT NULL,
    "status"                    savings_subscriptions_status DEFAULT 'ACTIVE' NOT NULL,
    "started_at"                TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "maturity_at"               TIMESTAMPTZ NOT NULL,
    "terminated_at"             TIMESTAMPTZ NULL,
    "received_at"               TIMESTAMPTZ NULL
);

CREATE TABLE "chore_completion_requests" (
    "chore_completion_request_id"   SERIAL      NOT NULL,
    "chore_id"                      INTEGER     NOT NULL,
    "child_id"                      INTEGER     NOT NULL,
    "photo_url"                     VARCHAR(100) NULL,
    "description"                   VARCHAR(255) NULL,
    "status"                        chore_completion_requests_status DEFAULT 'PENDING' NOT NULL,
    "requested_at"                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "processed_at"                  TIMESTAMPTZ NULL
);

CREATE TABLE "account_transactions" (
    "account_transaction_id"    SERIAL      NOT NULL,
    "account_id"                INTEGER     NOT NULL,
    "transaction_type"          account_transactions_transaction_type NOT NULL,
    "amount"                    INTEGER     NOT NULL,
    "balance_after"             INTEGER     NULL,
    "description"               VARCHAR(255) NULL,
    "transacted_at"             TIMESTAMPTZ NOT NULL
);

CREATE TABLE "investment_transactions" (
    "investment_transaction_id" SERIAL      NOT NULL,
    "family_id"                 INTEGER     NOT NULL,
    "child_id"                  INTEGER     NOT NULL,
    "investment_product_id"     INTEGER     NOT NULL,
    "transaction_type"          investment_transactions_transaction_type NOT NULL,
    "quantity"                  INTEGER     NOT NULL,
    "unit_price"                INTEGER     NOT NULL,
    "total_amount"              INTEGER     NOT NULL,
    "created_at"                TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "password_reset_tokens" (
    "password_reset_token_id"   SERIAL      NOT NULL,
    "user_id"                   INTEGER     NOT NULL,
    "token"                     VARCHAR(255) NOT NULL,
    "expires_at"                TIMESTAMPTZ NOT NULL,
    "used_at"                   TIMESTAMPTZ NULL,
    "created_at"                TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "investment_products" (
    "investment_product_id" SERIAL      NOT NULL,
    "name"                  VARCHAR(100) NOT NULL,
    "description"           VARCHAR(255) NULL,
    "category"              investment_products_category NULL,
    "current_price"         INTEGER     NOT NULL,
    "is_active"             BOOLEAN     DEFAULT TRUE NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "point_cashout_requests" (
    "cashout_request_id"    SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "child_id"              INTEGER     NOT NULL,
    "parent_id"             INTEGER     NOT NULL,
    "point_amount"          INTEGER     NOT NULL,
    "status"                point_cashout_requests_status DEFAULT 'PENDING' NOT NULL,
    "requested_at"          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "processed_at"          TIMESTAMPTZ NULL
);

CREATE TABLE "investment_price_history" (
    "investment_price_history_id"  SERIAL      NOT NULL,
    "investment_product_id"        INTEGER     NOT NULL,
    "price"                        INTEGER     NOT NULL,
    "recorded_at"                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "refresh_tokens" (
    "refresh_token_id"      SERIAL      NOT NULL,
    "user_id"               INTEGER     NOT NULL,
    "token_hash"            VARCHAR(255) NOT NULL,
    "expires_at"            TIMESTAMPTZ NOT NULL,
    "revoked_at"            TIMESTAMPTZ NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "family_codes" (
    "family_code_id"        SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "code"                  VARCHAR(100) NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "expired_at"            TIMESTAMPTZ NOT NULL
);

CREATE TABLE "savings_transactions" (
    "savings_transaction_id"    SERIAL      NOT NULL,
    "savings_subscription_id"   INTEGER     NOT NULL,
    "transaction_type"          savings_transactions_transaction_type NOT NULL,
    "amount"                    INTEGER     NOT NULL,
    "created_at"                TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "investment_news" (
    "investment_news_id"    SERIAL      NOT NULL,
    "title"                 VARCHAR(255) NOT NULL,
    "content"               VARCHAR(255) NOT NULL,
    "published_at"          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "store_products" (
    "store_product_id"      SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "created_by"            INTEGER     NOT NULL,
    "name"                  VARCHAR(100) NOT NULL,
    "description"           VARCHAR(255) NULL,
    "category"              store_products_category NOT NULL,
    "value"                 NUMERIC(10, 2) NULL,
    "price"                 INTEGER     NULL,
    "quantity"              INTEGER     DEFAULT 0 NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "words" (
    "word_id"               SERIAL      NOT NULL,
    "word"                  VARCHAR(100) NOT NULL,
    "description"           VARCHAR(255) NOT NULL,
    "difficulty_level"      INTEGER     NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "category"              words_category NULL
);

CREATE TABLE "investment_news_products" (
    "investment_news_product_id"  SERIAL  NOT NULL,
    "investment_news_id"          INTEGER NOT NULL,
    "investment_product_id"       INTEGER NOT NULL
);

CREATE TABLE "email_verifications" (
    "email_verification_id" SERIAL       NOT NULL,
    "user_id"               INTEGER      NOT NULL,
    "token"                 VARCHAR(255) NOT NULL,
    "expires_at"            TIMESTAMPTZ  NOT NULL,
    "verified_at"           TIMESTAMPTZ  NULL,
    "created_at"            TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "family_members" (
    "family_member_id"      SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "user_id"               INTEGER     NOT NULL,
    "joined_at"             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "deleted_at"            TIMESTAMPTZ NULL
);

CREATE TABLE "quizzes" (
    "quiz_id"               SERIAL       NOT NULL,
    "family_id"             INTEGER      NOT NULL,
    "child_id"              INTEGER      NOT NULL,
    "word_id"               INTEGER      NOT NULL,
    "question"              TEXT         NOT NULL,
    "correct_answer"        VARCHAR(255) NOT NULL,
    "explanation"           VARCHAR(255) NULL,
    "reward_points"         INTEGER      DEFAULT 0 NOT NULL,
    "quiz_date"             DATE         DEFAULT CURRENT_DATE NOT NULL,
    "created_at"            TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "purchases" (
    "purchase_id"           SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "child_id"              INTEGER     NOT NULL,
    "store_product_id"      INTEGER     NOT NULL,
    "quantity"              INTEGER     DEFAULT 1 NOT NULL,
    "price"                 INTEGER     NOT NULL,
    "purchase_date"         DATE        DEFAULT CURRENT_DATE NOT NULL,
    "purchased_at"          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "quiz_attempts" (
    "quiz_attempt_id"       SERIAL       NOT NULL,
    "quiz_id"               INTEGER      NOT NULL,
    "child_id"              INTEGER      NOT NULL,
    "submitted_answer"      VARCHAR(255) NOT NULL,
    "is_correct"            BOOLEAN      NOT NULL,
    "earned_points"         INTEGER      DEFAULT 0 NOT NULL,
    "submitted_at"          TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "point_wallets" (
    "wallet_id"             SERIAL      NOT NULL,
    "child_id"              INTEGER     NOT NULL,
    "balance"               INTEGER     DEFAULT 0 NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE "chores" (
    "chore_id"              SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "parent_id"             INTEGER     NOT NULL,
    "child_id"              INTEGER     NOT NULL,
    "title"                 VARCHAR(100) NOT NULL,
    "description"           VARCHAR(255) NULL,
    "reward_points"         INTEGER     NOT NULL,
    "repeat_type"           chores_repeat_type NOT NULL,
    "start_date"            DATE        DEFAULT CURRENT_DATE NOT NULL,
    "end_date"              DATE        NULL,
    "is_active"             BOOLEAN     DEFAULT TRUE NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE "savings_products" ADD CONSTRAINT "PK_SAVINGS_PRODUCTS" PRIMARY KEY ("savings_product_id");
ALTER TABLE "linked_accounts" ADD CONSTRAINT "PK_LINKED_ACCOUNTS" PRIMARY KEY ("account_id");
ALTER TABLE "users" ADD CONSTRAINT "PK_USERS" PRIMARY KEY ("user_id");
ALTER TABLE "point_transactions" ADD CONSTRAINT "PK_POINT_TRANSACTIONS" PRIMARY KEY ("point_transaction_id");
ALTER TABLE "families" ADD CONSTRAINT "PK_FAMILIES" PRIMARY KEY ("family_id");
ALTER TABLE "savings_subscriptions" ADD CONSTRAINT "PK_SAVINGS_SUBSCRIPTIONS" PRIMARY KEY ("savings_subscription_id");
ALTER TABLE "chore_completion_requests" ADD CONSTRAINT "PK_CHORE_COMPLETION_REQUESTS" PRIMARY KEY ("chore_completion_request_id");
ALTER TABLE "account_transactions" ADD CONSTRAINT "PK_ACCOUNT_TRANSACTIONS" PRIMARY KEY ("account_transaction_id");
ALTER TABLE "investment_transactions" ADD CONSTRAINT "PK_INVESTMENT_TRANSACTIONS" PRIMARY KEY ("investment_transaction_id");
ALTER TABLE "password_reset_tokens" ADD CONSTRAINT "PK_PASSWORD_RESET_TOKENS" PRIMARY KEY ("password_reset_token_id");
ALTER TABLE "investment_products" ADD CONSTRAINT "PK_INVESTMENT_PRODUCTS" PRIMARY KEY ("investment_product_id");
ALTER TABLE "point_cashout_requests" ADD CONSTRAINT "PK_POINT_CASHOUT_REQUESTS" PRIMARY KEY ("cashout_request_id");
ALTER TABLE "investment_price_history" ADD CONSTRAINT "PK_INVESTMENT_PRICE_HISTORY" PRIMARY KEY ("investment_price_history_id");
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "PK_REFRESH_TOKENS" PRIMARY KEY ("refresh_token_id");
ALTER TABLE "family_codes" ADD CONSTRAINT "PK_FAMILY_CODES" PRIMARY KEY ("family_code_id");
ALTER TABLE "savings_transactions" ADD CONSTRAINT "PK_SAVINGS_TRANSACTIONS" PRIMARY KEY ("savings_transaction_id");
ALTER TABLE "investment_news" ADD CONSTRAINT "PK_INVESTMENT_NEWS" PRIMARY KEY ("investment_news_id");
ALTER TABLE "store_products" ADD CONSTRAINT "PK_STORE_PRODUCTS" PRIMARY KEY ("store_product_id");
ALTER TABLE "words" ADD CONSTRAINT "PK_WORDS" PRIMARY KEY ("word_id");
ALTER TABLE "investment_news_products" ADD CONSTRAINT "PK_INVESTMENT_NEWS_PRODUCTS" PRIMARY KEY ("investment_news_product_id");
ALTER TABLE "email_verifications" ADD CONSTRAINT "PK_EMAIL_VERIFICATIONS" PRIMARY KEY ("email_verification_id");
ALTER TABLE "family_members" ADD CONSTRAINT "PK_FAMILY_MEMBERS" PRIMARY KEY ("family_member_id");
ALTER TABLE "quizzes" ADD CONSTRAINT "PK_QUIZZES" PRIMARY KEY ("quiz_id");
ALTER TABLE "purchases" ADD CONSTRAINT "PK_PURCHASES" PRIMARY KEY ("purchase_id");
ALTER TABLE "quiz_attempts" ADD CONSTRAINT "PK_QUIZ_ATTEMPTS" PRIMARY KEY ("quiz_attempt_id");
ALTER TABLE "point_wallets" ADD CONSTRAINT "PK_POINT_WALLETS" PRIMARY KEY ("wallet_id");
ALTER TABLE "chores" ADD CONSTRAINT "PK_CHORES" PRIMARY KEY ("chore_id");

ALTER TABLE "linked_accounts" ADD CONSTRAINT "FK_LINKED_ACCOUNTS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id");
ALTER TABLE "point_wallets" ADD CONSTRAINT "FK_POINT_WALLETS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "point_transactions" ADD CONSTRAINT "FK_POINT_TRANSACTIONS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "families" ADD CONSTRAINT "FK_FAMILIES_CREATED_BY" FOREIGN KEY ("created_by") REFERENCES "users" ("user_id");
ALTER TABLE "family_members" ADD CONSTRAINT "FK_FAMILY_MEMBERS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "family_members" ADD CONSTRAINT "FK_FAMILY_MEMBERS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id");
ALTER TABLE "family_codes" ADD CONSTRAINT "FK_FAMILY_CODES_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "password_reset_tokens" ADD CONSTRAINT "FK_PASSWORD_RESET_TOKENS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id");
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "FK_REFRESH_TOKENS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id");
ALTER TABLE "email_verifications" ADD CONSTRAINT "FK_EMAIL_VERIFICATIONS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("user_id");
ALTER TABLE "account_transactions" ADD CONSTRAINT "FK_ACCOUNT_TRANSACTIONS_ACCOUNT" FOREIGN KEY ("account_id") REFERENCES "linked_accounts" ("account_id");
ALTER TABLE "point_cashout_requests" ADD CONSTRAINT "FK_POINT_CASHOUT_REQUESTS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "point_cashout_requests" ADD CONSTRAINT "FK_POINT_CASHOUT_REQUESTS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "point_cashout_requests" ADD CONSTRAINT "FK_POINT_CASHOUT_REQUESTS_PARENT" FOREIGN KEY ("parent_id") REFERENCES "users" ("user_id");
ALTER TABLE "savings_subscriptions" ADD CONSTRAINT "FK_SAVINGS_SUBSCRIPTIONS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "savings_subscriptions" ADD CONSTRAINT "FK_SAVINGS_SUBSCRIPTIONS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "savings_subscriptions" ADD CONSTRAINT "FK_SAVINGS_SUBSCRIPTIONS_PRODUCT" FOREIGN KEY ("savings_product_id") REFERENCES "savings_products" ("savings_product_id");
ALTER TABLE "savings_transactions" ADD CONSTRAINT "FK_SAVINGS_TRANSACTIONS_SUBSCRIPTION" FOREIGN KEY ("savings_subscription_id") REFERENCES "savings_subscriptions" ("savings_subscription_id");
ALTER TABLE "investment_transactions" ADD CONSTRAINT "FK_INVESTMENT_TRANSACTIONS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "investment_transactions" ADD CONSTRAINT "FK_INVESTMENT_TRANSACTIONS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "investment_transactions" ADD CONSTRAINT "FK_INVESTMENT_TRANSACTIONS_PRODUCT" FOREIGN KEY ("investment_product_id") REFERENCES "investment_products" ("investment_product_id");
ALTER TABLE "investment_price_history" ADD CONSTRAINT "FK_INVESTMENT_PRICE_HISTORY_PRODUCT" FOREIGN KEY ("investment_product_id") REFERENCES "investment_products" ("investment_product_id");
ALTER TABLE "investment_news_products" ADD CONSTRAINT "FK_INVESTMENT_NEWS_PRODUCTS_NEWS" FOREIGN KEY ("investment_news_id") REFERENCES "investment_news" ("investment_news_id");
ALTER TABLE "investment_news_products" ADD CONSTRAINT "FK_INVESTMENT_NEWS_PRODUCTS_PRODUCT" FOREIGN KEY ("investment_product_id") REFERENCES "investment_products" ("investment_product_id");
ALTER TABLE "store_products" ADD CONSTRAINT "FK_STORE_PRODUCTS_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "store_products" ADD CONSTRAINT "FK_STORE_PRODUCTS_CREATED_BY" FOREIGN KEY ("created_by") REFERENCES "users" ("user_id");
ALTER TABLE "purchases" ADD CONSTRAINT "FK_PURCHASES_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "purchases" ADD CONSTRAINT "FK_PURCHASES_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "purchases" ADD CONSTRAINT "FK_PURCHASES_STORE_PRODUCT" FOREIGN KEY ("store_product_id") REFERENCES "store_products" ("store_product_id");
ALTER TABLE "quizzes" ADD CONSTRAINT "FK_QUIZZES_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "quizzes" ADD CONSTRAINT "FK_QUIZZES_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "quizzes" ADD CONSTRAINT "FK_QUIZZES_WORD" FOREIGN KEY ("word_id") REFERENCES "words" ("word_id");
ALTER TABLE "quiz_attempts" ADD CONSTRAINT "FK_QUIZ_ATTEMPTS_QUIZ" FOREIGN KEY ("quiz_id") REFERENCES "quizzes" ("quiz_id");
ALTER TABLE "quiz_attempts" ADD CONSTRAINT "FK_QUIZ_ATTEMPTS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "chores" ADD CONSTRAINT "FK_CHORES_FAMILY" FOREIGN KEY ("family_id") REFERENCES "families" ("family_id");
ALTER TABLE "chores" ADD CONSTRAINT "FK_CHORES_PARENT" FOREIGN KEY ("parent_id") REFERENCES "users" ("user_id");
ALTER TABLE "chores" ADD CONSTRAINT "FK_CHORES_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");
ALTER TABLE "chore_completion_requests" ADD CONSTRAINT "FK_CHORE_COMPLETION_REQUESTS_CHORE" FOREIGN KEY ("chore_id") REFERENCES "chores" ("chore_id");
ALTER TABLE "chore_completion_requests" ADD CONSTRAINT "FK_CHORE_COMPLETION_REQUESTS_CHILD" FOREIGN KEY ("child_id") REFERENCES "users" ("user_id");

ALTER TABLE "users" ADD CONSTRAINT "UQ_USERS_LOGIN_ID" UNIQUE ("login_id");
ALTER TABLE "users" ADD CONSTRAINT "UQ_USERS_EMAIL" UNIQUE ("email");
ALTER TABLE "linked_accounts" ADD CONSTRAINT "UQ_LINKED_ACCOUNTS_USER_ACCOUNT" UNIQUE ("user_id", "account_number");
ALTER TABLE "point_transactions" ADD CONSTRAINT "UQ_POINT_TRANSACTIONS_CHILD_IDEMPOTENCY_KEY" UNIQUE ("child_id", "idempotency_key");
ALTER TABLE "point_transactions" ADD CONSTRAINT "CK_POINT_TRANSACTIONS_AMOUNT_POSITIVE" CHECK ("amount" > 0);
ALTER TABLE "point_wallets" ADD CONSTRAINT "UQ_POINT_WALLETS_CHILD_ID" UNIQUE ("child_id");
ALTER TABLE "point_wallets" ADD CONSTRAINT "CK_POINT_WALLETS_BALANCE_NON_NEGATIVE" CHECK ("balance" >= 0);
ALTER TABLE "family_codes" ADD CONSTRAINT "UQ_FAMILY_CODES_CODE" UNIQUE ("code");
ALTER TABLE "store_products" ADD CONSTRAINT "CK_STORE_PRODUCTS_VALUE_POSITIVE" CHECK ("value" IS NULL OR "value" > 0);
ALTER TABLE "store_products" ADD CONSTRAINT "CK_STORE_PRODUCTS_PRICE_POSITIVE" CHECK ("price" IS NULL OR "price" > 0);

CREATE INDEX "IX_POINT_TRANSACTIONS_CHILD_CREATED_AT" ON "point_transactions" ("child_id", "created_at", "point_transaction_id");
