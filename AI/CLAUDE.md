# CLAUDE.md

## 절대 규칙

**이 AI 서버는 DB 테이블을 하나도 만들지 않는다.** 팀원들이 이미 만들어 둔 테이블을
활용하는 것이 이 서버의 역할이다. 필요한 컬럼이 없으면 직접 추가하지 말고
해당 도메인 담당자와 협의해서 백엔드 엔티티에 선언되게 한다.

자세한 절차와 테이블별 접근 범위는 @docs/db-boundary.md 참고.

See @AGENTS.md for team rules.
See @AGENTS.local.md for my personal notes (if it exists).

mr 메세지까지 작성한 후에는 브랜치와 클로드 세션을 옮길 것이므로, 다음 세션에서 이어서 할 수 있도록 맥락을 이 아래에 간단히 적는다. 기존 맥락은 놔둘 필요 없고, 지난 맥락은 지운다.

## 다음 세션 맥락 (2026-09-10)

- **A·B가 DB에 반영됨.** 요청 이름(`weekly_max_earning`)과 다르게 `families.weekly_reward_limit`
  로 들어왔고(B는 요청대로 `weekly_allowance`), 덤으로 `financial_knowledge_level`
  (아이 금융 이해도, 퀴즈 난이도 참고용)도 추가됐다. `app/domains/pricing/models.py` 의 `Family`
  는 이미 실제 컬럼명으로 매핑되어 있음 — 요청 파라미터로 받는 방식은 필요 없다.
  자세한 내용은 @docs/db-boundary.md 협의 항목 1번.
- **`app/config.py`·`db.py`·`security.py`를 `app/core/`로 옮겼다** (`database.py`로 이름도 바꿈).
  `service.py`도 각 도메인에서 `services.py`로 변경. import 경로가 `app.core.config` 등으로
  바뀌었으니 새 파일 만들 때 옛 경로(`app.db`, `app.security`)를 쓰지 않는다.
- **다음 작업: 상점 물품 가격 책정 로직 구현.** `app/domains/pricing/services.py` 는 아직 계획만
  적힌 빈 파일. `D = (weekly_allowance + weekly_reward_limit) / 7` 과 가중치→포인트 계산을
  **규칙 기반으로 먼저** 구현하고, LLM(`app/llm/client.py`)은 근거 문구 생성에 그 뒤에 붙인다.
  결과는 `store_products.value`(가중치)·`price`(포인트)에 쓴다. 근거(reason)는 v1에서는 저장하지 않고
  응답에만 담는다.
- **협의 대기 1건**: `investment_news_products`에 `impact`/`evidence_grade` 컬럼(투자 담당) — 아직
  DB에 반영 안 됨.
- `docs/ARCHITECTURE.md`의 `users.role` VARCHAR 기술과 실제 네이티브 ENUM 불일치는 백엔드 확인 대기중.
- **Jira 쓰기는 `mcp__claude_ai_Atlassian_Rovo__*`(서진주 계정)로만.**
  `_Rovo_2_`는 팀원 천명석 계정이므로 댓글·상태 변경에 쓰지 않는다.
- **로컬 실행**: 다른 창에서 `ssh -N A505` → `AI` 폴더에서 `.\.venv\Scripts\Activate.ps1`
  → `fastapi dev app/main.py` → http://127.0.0.1:8000/docs
