# frontend

React Native (Expo) 앱. [Expo Router](https://docs.expo.dev/router/introduction/) +
Feature-Sliced Design(FSD)-lite 구조를 사용합니다.

## 시작하기

```bash
cp .env.example .env   # EXPO_PUBLIC_API_URL 등 값 채우기
npm install
npm start
```

## 폴더 구조

정석 FSD(`app/pages/widgets/features/entities/shared` 6레이어)를 다 따라가지 않고,
우리 서비스 규모에 맞게 4레이어로 줄인 **FSD-lite**를 씁니다.

```
src/
  app/        # expo-router 라우트. (auth)/(child)/(parent) 같은 라우트 그룹으로 화면 묶음
  widgets/    # 여러 feature를 조합한 큰 UI 블록
  features/   # 사용자 행동/기능 단위. 자기 상태(model)까지 함께 소유
  shared/     # 레이어와 무관한 공용 코드 (api, ui, hooks, lib, constants, types, theme)
```

`pages`, `entities` 레이어는 두지 않습니다.

- **pages 없음**: expo-router 라우트 파일 자체가 화면이라 별도 레이어가 중복입니다.
  라우트 파일은 배치만 담당하고, 로직은 widgets/features로 내려보내세요.
- **entities 없음**: 도메인 모델을 여러 feature가 공유할 정도로 커지기 전까지는,
  각 feature가 자신이 다루는 상태(`model/`)를 직접 소유합니다. 나중에 2개 이상의
  feature가 같은 모델(예: user)을 공유하게 되면 그때 entities를 분리해도 됩니다.

**레이어 의존 규칙**: `app → widgets → features → shared` 방향으로만 import 합니다.
각 슬라이스는 `index.ts`(public API)를 통해서만 외부에 노출하고, 다른 슬라이스는
내부 파일을 직접 import하지 않습니다.

### 지금 실제로 있는 폴더 (예시)

`src/features/counter`, `src/widgets/counter-widget` 는 레이어별 패턴을 보여주기 위한
예시 슬라이스입니다. 실제 도메인 기능을 만들면서 지우고 교체하세요.
(`npm run fsd-lint` 실행 시 "참조가 하나뿐이니 합쳐라"라는 경고가 뜨는데, 예시라서
재사용처가 하나뿐이라 생기는 정상적인 경고입니다. 실제 기능으로 교체되면 사라집니다.)

### 앞으로 생길 폴더 (참고용 — 지금 다 만들어두지 않음)

아래는 지금 논의 중인 실제 도메인 기준 목록입니다. 필요할 때마다 그때그때 만드세요.

```
app/
  (auth)/     # 로그인/회원가입 등 인증 플로우
  (child)/    # 자녀 계정 화면
  (parent)/   # 부모 계정 화면

features/
  auth/ family/ account/ wallet/ task/ store/ savings/ investment/ quiz/ report/

widgets/
  child-home/ parent-home/ asset-summary/ goal-progress/

shared/
  constants/ types/ theme/   # 지금은 비어있음. 실제 값이 생기면 추가
```

## 스택

- **라우팅**: Expo Router (파일 기반)
- **상태 관리**: Zustand (클라이언트 상태) + TanStack Query (서버 상태)
- **스타일링**: React Native `StyleSheet`
- **HTTP**: axios (`shared/api/httpClient`)

## 테스트

[jest-expo](https://docs.expo.dev/develop/unit-testing/) + [React Native Testing Library](https://callstack.github.io/react-native-testing-library/)를 씁니다.

- 테스트 파일은 `*.test.ts`/`*.test.tsx`로 소스 파일 바로 옆에 둡니다 (예: `store.ts` 옆에 `store.test.ts`).
- 상태(zustand store)는 `useXStore.getState()`로 직접, 컴포넌트는 `render`/`screen`/`fireEvent`로 테스트합니다.
- RNTL v14부터 `render`가 비동기라 `await render(...)`로 호출해야 합니다.

```bash
npm test              # 전체 테스트 1회 실행
npm run test:watch    # 변경 감지 후 재실행
npm run test:coverage # 커버리지 리포트 포함
```

## 스크립트

| 명령                | 설명                                                                        |
| ------------------- | --------------------------------------------------------------------------- |
| `npm start`         | 개발 서버 실행                                                              |
| `npm run lint`      | ESLint (expo-config 기반)                                                   |
| `npm run fsd-lint`  | [steiger](https://github.com/feature-sliced/steiger)로 FSD 레이어 규칙 검사 |
| `npm run typecheck` | `tsc --noEmit`                                                              |
| `npm test`          | Jest 테스트 실행                                                            |
| `npm run format`    | Prettier로 전체 포맷                                                        |

커밋/푸시 전 검사는 지금은 자동화하지 않고 위 명령을 직접 실행합니다. (`npm run lint && npm run typecheck && npm test`)
