# AGENTS.md

이 저장소에서 작업하는 에이전트(Claude Code 등)를 위한 최상위 지침 문서입니다.
프론트엔드/백엔드별 세부 지침은 각 디렉토리의 `AGENTS.md`를 따르세요.

## 프로젝트 구조

```
project/
├── AGENTS.md
├── CLAUDE.md               ← See @AGENTS.md
├── frontend/
│   ├── AGENTS.md
│   └── CLAUDE.md            ← See @AGENTS.md
├── backend/
│   ├── AGENTS.md
│   └── CLAUDE.md            ← See @AGENTS.md
├── docs/
│   ├── Idea.md            ← 기획 원문 (프로젝트 개요, 핵심가치, MVP 범위)
│   ├── ARCHITECTURE.md     ← 시스템 아키텍처, 인프라 구조, DB ERD 요약, API 설계 원칙
│   ├── DESIGN.md           ← 디자인 시스템 (컬러 팔레트, 마스코트 가이드, 타이포)
│   ├── TODO.md             ← 현재 작업 목록 (세션 시작 시 자동 갱신)
│   ├── DECISIONS.md        ← 주요 기술/설계 결정과 이유 (ADR 스타일)
│   ├── API.md              ← 엔드포인트 명세 (요청/응답 예시)
│   └── CONVENTIONS.md      ← 커밋/브랜치/네이밍 컨벤션
├── docker-compose.local.yml
└── .env.example
```

## 세션 시작 시 해야 할 일

1. `docs/TODO.md`를 읽고 진행 중/다음 작업을 파악한다.
2. 작업이 완료되면 `docs/TODO.md`를 최신 상태로 갱신한다.
3. 새로운 기술/설계 결정이 있으면 `docs/DECISIONS.md`에 기록한다.

## 문서 우선순위

- 기획/범위가 궁금하면 → `docs/Idea.md`
- 구조/인프라/DB가 궁금하면 → `docs/ARCHITECTURE.md`
- 커밋/브랜치/네이밍 규칙이 궁금하면 → `docs/CONVENTIONS.md`
- API 스펙이 궁금하면 → `docs/API.md`

## 공통 규칙

- 커밋/브랜치/네이밍 컨벤션은 `docs/CONVENTIONS.md`를 따른다.
- 백엔드 작업은 `backend/AGENTS.md`, 프론트엔드 작업은 `frontend/AGENTS.md`를 우선 확인한다.
- 비밀값(.env 등)은 커밋하지 않는다. `.env.example`만 갱신한다.
