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
   핵심 기능(에픽)이 모두 마무리된 뒤에 인프라 담당자와 협의해 한 번에 반영한다.
   맨 위 DROP 문이 데이터를 전부 날리므로 그냥 붙여넣지 않는다.

   [주의]
   ddl-auto 는 none 이다. Hibernate 는 스키마를 만들지도 고치지도 않는다.
   엔티티에 컬럼을 추가했으면 이 파일에도 추가해야 한다.
   ========================================================= */


/* =========================================================
1. TABLE INITIALIZATION
========================================================= */

DROP TABLE IF EXISTS
"notifications",
"device_tokens",
"point_transactions",
"point_cashout_requests",
"investment_transactions",
"investment_holdings",
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
"refresh_tokens",
"investment_news",
"investment_products",
"savings_products",
"words",
"families",
"users"
CASCADE;


/* =========================================================
2. ENUM TYPE INITIALIZATION
========================================================= */

DROP TYPE IF EXISTS
notifications_type,
device_tokens_platform,
point_transactions_transaction_type,
point_cashout_requests_status,
users_role,
investment_transactions_transaction_type,
investment_products_category,
savings_products_product_type,
savings_subscriptions_status,
savings_transactions_transaction_type,
chores_repeat_type,
task_difficulty,
chore_completion_requests_status,
store_products_category,
words_category,
investment_news_impact
CASCADE;


/* =========================================================
3. ENUM TYPE CREATION
========================================================= */

/* 알림 타입 */
CREATE TYPE notifications_type AS ENUM (
    'INVITATION',
    'PURCHASE',
    'CHARGE',
    'CASHOUT',
    'CHORE',
    'SAVINGS',
    'INVESTMENT'
);


/* FCM 푸시를 수신할 디바이스 플랫폼 */
CREATE TYPE device_tokens_platform AS ENUM (
    'ANDROID',
    'IOS'
);


/* 포인트 거래 발생 사유
   입출금 방향이 아닌 "왜 포인트가 변했는지"를 저장 */
CREATE TYPE point_transactions_transaction_type AS ENUM (
    'CHARGE',
    'GRANT',
    'ALLOWANCE',
    'CHORE_REWARD',
    'QUIZ_REWARD',
    'PURCHASE',
    'SAVINGS_DEPOSIT',
    'SAVINGS_WITHDRAWAL',
    'INVESTMENT_BUY',
    'INVESTMENT_SELL',
    'CASHOUT',
    'REFUND'
);


/* 포인트 현금화 요청 상태 */
CREATE TYPE point_cashout_requests_status AS ENUM (
    'PENDING',
    'PROGRESS',
    'CANCELLED',
    'DECLINED',
    'SUCCEED',
    'FAILED'
);


/* 사용자 역할 */
CREATE TYPE users_role AS ENUM (
    'PARENT',
    'CHILD'
);


/* 투자 거래 유형 */
CREATE TYPE investment_transactions_transaction_type AS ENUM (
    'BUY',
    'SELL'
);


/* 투자 상품 카테고리 */
CREATE TYPE investment_products_category AS ENUM (
    'STOCK',
    'BOND',
    'ETF',
    'FUND',
    'OTHER'
);


/* 예적금 상품 유형 */
CREATE TYPE savings_products_product_type AS ENUM (
    'INSTALLMENT',
    'DEPOSIT'
);


/* 예적금 가입 상태 */
CREATE TYPE savings_subscriptions_status AS ENUM (
    'ACTIVE',
    'PROGRESS',
    'EXPIRED',
    'CANCELLED'
);


/* 예적금 거래 유형 */
CREATE TYPE savings_transactions_transaction_type AS ENUM (
    'INITIAL_DEPOSIT',
    'CONTRIBUTION',
    'INTEREST',
    'EARLY_TERMINATION_PAYOUT',
    'MATURITY_PAYOUT'
);


/* 할 일 반복 주기

   ONCE 는 반복하지 않는 일회성이다. 화면의 "반복" 체크를 끄면 이 값이 되고,
   그때는 부모가 시작일과 종료일을 직접 넣는다.

   나머지 셋은 반복이다. 부모는 주기만 고르고, 기간은 등록한 날에서 계산한다
   (일간은 그날, 주간은 그 주 일요일, 월간은 그 달 말일).
   주기가 끝나면 크론이 다음 주기로 기간을 밀어 준다. */
CREATE TYPE chores_repeat_type AS ENUM (
    'ONCE',
    'DAY',
    'WEEK',
    'MONTH'
);


/* 할 일 난이도
   AI 가 제목·설명을 보고 판정하며, 주간 기본 용돈(families.weekly_allowance)
   대비 보상 가중치(chores.value)를 매기는 근거가 된다.
   집안일 외의 할 일에도 쓸 수 있도록 테이블명을 앞에 붙이지 않았다. */
CREATE TYPE task_difficulty AS ENUM (
    'EASY',
    'NORMAL',
    'HARD'
);


