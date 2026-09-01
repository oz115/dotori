# AGENTS.md (backend)

백엔드(`backend/`) 작업 시 참고하는 지침입니다. 저장소 공통 지침은 [../AGENTS.md](../AGENTS.md)를 따릅니다.

## 스택

- 언어/프레임워크: Java, Spring Boot
- 빌드 도구: Gradle (`gradlew`)
- 패키지 루트: `com.bana.dotori`
- 인프라: PostgreSQL, Redis (`docker-compose.local.yml` 참고)

## 자주 쓰는 명령어

```bash
# 로컬 인프라(DB/Redis) 기동
docker compose -f docker-compose.local.yml up -d

# 빌드
./gradlew build

# 테스트
./gradlew test

# 로컬 실행
./gradlew bootRun
```

## 디렉토리 구조

```
backend/
└── src/
    ├── main/
    │   ├── java/com/bana/dotori/
    │   └── resources/application.yaml
    └── test/java/com/bana/dotori/
```

## 코드 스타일 (네이버 Java 코딩 컨벤션 v1.2)

IntelliJ에 적용된 `Naver-coding-convention-v1.2` 스킴 기준. 전체 문서: https://naver.github.io/hackday-conventions-java/

- 들여쓰기: **탭 문자** 사용 (스페이스 금지), 연속 줄(continuation) 들여쓰기는 추가 1탭
- 최대 줄 길이: **120자**
- 중괄호: K&R 스타일 (여는 중괄호를 선언/제어문과 같은 줄 끝에)
- 제어문은 한 줄이어도 반드시 중괄호 사용, 한 줄로 축약 금지 (`if (x) return;` 금지)
- import: 와일드카드(`*`) 금지, 순서는 static → java → javax → org → net → com → 기타 (그룹 사이 빈 줄 1개)
- 연산자에서 줄바꿈 시 연산자를 **다음 줄 맨 앞**에 배치
- 타입 캐스트 뒤 공백 없음: `(String)value`
- 배열 초기화 중괄호 앞 공백: `int[] arr = {1, 2, 3};` 처럼 `= {` 형태
- 연속 빈 줄은 최대 1줄
- 라인 주석은 `// 내용` 처럼 `//` 뒤에 공백 1개

## 규칙

- 네이밍/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- API 스펙 변경 시 [../docs/API.md](../docs/API.md)를 함께 갱신한다.
- DB/아키텍처 변경 시 [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)를 함께 갱신한다.
- 환경변수는 `.env`(gitignore 대상)로 관리하고 `.env.example`을 최신 상태로 유지한다.
