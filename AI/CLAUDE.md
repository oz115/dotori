# CLAUDE.md

## 절대 규칙

**이 AI 서버는 DB 테이블을 하나도 만들지 않는다.** 팀원들이 이미 만들어 둔 테이블을
활용하는 것이 이 서버의 역할이다. 필요한 컬럼이 없으면 직접 추가하지 말고
해당 도메인 담당자와 협의해서 백엔드 엔티티에 선언되게 한다.

자세한 절차와 테이블별 접근 범위는 @docs/db-boundary.md 참고.

See @AGENTS.md for team rules.
See @AGENTS.local.md for my personal notes (if it exists).

mr 메세지까지 작성한 후에는 브랜치와 클로드 세션을 옮길 것이므로, 다음 세션에서 이어서 할 수 있도록 맥락을 이 아래에 간단히 적는다. 기존 맥락은 놔둘 필요 없고, 지난 맥락은 지운다.

## 다음 세션 맥락 (2026-09-08)

- **82번(파이썬 서버 환경 구축) 완료, MR 올리는 중.** MR이 merge되면 Jira 82를 완료로 옮긴다.
- **다음 작업: 상점 물품 가격 책정.** A·B(`families.weekly_max_earning`, `weekly_allowance`)는
  아직 DB에 없고 요청만 해둔 상태다. 그러니 A·B를 요청 파라미터로 받아
  `D = (A + B) / 7`과 가중치→포인트 계산을 **규칙 기반으로 먼저** 구현하고, LLM은 그 뒤에 붙인다.
  결과는 `store_products.value`(가중치)·`price`(포인트)에 쓴다. 근거(reason)는 v1에서는 저장하지 않고
  응답에만 담는다 (자세한 내용은 @docs/db-boundary.md).
- **협의 대기 2건**: `investment_news_products`에 `impact`/`evidence_grade` 컬럼(투자 담당),
  `docs/ARCHITECTURE.md`의 `users.role` VARCHAR 기술과 실제 네이티브 ENUM 불일치(백엔드 확인).
- **Jira 쓰기는 `mcp__claude_ai_Atlassian_Rovo__*`(서진주 계정)로만.**
  `_Rovo_2_`는 팀원 천명석 계정이므로 댓글·상태 변경에 쓰지 않는다.
- **로컬 실행**: 다른 창에서 `ssh -N A505` → `AI` 폴더에서 `.\.venv\Scripts\Activate.ps1`
  → `fastapi dev app/main.py` → http://127.0.0.1:8000/docs