/* 집안일 완료 요청 상태

   PENDING 에서 시작해 SUCCEED · REJECTED · CANCELLED 중 하나로 한 번만 옮겨 간다.
   되돌아오는 전이는 없다.

   PROGRESS(처리 중)를 두지 않는다. 아이가 사진과 내용을 한 번에 올려 요청이 완성되고,
   부모가 거절하면 그 행을 고치는 게 아니라 아이가 새 요청을 올린다. 즉 "올리는 중"
   이나 "고치는 중" 으로 머무를 구간이 없다.
   (현금화처럼 외부 송금을 기다리는 요청은 그 구간이 있어 PROGRESS 를 쓴다.) */
CREATE TYPE chore_completion_requests_status AS ENUM (
    'PENDING',
    'PROGRESS',
    'CANCELLED',
    'SUCCEED',
    'REJECTED'
);


/* 상점 상품 카테고리
   카테고리별 이미지는 S3에 미리 준비해 두고, 상품에는 카테고리만 저장한다 */
CREATE TYPE store_products_category AS ENUM (
    'GAME',
    'SNACK',
    'TOY',
    'OUTING',
    'PRIVILEGE',
    'ETC'
);


/* 경제 용어 카테고리 */
CREATE TYPE words_category AS ENUM (
    'INCOME',
    'CONSUMPTION',
    'SAVINGS',
    'INVESTMENT',
    'BANKING',
    'ECONOMY',
    'OTHER'
);


/* 투자 뉴스가 상품 가격에 주는 영향 방향 */
CREATE TYPE investment_news_impact AS ENUM (
    'POSITIVE',
    'NEGATIVE'
);


/* =========================================================
4. TABLE CREATION
========================================================= */

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


/* deleted_at: 가족 소프트 삭제. Family 엔티티가 사용한다.

   아래 세 값은 가족 생성 온보딩에서 정한다.
   weekly_allowance:          B — 주간 기본 용돈(포인트 환산).
                              할 일 보상 산정의 기준값이다 (chores.reward_points = value * B).
   weekly_reward_limit:       A — 예적금·투자·퀴즈로 주당 벌 수 있는 최대 포인트 (할 일 제외).
   financial_knowledge_level: 아이의 금융 이해도. AI 가 보상·퀴즈 난이도를 정할 때 참고한다.

   셋 다 NOT NULL 이지만 DEFAULT 를 둔다. Family 엔티티가 아직 이 컬럼들을 매핑하지 않아
   Hibernate 의 INSERT 문에 컬럼이 실리지 않는데, DEFAULT 가 없으면 그 INSERT 가
   NOT NULL 위반으로 실패해 가족 생성 API 가 통째로 깨진다.
   엔티티가 세 필드를 갖게 되면 DEFAULT 는 쓰이지 않는다.

   financial_knowledge_level 은 이 스키마에서 유일하게 네이티브 ENUM 이 아닌 열거형이다.
   엔티티가 @Enumerated(EnumType.STRING) + length=20 으로 잡혀 있어 그쪽에 맞췄고,
   대신 CHECK 로 값을 제한해 네이티브 ENUM 과 같은 보호를 준다. */
