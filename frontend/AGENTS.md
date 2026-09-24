# AGENTS.md (frontend)

React Native·Expo 프론트엔드(`frontend/`) 작업 지침입니다.
저장소 공통 규칙은 [../AGENTS.md](../AGENTS.md)를 우선 적용합니다.
구조·컨벤션의 배경 설명은 [README.md](./README.md)에 더 자세히 있습니다.

## 프로젝트 개요

- 대상 플랫폼: Android 우선 (Expo, iOS/web도 빌드 가능)
- 사용자 역할: 부모(PARENT), 아이(CHILD, 초등 4~6학년)
- 주요 기술:
  - React Native (Expo 57), React 19
  - Expo Router (파일 기반 라우팅, `typedRoutes` + `reactCompiler` 실험 기능 사용)
  - TypeScript (`strict`), 경로 별칭 `@/* → src/*`, `@/assets/* → assets/*`
  - TanStack Query(서버 상태) + Zustand(클라이언트 상태)
  - Axios (`src/shared/api/httpClient`)
  - Jest (jest-expo) / React Native Testing Library
  - 스타일링: React Native `StyleSheet` (별도 UI 라이브러리 없음)
  - FSD 레이어 검사: steiger (`npm run fsd-lint`)

서비스 기획은 [../docs/Idea.md](../docs/Idea.md), 디자인 기준은 `../docs/DESIGN.md`(작성되면)를 참고합니다.

## 자주 쓰는 명령어

```bash
cp .env.example .env   # EXPO_PUBLIC_API_URL 등 값 채우기 (최초 1회)
npm install
npm start              # 개발 서버

npm run lint           # ESLint (eslint-config-expo + no-restricted-imports)
npm run fsd-lint       # steiger — FSD 레이어 규칙(import 방향, public API) 검사
npm run typecheck      # tsc --noEmit
npm test               # Jest
npm run format         # Prettier 전체 포맷 / format:check 는 검사만
```

## 디렉터리 구조 (FSD-lite)

정석 FSD 6레이어가 아니라 4레이어로 줄인 FSD-lite를 씁니다.

- `src/app`: Expo Router 라우트 = 화면. 배치와 조합만 담당한다.
- `src/widgets`: 여러 feature를 조합하는 화면 배치용 큰 UI 블록.
- `src/features`: 사용자 행동/기능 단위. 자기 상태(`model/`)까지 함께 소유한다.
- `src/shared`: 레이어 무관 공용 코드 (api, ui, config, lib; 필요해지면 hooks/constants/types/theme).

의존성은 `app → widgets → features → shared` 한 방향만 허용합니다. 하위 레이어가 상위를 import하면 안 됩니다.

- `pages`, `entities` 레이어는 **두지 않는다.**
  - pages 없음: 라우트 파일 자체가 화면이라 중복. 라우트 파일은 배치만, 로직은 widgets/features로.
  - entities 없음: 2개 이상 feature가 같은 도메인 모델을 공유하기 전까지 각 feature가 `model/`을 직접 소유. 그때 가서 분리한다.
- `src/features/counter`, `src/widgets/counter-widget`은 레이어 패턴 예시다. 실제 도메인 기능을 만들면서 지우고 교체한다. (교체 전까지 `fsd-lint`의 "참조가 하나뿐" 경고는 정상)

### 앞으로 생길 폴더 (미리 다 만들지 않는다)

```
app/       (auth)/ (child)/ (parent)/
features/  auth/ family/ account/ wallet/ task/ store/ savings/ investment/ quiz/ report/
widgets/   child-home/ parent-home/ asset-summary/ goal-progress/
```

## 모듈 작성 규칙

- 각 feature와 widget은 외부에 공개할 항목을 `index.ts`(public API)로 노출한다.
- 다른 슬라이스의 내부 파일을 직접 import하지 않는다. 항상 배럴(`@/features/x`, `@/shared/ui`)을 통한다. (같은 슬라이스 내부는 예외)
- 같은 디렉터리 안에서는 `./` 상대경로를 쓸 수 있다.
- 상위 디렉터리로 올라가는 `../` import는 쓰지 않는다 (ESLint 에러). 프로젝트 내부 경로는 `@/` 별칭.
- 재사용되지 않는 코드를 성급하게 `shared`로 올리지 않는다.
- 각 공개 모듈 상단에 "이 조각이 무엇을 캡슐화하는지" 한 문단 주석을 다는 기존 관행을 따른다.

## 라우팅

- Expo Router의 파일 기반 라우팅을 사용한다.
- 라우트 파일은 화면 배치와 조합에 집중한다. 비즈니스 로직과 복잡한 상태는 feature/widget으로 분리한다.
- 인증·부모·아이 화면은 각각 `(auth)`, `(parent)`, `(child)` 라우트 그룹으로 구분한다.
- 역할별 접근 권한을 고려해 화면 진입을 제어한다.

## 상태 관리

- 서버에서 가져오는 데이터는 TanStack Query로 관리한다. 서버 응답을 Zustand에 중복 저장하지 않는다.
- UI 상태와 로컬 클라이언트 상태는 Zustand 또는 컴포넌트 상태로 관리한다.
- Query Key는 기능별로 일관되게 관리한다.
- mutation 성공 후 관련 query의 갱신 또는 무효화를 처리한다.
- 전역 Zustand 스토어는 모듈 싱글턴이다. 테스트에서 `beforeEach`로 `setState` 초기화한다.

