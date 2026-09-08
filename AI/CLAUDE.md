# CLAUDE.md

## 절대 규칙

**이 AI 서버는 DB 테이블을 하나도 만들지 않는다.** 팀원들이 이미 만들어 둔 테이블을
활용하는 것이 이 서버의 역할이다. 필요한 컬럼이 없으면 직접 추가하지 말고
해당 도메인 담당자와 협의해서 백엔드 엔티티에 선언되게 한다.

자세한 절차와 테이블별 접근 범위는 @docs/db-boundary.md 참고.

See @AGENTS.md for team rules.
See @AGENTS.local.md for my personal notes (if it exists).