CREATE TABLE "families" (
    "family_id"                 SERIAL          NOT NULL,
    "name"                      VARCHAR(100)    NOT NULL,
    "created_by"                INTEGER         NOT NULL,
    "weekly_allowance"          INTEGER         DEFAULT 0 NOT NULL,
    "weekly_reward_limit"       INTEGER         DEFAULT 0 NOT NULL,
    "financial_knowledge_level" VARCHAR(20)     DEFAULT 'BEGINNER' NOT NULL,
    "created_at"                TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"                TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "deleted_at"                TIMESTAMPTZ     NULL
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


/* photo_key: 인증 사진의 S3 저장 키다 (CHORE-13). URL 이 아니다.
             형식은 chores/{family_id}/{child_id}/{uuid}.jpg 로 약 55자다.

             버킷이 비공개라 URL 을 저장해도 그 URL 로는 아무도 볼 수 없다. 볼 수 있는
             presigned URL 은 서명과 만료시각이 붙어 500자를 넘고 몇 분 뒤 죽으므로
             저장할 값이 아니다. 화면에 보여 줄 때마다 이 키로 새로 발급한다. */
CREATE TABLE "chore_completion_requests" (
    "chore_completion_request_id"   SERIAL      NOT NULL,
    "chore_id"                      INTEGER     NOT NULL,
    "child_id"                      INTEGER     NOT NULL,
    "photo_key"                     VARCHAR(100) NULL,
    "description"                   VARCHAR(255) NULL,
    "status"                        chore_completion_requests_status DEFAULT 'PENDING' NOT NULL,
    "requested_at"                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "processed_at"                  TIMESTAMPTZ NULL
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


CREATE TABLE "investment_products" (
    "investment_product_id" SERIAL      NOT NULL,
    "name"                  VARCHAR(100) NOT NULL,
    "description"           VARCHAR(255) NULL,
    "category"              investment_products_category NULL,
    "news_context"          TEXT        NOT NULL,
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
    "family_id"                    INTEGER     NOT NULL,
    "investment_product_id"        INTEGER     NOT NULL,
    "price"                        INTEGER     NOT NULL,
    "recorded_at"                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);


/* 아이별 현재 보유 현황. 매수/매도 API 가 investment_transactions 기록과
   같은 트랜잭션으로 이 테이블을 갱신하고, 보유 조회(INV-T-03)는 이 테이블만 읽는다.
   average_buy_price 는 매수 시점 가격의 수량 가중 평균 — 전량 매도 시 행을 지운다. */
CREATE TABLE "investment_holdings" (
    "investment_holding_id" SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "child_id"              INTEGER     NOT NULL,
    "investment_product_id" INTEGER     NOT NULL,
    "quantity"              INTEGER     NOT NULL,
    "average_buy_price"     INTEGER     NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE TABLE "refresh_tokens" (
    "refresh_token_id"      SERIAL      NOT NULL,
    "user_id"               INTEGER     NOT NULL,
    "token_hash"            VARCHAR(255) NOT NULL,
    "expires_at"            TIMESTAMPTZ NOT NULL,
    "revoked_at"            TIMESTAMPTZ NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);


/* ref_id: 딥링크용 연관 리소스 ID (예: chore_id). 알림 타입마다 가리키는 대상 테이블이
           달라 FK 를 걸 수 없다 */
CREATE TABLE "notifications" (
    "notification_id"       SERIAL      NOT NULL,
    "user_id"               INTEGER     NOT NULL,
    "type"                  notifications_type NOT NULL,
    "title"                 VARCHAR(100) NOT NULL,
    "body"                  VARCHAR(255) NOT NULL,
    "ref_id"                INTEGER     NULL,
    "is_read"               BOOLEAN     DEFAULT FALSE NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);


/* fcm_token: 유일해야 upsert(등록 API가 이미 있는 토큰이면 user_id 를 갈아치우는 방식)로
              기기 재로그인 시 이전 계정 소유권을 정리할 수 있다.
   last_used_at: 등록/갱신 API 호출마다 갱신. 오래 미사용된 토큰 정리 기준으로 쓴다. */
CREATE TABLE "device_tokens" (
    "device_token_id"       SERIAL      NOT NULL,
    "user_id"               INTEGER     NOT NULL,
    "fcm_token"             VARCHAR(255) NOT NULL,
    "platform"              device_tokens_platform NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "last_used_at"          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
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
    "impact"                investment_news_impact NOT NULL,
    "published_at"          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);


/* category: 상품 이미지는 카테고리별로 S3에 준비되므로 카테고리만 저장한다.
   value: AI 서버가 비동기로 산정하는 상품 가치 가중치(아이 하루 소득 D의 배수).
          등록 직후에는 아직 산정 전이므로 NULL, price = value * D.
   price: value 산정이 끝나야 계산할 수 있으므로 NULL 허용(NULL = 가격 산정 중). */
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
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "deleted_at"            TIMESTAMPTZ NULL
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


/* deleted_at: 가족 구성원 소프트 삭제. FamilyMember 엔티티가 사용한다. */
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
    "product_name"          VARCHAR(100) NOT NULL,
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


/* difficulty: AI 가 제목·설명을 보고 판정한 난이도. 보상 산정의 근거로 남긴다.
   value: AI 서버가 비동기로 산정하는 할 일 가치 가중치(주간 기본 용돈 B의 배수).
          등록 직후에는 아직 산정 전이므로 NULL, reward_points = value * B.
   reward_points: value 산정이 끝나야 계산할 수 있으므로 NULL 허용(NULL = 보상 산정 중).
                  승인 시점의 이 값을 지급한다. 요청 시점 값을 따로 스냅샷하지 않는다.
   deleted_at: 집안일 소프트 삭제. 완료 이력이 chore_id 를 참조하므로 행을 지우면
               이미 지급한 보상의 근거가 사라진다.
               목록에서는 빠지지만 상세 조회와 대기 중인 완료 요청 처리는 계속 된다.

   is_active 는 두지 않는다. 삭제 표시를 deleted_at 하나로 통일했다.
   둘을 함께 두면 "비활성이지만 삭제는 아닌" 상태가 생겨 목록 조회 조건이
   두 갈래로 갈리고, 어느 쪽이 삭제인지 코드마다 달라진다. */
CREATE TABLE "chores" (
    "chore_id"              SERIAL      NOT NULL,
    "family_id"             INTEGER     NOT NULL,
    "parent_id"             INTEGER     NOT NULL,
    "child_id"              INTEGER     NOT NULL,
    "title"                 VARCHAR(100) NOT NULL,
    "description"           VARCHAR(255) NULL,
    "difficulty"            task_difficulty NOT NULL,
    "value"                 NUMERIC(10, 2) NULL,
    "reward_points"         INTEGER     NULL,
    "repeat_type"           chores_repeat_type NOT NULL,
    "start_date"            DATE        DEFAULT CURRENT_DATE NOT NULL,
    "end_date"              DATE        NULL,
    "created_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at"            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "deleted_at"            TIMESTAMPTZ NULL
);


/* =========================================================
5. PRIMARY KEY
========================================================= */

ALTER TABLE "savings_products"
ADD CONSTRAINT "PK_SAVINGS_PRODUCTS"
PRIMARY KEY ("savings_product_id");


ALTER TABLE "linked_accounts"
ADD CONSTRAINT "PK_LINKED_ACCOUNTS"
PRIMARY KEY ("account_id");


ALTER TABLE "users"
ADD CONSTRAINT "PK_USERS"
PRIMARY KEY ("user_id");


ALTER TABLE "point_transactions"
ADD CONSTRAINT "PK_POINT_TRANSACTIONS"
PRIMARY KEY ("point_transaction_id");


ALTER TABLE "families"
ADD CONSTRAINT "PK_FAMILIES"
PRIMARY KEY ("family_id");


ALTER TABLE "savings_subscriptions"
ADD CONSTRAINT "PK_SAVINGS_SUBSCRIPTIONS"
PRIMARY KEY ("savings_subscription_id");


ALTER TABLE "chore_completion_requests"
ADD CONSTRAINT "PK_CHORE_COMPLETION_REQUESTS"
PRIMARY KEY ("chore_completion_request_id");


ALTER TABLE "investment_transactions"
ADD CONSTRAINT "PK_INVESTMENT_TRANSACTIONS"
PRIMARY KEY ("investment_transaction_id");


ALTER TABLE "investment_products"
ADD CONSTRAINT "PK_INVESTMENT_PRODUCTS"
PRIMARY KEY ("investment_product_id");


ALTER TABLE "point_cashout_requests"
ADD CONSTRAINT "PK_POINT_CASHOUT_REQUESTS"
PRIMARY KEY ("cashout_request_id");


ALTER TABLE "investment_price_history"
ADD CONSTRAINT "PK_INVESTMENT_PRICE_HISTORY"
PRIMARY KEY ("investment_price_history_id");


ALTER TABLE "investment_holdings"
ADD CONSTRAINT "PK_INVESTMENT_HOLDINGS"
PRIMARY KEY ("investment_holding_id");


ALTER TABLE "refresh_tokens"
ADD CONSTRAINT "PK_REFRESH_TOKENS"
PRIMARY KEY ("refresh_token_id");


ALTER TABLE "notifications"
ADD CONSTRAINT "PK_NOTIFICATIONS"
PRIMARY KEY ("notification_id");


ALTER TABLE "device_tokens"
ADD CONSTRAINT "PK_DEVICE_TOKENS"
PRIMARY KEY ("device_token_id");


ALTER TABLE "family_codes"
ADD CONSTRAINT "PK_FAMILY_CODES"
PRIMARY KEY ("family_code_id");


ALTER TABLE "savings_transactions"
ADD CONSTRAINT "PK_SAVINGS_TRANSACTIONS"
PRIMARY KEY ("savings_transaction_id");


ALTER TABLE "investment_news"
ADD CONSTRAINT "PK_INVESTMENT_NEWS"
PRIMARY KEY ("investment_news_id");


ALTER TABLE "store_products"
ADD CONSTRAINT "PK_STORE_PRODUCTS"
PRIMARY KEY ("store_product_id");


ALTER TABLE "words"
ADD CONSTRAINT "PK_WORDS"
PRIMARY KEY ("word_id");


ALTER TABLE "investment_news_products"
ADD CONSTRAINT "PK_INVESTMENT_NEWS_PRODUCTS"
PRIMARY KEY ("investment_news_product_id");


ALTER TABLE "family_members"
ADD CONSTRAINT "PK_FAMILY_MEMBERS"
PRIMARY KEY ("family_member_id");


ALTER TABLE "quizzes"
ADD CONSTRAINT "PK_QUIZZES"
PRIMARY KEY ("quiz_id");


ALTER TABLE "purchases"
ADD CONSTRAINT "PK_PURCHASES"
PRIMARY KEY ("purchase_id");


ALTER TABLE "quiz_attempts"
ADD CONSTRAINT "PK_QUIZ_ATTEMPTS"
PRIMARY KEY ("quiz_attempt_id");


ALTER TABLE "point_wallets"
ADD CONSTRAINT "PK_POINT_WALLETS"
PRIMARY KEY ("wallet_id");


ALTER TABLE "chores"
ADD CONSTRAINT "PK_CHORES"
PRIMARY KEY ("chore_id");


/* =========================================================
6. FOREIGN KEY
========================================================= */

/* linked_accounts */
ALTER TABLE "linked_accounts"
ADD CONSTRAINT "FK_LINKED_ACCOUNTS_USER"
FOREIGN KEY ("user_id")
REFERENCES "users" ("user_id");


/* point_wallets */
ALTER TABLE "point_wallets"
ADD CONSTRAINT "FK_POINT_WALLETS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");


/* point_transactions */
ALTER TABLE "point_transactions"
ADD CONSTRAINT "FK_POINT_TRANSACTIONS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");


/* families */
ALTER TABLE "families"
ADD CONSTRAINT "FK_FAMILIES_CREATED_BY"
FOREIGN KEY ("created_by")
REFERENCES "users" ("user_id");


/* family_members */
ALTER TABLE "family_members"
ADD CONSTRAINT "FK_FAMILY_MEMBERS_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "family_members"
ADD CONSTRAINT "FK_FAMILY_MEMBERS_USER"
FOREIGN KEY ("user_id")
REFERENCES "users" ("user_id");


/* family_codes */
ALTER TABLE "family_codes"
ADD CONSTRAINT "FK_FAMILY_CODES_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");