## 금융 화면 구현 원칙

금융 앱이므로 잔액·거래·자산을 다루는 화면은 다음을 지킨다.

- 서버 금융 데이터는 TanStack Query를 단일 원본으로 사용한다. 잔액·거래내역·자산 정보를 Zustand에 복제하지 않는다.
- 금융성 mutation은 서버 성공 확인 전 화면에 확정 반영하지 않는다 (낙관적 업데이트 금지).
- 거래 처리 중에는 중복 제출을 막고 명확한 진행 상태를 표시한다.
- 거래 실패와, 거래 결과를 확인할 수 없는 상태(타임아웃·네트워크 끊김)를 구분해 처리한다.
- mutation 후 영향을 받는 모든 Query를 함께 갱신·무효화한다.
- 사용자·가족·자녀 전환 시 이전 사용자의 금융정보가 남아 노출되지 않게 한다 (관련 Query 캐시 제거).
- 금액 입력과 원↔포인트 변환은 공통 유틸리티를 사용한다.
- 거래 화면은 로딩·빈 상태·오류·권한 없음·성공 상태를 모두 구현한다.
- 렌더링 최적화는 목록·차트 등 측정 가능한 병목에만 적용한다.
- 금융정보와 인증정보(토큰·잔액·계좌번호 등)를 로그에 출력하지 않는다.

## API 통신

- API 요청은 `src/shared/api/httpClient`(공용 axios 인스턴스)를 통해 수행한다. 컴포넌트에서 axios를 직접 호출하거나 새 인스턴스를 만들지 않는다.
- 인증 토큰 등 공통 헤더/인터셉터는 `httpClient.ts`에 추가한다.
- 요청·응답 타입을 명시한다.
- 인증 실패, 네트워크 오류, 서버 오류를 사용자에게 구분 가능한 형태로 처리한다.
- 환경 변수는 `src/shared/config/env.ts`만 거쳐서 읽는다. 다른 곳에서 `process.env`를 직접 읽지 않는다.
- 클라이언트 번들에 노출되는 값은 `EXPO_PUBLIC_` 접두사만. API URL 등 환경별 값은 환경변수로 관리한다.
- `.env`(gitignore 대상)와 비밀값은 커밋하지 않는다. 예시가 바뀌면 `.env.example`을 갱신한다.
- API 명세는 노션(작업 > API 명세)이 유일한 원본이다. `../docs/API.md`는 갱신하지 않는다.

## UI 작성 원칙

- 공통 컴포넌트가 있으면 `src/shared/ui`의 컴포넌트를 우선 사용한다.
- 색상·타이포그래피·간격은 디자인 토큰(`shared/theme`, 생기면)을 통해 사용한다. 임의의 색상·크기를 반복 하드코딩하지 않는다.
- 스타일은 파일 하단 `StyleSheet.create`로, 다크 모드는 `useColorScheme() === 'dark'` 분기로 처리한다.
- 초등학생 사용자를 고려해 터치 영역, 글자 크기, 피드백을 명확하게 구성한다.
- 부모와 아이 화면의 역할과 행동이 혼동되지 않도록 구분한다.
- 로딩, 빈 화면, 오류 상태를 함께 구현한다.
- 전역 Provider는 `src/shared/lib/providers/AppProviders.tsx` 안에서만 확장한다. `app/_layout.tsx`는 라우팅과 `<AppProviders>` 조립만.

## 테스트

- jest-expo + React Native Testing Library(RNTL v14).
- 테스트 파일은 대상 코드 **바로 옆**에 `*.test.ts` / `*.test.tsx`로 둔다 (`store.ts` ↔ `store.test.ts`).
  - **예외: `src/app/` 아래엔 테스트 파일 금지.** expo-router가 `.test.tsx`도 라우트로 스캔해 테스트 라이브러리가 앱 번들에 섞여 빌드가 깨진다. 화면 로직은 `features/*/lib` 등으로 빼서 테스트한다.
- 컴포넌트 테스트는 `render` / `screen` / `fireEvent`. RNTL v14는 `render`가 비동기이므로 `await render(...)`.
- Zustand 상태는 `useXStore.getState()`로 직접 검증한다.
- 구현 세부사항보다 사용자의 행동과 화면 결과를 검증한다.
- 기능 변경 시 정상 흐름과 주요 오류 흐름을 함께 테스트한다.
- 커버리지 수집 대상에서 `src/app/**`는 제외된다 (라우트 파일은 배치만 하므로).

## 작업 완료 전 검증

커밋/푸시 전 자동화(husky 등)는 두지 않는다. 변경 범위에 맞춰 다음 명령을 직접 실행하고 통과시킨다.

```bash
npm run lint
npm run fsd-lint
npm run typecheck
npm test
npm run format:check
```

## 규칙

- 네이밍/브랜치/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- FSD 레이어/구조를 바꾸면 [README.md](./README.md)를 함께 갱신한다.
- 개인 메모가 필요하면 `AGENTS.local.md`를 만든다 (`CLAUDE.md`가 참조, gitignore 확인).
