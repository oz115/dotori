# CLAUDE.md

## 절대 규칙

**이 AI 서버는 DB 테이블을 하나도 만들지 않는다.** 팀원들이 이미 만들어 둔 테이블을
활용하는 것이 이 서버의 역할이다. 필요한 컬럼이 없으면 직접 추가하지 말고
해당 도메인 담당자와 협의해서 백엔드 엔티티에 선언되게 한다.

자세한 절차와 테이블별 접근 범위는 @docs/db-boundary.md 참고.

See @AGENTS.md for team rules.
See @AGENTS.local.md for my personal notes (if it exists).

mr 메세지까지 작성한 후에는 브랜치와 클로드 세션을 옮길 것이므로, 다음 세션에서 이어서 할 수 있도록 맥락을 이 아래에 간단히 적는다. 기존 맥락은 놔둘 필요 없고, 지난 맥락은 지운다.

## 다음 세션 맥락 (2026-09-12)

**브랜치 `feat/S15P21A505-149`.** 상점(shopping)에 이어 할 일(work) 보상 책정까지 구현했다.
149 브랜치에서 둘 다 작업 중 — 189(할 일)로 안 옮기고, 149 에 상점 공통 코드가 있어 여기서
이어감(리뷰어가 아직 149 를 못 봤다고 해서). **아직 커밋/푸시 안 함** (직전 커밋은 로그 설정
`53b0e2e`, 그 위 미커밋: work 도메인 + messaging 다중구독 수정 + 이 CLAUDE.md).

### 구조 (상점·할일 공통 패턴)
도메인마다: `router`(복구용 HTTP) / `consumer`(큐 트리거) / `services`(계산·흐름) /
`repository`(SQL) / `prompt`(조립) / `prompts/`(문구·few-shot) / `schemas` / `models`.
공용: `app/llm/{client,response}` (호출·응답처리), `app/messaging/consumer`(큐 배관),
`app/core/{points,models,config,logging_setup,database,security}`.

### 상점 (shopping) — 완료
- 트리거: `store.product.exchange` 컨슈머. `price = value × 하루소득((A+B)/7)`.
- MVP 검증 다 됨. **실제 UPDATE 경로만 미검증** (store_products 0행).

### 할 일 (work) — 이번에 구현
- 트리거: `chore.exchange` / `chore.reward-evaluation` 컨슈머.
  `reward_points = value × weekly_allowance` (하루소득 아님. 노션·javadoc 일치).
- **난이도 밴드 clamp**: EASY 0.05~0.2 / NORMAL 0.2~0.4 / HARD 0.4~5.0. difficulty 는
  부모 확정 입력이라 밴드가 정답 범위 — 상점처럼 버리지 않고 **경계로 clamp**. HARD 상한 5.0
  은 폭주 방어(부모가 큰 보상 주는 건 허용, value=50 오작동만 막음).
- few-shot 9개 (EASY 4/NORMAL 3/HARD 2). 근거는 설거지 0.1·구몬 10장 0.3 정도, 나머지 잠정값.
- 실호출 확인: 이불정리 EASY→0.1, 영어단어 NORMAL→0.3, 한달일기 HARD→0.6.

### 이번에 고친 것 (상점에도 영향)
- `messaging/consumer` 가 컨슈머 하나 전제 싱글턴이었다 → work 붙이니 조기반환으로 안 뜸.
  구독마다 스레드 갖도록 리스트로 바꿈(공유 종료신호). 로컬 브로커로 상점·할일 동시 기동 확인.

### 다음 작업 후보 / 미결
- **실제 UPDATE 경로 검증**: store_products·chores 둘 다 0행. 백엔드 bootRun + 부모 로그인
  (`parent` 계정, 비번은 사용자만 앎) + 상품/할일 등록으로 전 구간 확인 필요.
- **few-shot 앵커 대부분 잠정값** — 상점·할일 둘 다. 출시 전 기획 검토.
- **폴백 없음**: LLM 실패 시 value 가 NULL 로 남음. 운영 폴백 필요 여부 미결.
- **updated_at 안 씀**: 합의 범위가 value·(price|reward_points)뿐. 트리거 없어 갱신 안 되는 점
  상점 담당자와 정리.
- **커밋 예정 2개**: (1) work 도메인 + messaging 수정 (feat), (2) 이 CLAUDE.md (docs).
  푸시 전 `git merge origin/dev` 필수. MR 은 기존 149 것에 커밋 얹힘.
- **리뷰어 후속(내 범위 밖)**: pytest 가 requirements.txt 에 있음(배포 이미지 섞임),
  investment 가 app/llm/response 재사용하면 중복 사라짐.
- **db-boundary.md 갱신 필요**: chores 를 "읽기+reward_points 쓰기" 로만 적었는데 value 도 쓴다.

### 협의/불일치 대기
- `investment_news_products` impact/evidence_grade — impact 는 investment_news 로, evidence_grade
  철회 (@docs/db-boundary.md 협의 2).
- `docs/ARCHITECTURE.md` users.role VARCHAR vs 네이티브 ENUM — 백엔드 확인 대기.

### 규칙
- **Jira 쓰기는 `mcp__claude_ai_Atlassian_Rovo__*`(서진주)만.** `_Rovo_2_` 는 천명석.
- **로컬 실행**: `ssh -N A505` → `AI` 에서 `.\.venv\Scripts\Activate.ps1`
  → `fastapi dev app/main.py`. 브로커 `docker compose -f ../docker-compose.local.yml up -d`.
  공유 큐 안 가로채려면 `AI/.env` 에 `STORE_PRICING_CONSUMER_ENABLED=false`.