/* refresh_tokens */
ALTER TABLE "refresh_tokens"
ADD CONSTRAINT "FK_REFRESH_TOKENS_USER"
FOREIGN KEY ("user_id")
REFERENCES "users" ("user_id");


/* notifications */
ALTER TABLE "notifications"
ADD CONSTRAINT "FK_NOTIFICATIONS_USER"
FOREIGN KEY ("user_id")
REFERENCES "users" ("user_id");


/* device_tokens */
ALTER TABLE "device_tokens"
ADD CONSTRAINT "FK_DEVICE_TOKENS_USER"
FOREIGN KEY ("user_id")
REFERENCES "users" ("user_id");


/* point_cashout_requests */
ALTER TABLE "point_cashout_requests"
ADD CONSTRAINT "FK_POINT_CASHOUT_REQUESTS_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "point_cashout_requests"
ADD CONSTRAINT "FK_POINT_CASHOUT_REQUESTS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");

ALTER TABLE "point_cashout_requests"
ADD CONSTRAINT "FK_POINT_CASHOUT_REQUESTS_PARENT"
FOREIGN KEY ("parent_id")
REFERENCES "users" ("user_id");


/* savings_subscriptions */
ALTER TABLE "savings_subscriptions"
ADD CONSTRAINT "FK_SAVINGS_SUBSCRIPTIONS_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "savings_subscriptions"
ADD CONSTRAINT "FK_SAVINGS_SUBSCRIPTIONS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");

