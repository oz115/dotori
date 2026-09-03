# AGENTS.md

프로젝트 개요·기획은 docs/Idea.md, 아키텍처는 docs/ARCHITECTURE.md 참고.
하위 폴더 규칙은 frontend/AGENTS.md, backend/AGENTS.md 우선 적용.

## Jira 워크플로우

- 이슈 관리: Jira 사용 (별도 TODO.md 파일 관리 안 함)
- 하위작업(Subtask) 생성은 Jira UI에서 직접 (Story 밑에 Subtask 단위로 생성)
- Subtask 제목은 한국어 명사구로 작성 (예: `포인트 충전 API`, `포인트 차감 내부 서비스`)
  - 테스트 하위작업은 대상을 앞에 두고 `... 예외/동시성 통합 테스트` 형태로 맞춘다
- Jira MCP는 이슈 조회·상태 변경·댓글에 사용 (하위작업/브랜치 생성은 위임하지 않음)
  - 예외: Notion에 정리된 API 명세를 하위작업으로 옮기는 반복 작업은 MCP로 자동화 가능

## 브랜치 규칙

- 브랜치명: {type}/{이슈키} (예: feat/S15P21A505-6)
- type: feat, fix, refactor, docs, test, chore
- 브랜치명만으로 작업 내용 파악 불가 → 작업 전 Jira MCP로 해당 이슈 조회 먼저

## 커밋 메시지

형식: {type}: {설명} ({이슈키})
예시: feat: 회원가입 API 구현 (S15P21A505-6)

## 경계 (Boundaries)

- 항상 가능: 기존 코드 스타일 따라 기능 추가, 테스트 작성
- 먼저 물어보기: DB 스키마 변경, 새 외부 라이브러리 추가
- 절대 금지: .env / 시크릿 파일 수정, main 브랜치 직접 push

## 유지보수

- 이 파일이 실제와 달라지면 그 자리에서 바로 수정
- 프로젝트 트리 전체를 여기 박제하지 않는다 (docs/ARCHITECTURE.md 참고)
- 코드에 이미 일관되게 반영된 패턴(예: 패키지 구조)은 굳이 여기 중복해서 안 남긴다
- 너가 기능 완성될 때마다 자동으로 커밋까진 괜찮은데 푸시는 하지마
