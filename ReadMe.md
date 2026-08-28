# 🌿 Git 협업 규칙

## 1. 브랜치 전략

우리 프로젝트는 다음과 같이 브랜치를 운영합니다.

```text
master
  ↑
 dev
  ↑
feature / fix / refactor / docs / chore
```

### `master`

* **배포용 브랜치**
* 실제 운영 환경에 배포할 코드만 존재합니다.
* 누구도 직접 push하지 않습니다.
* `dev` 브랜치에서 충분히 검증된 코드만 MR을 통해 merge합니다.

### `dev`

* **개발 통합 브랜치**
* 각 기능 브랜치에서 개발한 내용을 모으는 브랜치입니다.
* 기능 개발이 완료되면 `feature`, `fix` 등의 브랜치에서 `dev`로 MR을 생성합니다.
* 여러 기능이 통합된 뒤 테스트와 QA를 진행합니다.
* 배포 가능한 상태가 되면 `dev`에서 `master`로 MR을 생성합니다.

### 작업 브랜치

* 실제 기능 개발과 수정 작업을 진행하는 브랜치입니다.
* 항상 최신 `dev` 브랜치를 기준으로 생성합니다.
* 작업 완료 후 `dev` 브랜치로 MR을 생성합니다.

---

## 2. 브랜치 이름 규칙

```text
<타입>/<이름이니셜>/<작업내용>
```

예시:

```text
feature/kts/login
fix/kts/login-error
refactor/kts/api-service
docs/kts/readme
chore/kts/docker-setup
```

* 이니셜은 본인 영문 이니셜을 사용합니다.

  * 예: 김태수 → `kts`
* 작업 내용은 **영어 소문자와 하이픈(-)** 으로 작성합니다.
* 하나의 브랜치에는 하나의 기능 또는 하나의 목적만 포함합니다.

| 타입          | 용도             | 예시                         |
| ----------- | -------------- | -------------------------- |
| `feature/`  | 새로운 기능 개발      | `feature/kts/login`        |
| `fix/`      | 버그 수정          | `fix/kts/login-error`      |
| `refactor/` | 기능 변화 없는 코드 개선 | `refactor/kts/api-service` |
| `docs/`     | README, 문서 수정  | `docs/kts/readme`          |
| `chore/`    | 빌드, 설정, 환경 구성  | `chore/kts/docker-setup`   |

---

## 3. 작업 흐름

모든 기능 개발은 최신 `dev` 브랜치에서 시작합니다.

```bash
# 1. dev 브랜치로 이동
git switch dev

# 2. 최신 dev 반영
git pull origin dev

# 3. 작업 브랜치 생성
git switch -c feature/kts/login

# 4. 작업 후 변경사항 확인
git status

# 5. 커밋
git add .
git commit -m "feat(login): 로그인 기능 구현"

# 6. 원격 브랜치에 push
git push -u origin feature/kts/login
```

이후 GitLab에서 다음 방향으로 MR을 생성합니다.

```text
feature/kts/login
        ↓
       dev
```

팀원 리뷰 후 `dev` 브랜치에 merge합니다.

merge가 완료된 작업 브랜치는 삭제합니다.

다음 기능을 개발할 때는 다시 최신 `dev`를 기준으로 새로운 브랜치를 생성합니다.

```bash
git switch dev
git pull origin dev
git switch -c feature/kts/signup
```

---

## 4. 배포 흐름

기능 개발이 완료되고 `dev` 브랜치에서 테스트와 QA가 끝나면 배포를 위해 `master`로 merge합니다.

```text
feature/login ─┐
feature/signup ├──→ dev ──→ master ──→ 배포
fix/api-error ─┘
```

배포 순서는 다음과 같습니다.

1. 각 기능 브랜치에서 개발
2. 기능 브랜치 → `dev` MR 생성
3. 코드 리뷰 후 `dev`에 merge
4. `dev`에서 통합 테스트 및 QA
5. 배포 가능한 상태가 되면 `dev` → `master` MR 생성
6. 최종 확인 후 `master`에 merge
7. `master` 기준으로 배포

`master`와 `dev` 모두 직접 push하지 않고 MR을 통해서만 변경합니다.

---

## 5. 커밋 메시지 규칙

커밋 메시지는 다음 형식을 사용합니다.

```text
<타입>(<스코프>): <제목>
```

예시:

```text
feat(login): 로그인 기능 구현
fix(auth): 토큰 재발급 오류 수정
refactor(api): API 응답 구조 정리
docs(readme): Git 협업 규칙 추가
chore(docker): Docker 환경 설정
```

| 타입         | 용도                |
| ---------- | ----------------- |
| `feat`     | 새로운 기능 추가         |
| `fix`      | 버그 수정             |
| `refactor` | 기능 변화 없는 코드 개선    |
| `style`    | 코드 포맷팅, 세미콜론 등    |
| `docs`     | 문서 수정             |
| `test`     | 테스트 코드 추가 또는 수정   |
| `chore`    | 빌드, 환경 설정 등 기타 작업 |

### 스코프

스코프에는 작업한 기능이나 영역을 작성합니다.

예시:

```text
login
signup
family
account
saving
investment
quiz
api
auth
infra
```

스코프가 명확하지 않은 경우 생략할 수 있습니다.

```text
docs: README 수정
chore: 프로젝트 초기 설정
```

### 커밋 작성 규칙

* 제목은 한글로 작성해도 됩니다.
* 무엇을 변경했는지 명확하게 작성합니다.
* 커밋 메시지에 작성자 이름을 작성하지 않습니다.
* 하나의 커밋에는 가능한 한 하나의 목적만 담습니다.

---

## 6. Merge Request 규칙

### 기능 개발 MR

```text
feature / fix / refactor
        ↓
       dev
```

예시:

```text
feature/kts/login → dev
```

### 배포 MR

```text
dev
 ↓
master
```

### MR 제목

커밋 메시지와 동일한 형식을 사용합니다.

```text
feat(login): 로그인 기능 구현
fix(account): 계좌 조회 오류 수정
```

### MR 설명

MR에는 다음 내용을 간단히 작성합니다.

```text
## 작업 내용
- 로그인 API 구현
- JWT Access Token 발급 처리

## 변경 이유
- 사용자 인증 기능 구현을 위해 추가

## 확인 사항
- 로그인 성공/실패 테스트 완료
```

### MR 규칙

* 본인의 MR은 본인이 merge하지 않습니다.
* 팀원 1명 이상 리뷰 후 merge합니다.
* 충돌이 발생하면 본인의 작업 브랜치에서 해결합니다.
* 하나의 MR에 서로 다른 여러 기능을 넣지 않습니다.

---

## 7. Conflict 해결

MR 과정에서 `dev`와 충돌이 발생하면 작업 브랜치에서 최신 `dev`를 받아 해결합니다.

```bash
# 현재 작업 브랜치에서
git fetch origin

git merge origin/dev
```

충돌을 해결한 뒤:

```bash
git add .
git commit
git push
```

MR에 변경 내용이 자동으로 반영됩니다.

---

## 8. 브랜치 보호 규칙

### `master`

* 직접 push 금지
* MR을 통해서만 merge
* 배포 가능한 코드만 유지

### `dev`

* 직접 push 금지
* 기능 브랜치에서 MR을 통해서만 merge
* 여러 기능이 통합되는 개발 기준 브랜치

---

## 9. 금지 사항 🚫

* `master` 직접 push ❌
* `dev` 직접 push ❌
* 리뷰 없이 merge ❌
* 하나의 브랜치에서 여러 기능 개발 ❌
* 하나의 MR에 서로 관련 없는 기능 포함 ❌
* 다른 사람의 작업 브랜치에 임의로 push ❌

---

## 10. 시작 전 체크리스트 ✅

* [ ] `git config user.name` 확인
* [ ] `git config user.email` 확인
* [ ] GitLab 계정 정보와 Git 설정 확인
* [ ] `master`, `dev` 보호 브랜치 설정
* [ ] 팀 공통 스코프 목록 확정

확인 방법:

```bash
git config user.name
git config user.email
```

---

## 11. 전체 Git Flow

```text
            feature/login ─────┐
                               │
            feature/signup ────┤
                               ↓
                            ┌─────┐
                            │ dev │
                            └─────┘
                               │
                         통합 테스트 / QA
                               │
                               ↓
                          ┌────────┐
                          │ master │
                          └────────┘
                               │
                               ↓
                              배포
```

핵심 원칙은 다음과 같습니다.

```text
작업 브랜치 → dev → master → 배포
```

`master`는 항상 배포 가능한 상태를 유지하고, 개발 중인 기능은 `dev`를 기준으로 관리합니다.