ALTER TABLE "savings_subscriptions"
ADD CONSTRAINT "FK_SAVINGS_SUBSCRIPTIONS_PRODUCT"
FOREIGN KEY ("savings_product_id")
REFERENCES "savings_products" ("savings_product_id");


/* savings_transactions */
ALTER TABLE "savings_transactions"
ADD CONSTRAINT "FK_SAVINGS_TRANSACTIONS_SUBSCRIPTION"
FOREIGN KEY ("savings_subscription_id")
REFERENCES "savings_subscriptions" ("savings_subscription_id");


/* investment_transactions */
ALTER TABLE "investment_transactions"
ADD CONSTRAINT "FK_INVESTMENT_TRANSACTIONS_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "investment_transactions"
ADD CONSTRAINT "FK_INVESTMENT_TRANSACTIONS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");

ALTER TABLE "investment_transactions"
ADD CONSTRAINT "FK_INVESTMENT_TRANSACTIONS_PRODUCT"
FOREIGN KEY ("investment_product_id")
REFERENCES "investment_products" ("investment_product_id");


/* investment_price_history */
ALTER TABLE "investment_price_history"
ADD CONSTRAINT "FK_INVESTMENT_PRICE_HISTORY_PRODUCT"
FOREIGN KEY ("investment_product_id")
REFERENCES "investment_products" ("investment_product_id");

ALTER TABLE "investment_price_history"
ADD CONSTRAINT "FK_INVESTMENT_PRICE_HISTORY_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");


/* investment_holdings */
ALTER TABLE "investment_holdings"
ADD CONSTRAINT "FK_INVESTMENT_HOLDINGS_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "investment_holdings"
ADD CONSTRAINT "FK_INVESTMENT_HOLDINGS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");

ALTER TABLE "investment_holdings"
ADD CONSTRAINT "FK_INVESTMENT_HOLDINGS_PRODUCT"
FOREIGN KEY ("investment_product_id")
REFERENCES "investment_products" ("investment_product_id");


/* investment_news_products */
ALTER TABLE "investment_news_products"
ADD CONSTRAINT "FK_INVESTMENT_NEWS_PRODUCTS_NEWS"
FOREIGN KEY ("investment_news_id")
REFERENCES "investment_news" ("investment_news_id");

ALTER TABLE "investment_news_products"
ADD CONSTRAINT "FK_INVESTMENT_NEWS_PRODUCTS_PRODUCT"
FOREIGN KEY ("investment_product_id")
REFERENCES "investment_products" ("investment_product_id");


/* store_products */
ALTER TABLE "store_products"
ADD CONSTRAINT "FK_STORE_PRODUCTS_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "store_products"
ADD CONSTRAINT "FK_STORE_PRODUCTS_CREATED_BY"
FOREIGN KEY ("created_by")
REFERENCES "users" ("user_id");


/* purchases */
ALTER TABLE "purchases"
ADD CONSTRAINT "FK_PURCHASES_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "purchases"
ADD CONSTRAINT "FK_PURCHASES_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");

ALTER TABLE "purchases"
ADD CONSTRAINT "FK_PURCHASES_STORE_PRODUCT"
FOREIGN KEY ("store_product_id")
REFERENCES "store_products" ("store_product_id");


/* quizzes */
ALTER TABLE "quizzes"
ADD CONSTRAINT "FK_QUIZZES_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "quizzes"
ADD CONSTRAINT "FK_QUIZZES_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");

ALTER TABLE "quizzes"
ADD CONSTRAINT "FK_QUIZZES_WORD"
FOREIGN KEY ("word_id")
REFERENCES "words" ("word_id");


/* quiz_attempts */
ALTER TABLE "quiz_attempts"
ADD CONSTRAINT "FK_QUIZ_ATTEMPTS_QUIZ"
FOREIGN KEY ("quiz_id")
REFERENCES "quizzes" ("quiz_id");

ALTER TABLE "quiz_attempts"
ADD CONSTRAINT "FK_QUIZ_ATTEMPTS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");


/* chores */
ALTER TABLE "chores"
ADD CONSTRAINT "FK_CHORES_FAMILY"
FOREIGN KEY ("family_id")
REFERENCES "families" ("family_id");

ALTER TABLE "chores"
ADD CONSTRAINT "FK_CHORES_PARENT"
FOREIGN KEY ("parent_id")
REFERENCES "users" ("user_id");

ALTER TABLE "chores"
ADD CONSTRAINT "FK_CHORES_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");


/* chore_completion_requests */
ALTER TABLE "chore_completion_requests"
ADD CONSTRAINT "FK_CHORE_COMPLETION_REQUESTS_CHORE"
FOREIGN KEY ("chore_id")
REFERENCES "chores" ("chore_id");

ALTER TABLE "chore_completion_requests"
ADD CONSTRAINT "FK_CHORE_COMPLETION_REQUESTS_CHILD"
FOREIGN KEY ("child_id")
REFERENCES "users" ("user_id");


/* =========================================================
7. UNIQUE / CHECK CONSTRAINT
========================================================= */

/* ---------------------------------------------------------
users

로그인 아이디와 이메일은 살아있는 회원끼리만 유일해야 함.

탈퇴(MYPAGE-05)는 이력 보존 때문에 행을 지우지 않고 deleted_at 만 찍는
소프트 삭제라, 전체 UNIQUE 로 걸면 탈퇴자가 쓰던 이메일·아이디로
다시 가입할 수 없다. 부분 유니크 인덱스로 탈퇴 행을 제외한다.
(중복 검사 쿼리도 같은 기준 — UserRepository 의 *AndDeletedAtIsNull)

위반(23505)은 UserServiceImpl.resolveDuplicateError 가 잡아 409 로 바꾼다.
--------------------------------------------------------- */

CREATE UNIQUE INDEX "UX_USERS_LOGIN_ID_ACTIVE"
ON "users" ("login_id")
WHERE "deleted_at" IS NULL;


CREATE UNIQUE INDEX "UX_USERS_EMAIL_ACTIVE"
ON "users" ("email")
WHERE "deleted_at" IS NULL;


/* ---------------------------------------------------------
linked_accounts

같은 사용자가 같은 계좌번호를 두 번 연동할 수 없음
--------------------------------------------------------- */

ALTER TABLE "linked_accounts"
ADD CONSTRAINT "UQ_LINKED_ACCOUNTS_USER_ACCOUNT"
UNIQUE ("user_id", "account_number");


/* ---------------------------------------------------------
point_transactions

1. 동일 요청 재시도로 인한 이중 충전 방지
2. 포인트 거래 금액은 반드시 양수

멱등키만으로 유일하게 두면, 키는 클라이언트가 정하는 값이라
서로 다른 두 자녀가 우연히 같은 Idempotency-Key 를 보냈을 때
뒤에 온 요청이 남의 거래를 자기 것으로 착각한다.
유일성 범위를 자녀 안으로 좁힌다.

PostgreSQL 은 NULL 을 서로 다른 값으로 보므로,
멱등키 없는 내부 거래는 여러 건 허용된다.
--------------------------------------------------------- */

ALTER TABLE "point_transactions"
ADD CONSTRAINT "UQ_POINT_TRANSACTIONS_CHILD_IDEMPOTENCY_KEY"
UNIQUE ("child_id", "idempotency_key");


ALTER TABLE "point_transactions"
ADD CONSTRAINT "CK_POINT_TRANSACTIONS_AMOUNT_POSITIVE"
CHECK ("amount" > 0);


/* ---------------------------------------------------------
point_wallets

1. 하나의 자녀는 하나의 포인트 지갑만 보유
2. 지갑 잔액은 음수가 될 수 없음

V1.0의 UNIQUE(user_id)는
V1.1에서 컬럼명이 child_id로 변경되었으므로
UNIQUE(child_id)로 적용
--------------------------------------------------------- */

ALTER TABLE "point_wallets"
ADD CONSTRAINT "UQ_POINT_WALLETS_CHILD_ID"
UNIQUE ("child_id");


ALTER TABLE "point_wallets"
ADD CONSTRAINT "CK_POINT_WALLETS_BALANCE_NON_NEGATIVE"
CHECK ("balance" >= 0);


/* ---------------------------------------------------------
family_codes

하나의 가족 초대 코드는 전체 시스템에서 유일해야 함
--------------------------------------------------------- */

ALTER TABLE "family_codes"
ADD CONSTRAINT "UQ_FAMILY_CODES_CODE"
UNIQUE ("code");


/* ---------------------------------------------------------
device_tokens

같은 FCM 토큰이 여러 user_id 에 동시에 남아있으면 안 됨.
기기가 재로그인하면 등록 API 가 이 유니크를 이용해
기존 소유자를 새 user_id 로 갈아치우는 upsert 로 동작한다.
--------------------------------------------------------- */

ALTER TABLE "device_tokens"
ADD CONSTRAINT "UQ_DEVICE_TOKENS_FCM_TOKEN"
UNIQUE ("fcm_token");


/* ---------------------------------------------------------
families

1. 주간 기본 용돈과 주간 획득 한도는 음수가 될 수 없음
   (온보딩 전 기본값 0 은 허용)
2. 금융 이해도는 정해진 세 값 중 하나

financial_knowledge_level 은 네이티브 ENUM 이 아니라 VARCHAR 이므로
DB 가 값을 걸러 주지 않는다. CHECK 로 같은 보호를 준다.
FinancialKnowledgeLevel 열거형에 값을 추가하면 이 CHECK 도 같이 고쳐야 한다.
--------------------------------------------------------- */

ALTER TABLE "families"
ADD CONSTRAINT "CK_FAMILIES_WEEKLY_ALLOWANCE_NON_NEGATIVE"
CHECK ("weekly_allowance" >= 0);


ALTER TABLE "families"
ADD CONSTRAINT "CK_FAMILIES_WEEKLY_REWARD_LIMIT_NON_NEGATIVE"
CHECK ("weekly_reward_limit" >= 0);


ALTER TABLE "families"
ADD CONSTRAINT "CK_FAMILIES_FINANCIAL_KNOWLEDGE_LEVEL"
CHECK ("financial_knowledge_level" IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'));


/* ---------------------------------------------------------
device_tokens

같은 FCM 토큰이 여러 user_id 에 동시에 남아있으면 안 됨.
기기가 재로그인하면 등록 API 가 이 유니크를 이용해
기존 소유자를 새 user_id 로 갈아치우는 upsert 로 동작한다.
--------------------------------------------------------- */

ALTER TABLE "device_tokens"
ADD CONSTRAINT "UQ_DEVICE_TOKENS_FCM_TOKEN"
UNIQUE ("fcm_token");


/* ---------------------------------------------------------
store_products

1. 가치 가중치는 양수 (AI 산정 전 NULL 은 허용)
2. 가격은 양수 (value 산정 전 NULL 은 허용)
--------------------------------------------------------- */

ALTER TABLE "store_products"
ADD CONSTRAINT "CK_STORE_PRODUCTS_VALUE_POSITIVE"
CHECK ("value" IS NULL OR "value" > 0);


ALTER TABLE "store_products"
ADD CONSTRAINT "CK_STORE_PRODUCTS_PRICE_POSITIVE"
CHECK ("price" IS NULL OR "price" > 0);


/* ---------------------------------------------------------
chores

1. 가치 가중치는 양수 (AI 산정 전 NULL 은 허용)
2. 보상 포인트는 양수 (value 산정 전 NULL 은 허용)

상점 상품과 같은 기준이다. 0 을 허용하면 "해도 아무것도 안 주는"
할 일이 만들어진다.
--------------------------------------------------------- */

ALTER TABLE "chores"
ADD CONSTRAINT "CK_CHORES_VALUE_POSITIVE"
CHECK ("value" IS NULL OR "value" > 0);


ALTER TABLE "chores"
ADD CONSTRAINT "CK_CHORES_REWARD_POINTS_POSITIVE"
CHECK ("reward_points" IS NULL OR "reward_points" > 0);


/* =========================================================
8. INDEX
========================================================= */

/* ---------------------------------------------------------
point_transactions

거래내역 커서 페이징이 쓰는 인덱스. 정렬 키와 순서가 같아야 한다.
"이 지점 이후"를 인덱스에서 바로 찾아 limit 만큼 읽고 멈추는 것이
핵심이라, 이 인덱스가 없으면 자녀의 거래를 전부 읽고 정렬한 뒤 잘라낸다.

방향은 ASC 로만 만든다. PostgreSQL 은 인덱스를 역방향으로도
스캔하므로 최신순(DESC) 조회도 같은 인덱스를 쓴다.
--------------------------------------------------------- */

CREATE INDEX "IX_POINT_TRANSACTIONS_CHILD_CREATED_AT"
ON "point_transactions" ("child_id", "created_at", "point_transaction_id");


/* ---------------------------------------------------------
notifications

알림 목록 조회(NOTI-01)의 페이징이 쓰는 인덱스. point_transactions 와 같은 이유로
정렬 키(created_at)와 순서를 맞춰 커서 지점부터 필요한 만큼만 읽고 멈추게 한다.
--------------------------------------------------------- */

CREATE INDEX "IX_NOTIFICATIONS_USER_CREATED_AT"
ON "notifications" ("user_id", "created_at", "notification_id");


/* ---------------------------------------------------------
chores

FK 제약은 인덱스를 만들어 주지 않는다. PostgreSQL 은 참조받는 쪽(users.user_id)에만
인덱스를 요구하고 참조하는 쪽(chores.family_id)은 그냥 둔다. 없으면 아래 조회가
매번 테이블 전체를 훑고 정렬까지 따로 한다.

FAMILY_CHILD: 할 일 목록 조회(CHORE-01)가 쓴다.
경로가 가족과 아이를 지목하므로 (family_id, child_id) 로 좁힌다. 앞머리가 등호
조건이어야 인덱스를 타고, 뒤의 created_at·chore_id 는 목록 정렬
(created_at DESC, chore_id DESC)을 인덱스로 처리해 별도 sort 를 없앤다.

삭제 조건은 컬럼에 넣지 않고 WHERE 절(부분 인덱스)로 둔다. 목록은 살아 있는 것만
보므로 지워진 행은 인덱스에 담을 이유가 없다. 컬럼으로 넣는 것보다 인덱스가 작아지고
조회 조건과 모양이 정확히 같아진다.

FAMILY: 완료 요청 목록(CHORE-08)이 chore_completion_requests 와 chores 를
조인해 family_id 로 좁힐 때 쓴다. 그쪽은 삭제 여부를 보지 않으므로 family_id 만 건다.
지워진 할 일에 걸려 있는 요청도 부모가 처리할 수 있어야 하기 때문이다.
--------------------------------------------------------- */

CREATE INDEX "IX_CHORES_FAMILY_CHILD"
ON "chores" ("family_id", "child_id", "created_at", "chore_id")
WHERE "deleted_at" IS NULL;


CREATE INDEX "IX_CHORES_FAMILY"
ON "chores" ("family_id");


/* ---------------------------------------------------------
chore_completion_requests

FK 는 인덱스를 만들어 주지 않으므로 chore_id 쪽은 비어 있다. 조회가 두 가지다.

1. 주기당 지급 여부(CHORE-09) — chore_id 로 좁히고 status='SUCCEED' 인 것 중
   processed_at 이 이번 주기 시작 이후인지 본다. 세 컬럼이 앞에서부터 그대로 쓰인다.
2. 부모 대기함(CHORE-08) — chores 와 조인해 status='PENDING' 만 고른다.
   (chore_id, status) 까지만 쓰지만 그건 아래 인덱스의 앞머리라 같은 인덱스를 탄다.

그래서 하나로 충분하다. 컬럼 순서는 등호로 좁히는 것(chore_id, status)이 앞이고
범위로 보는 것(processed_at)이 뒤다. 범위 조건을 앞에 두면 그 뒤 컬럼은
인덱스로 걸러지지 않고 읽은 뒤 버려진다.

CHILD_STATUS: 부모 대기함(CHORE-08)이 쓴다. 경로가 아이를 지목하므로 child_id 로
좁히고 status 로 거른 뒤 requested_at 역순으로 정렬한다. 앞의 두 컬럼이 등호라
인덱스를 타고, 뒤의 requested_at·id 는 정렬을 인덱스로 처리해 별도 sort 를 없앤다.
CHORE_STATUS 로는 대신할 수 없다 — 그쪽 앞머리가 chore_id 라서 아이로 좁히는
조회에서는 첫 컬럼부터 어긋난다.
--------------------------------------------------------- */

CREATE INDEX "IX_CHORE_COMPLETION_REQUESTS_CHORE_STATUS"
ON "chore_completion_requests" ("chore_id", "status", "processed_at");


CREATE INDEX "IX_CHORE_COMPLETION_REQUESTS_CHILD_STATUS"
ON "chore_completion_requests" ("child_id", "status", "requested_at", "chore_completion_request_id");


/* ---------------------------------------------------------
chore_completion_requests — 대기 중 요청은 할 일당 하나

한 집안일에 처리 대기 중(PENDING)인 요청이 둘 이상 있을 수 없다는 불변식이다.
부분 유니크 인덱스라 PENDING 행만 유일성을 따지고, 처리가 끝난
SUCCEED·REJECTED·CANCELLED 는 몇 건이든 쌓인다 — 그래야 거절당한 아이가
같은 주기에 다시 요청할 수 있다.

애플리케이션 검사(existsByChoreIdAndStatus)만으로는 못 막는다. 그 검사와 INSERT
사이에 틈이 있어서 아이가 버튼을 연타하면 요청이 여러 건 들어가고, 부모 대기함에
같은 항목이 여러 줄 뜬다. 지급은 멱등키가 한 번으로 막지만 대기함은 지저분해진다.

위반(23505)은 서비스가 잡아 409 로 바꾼다. 그냥 두면 500 이 나간다
(지갑의 멱등키 위반 처리와 같은 방식이다).
--------------------------------------------------------- */

CREATE UNIQUE INDEX "UX_CHORE_COMPLETION_REQUESTS_PENDING"
ON "chore_completion_requests" ("chore_id")
WHERE "status" = 'PENDING';


/* ---------------------------------------------------------
investment_price_history

가족별 상품 가격 추이 조회용. 조회가 가족·상품으로 좁히고
recorded_at 순으로 정렬하므로 그 순서 그대로 건다.
--------------------------------------------------------- */

CREATE INDEX "IX_INVESTMENT_PRICE_HISTORY_FAMILY_PRODUCT_RECORDED_AT"
ON "investment_price_history" ("family_id", "investment_product_id", "recorded_at");


/* ---------------------------------------------------------
investment_holdings — 아이·종목당 보유 행은 하나

보유는 (child_id, investment_product_id)당 한 행을 매수/매도가 갱신하는
구조라는 불변식이다. 애플리케이션 upsert 만으로는 동시 매수(연타)가
같은 종목 행을 두 개 만들 수 있어 DB 가 막는다. 조회(가족+아이로 좁힘)도
이 인덱스의 child_id 를 타므로 별도 조회용 인덱스는 두지 않는다.
--------------------------------------------------------- */

CREATE UNIQUE INDEX "UX_INVESTMENT_HOLDINGS_CHILD_PRODUCT"
ON "investment_holdings" ("child_id", "investment_product_id");
