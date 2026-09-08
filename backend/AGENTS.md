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

## 테스트 규칙

- 테스트는 레이어별로 분리해서 작성한다 (main과 동일한 패키지 구조 아래에 배치):
  - **dto**: Bean Validation 검증 테스트 — `jakarta.validation.Validator`로 필드 제약(@NotBlank, @Email 등)을 단위 검증
  - **service**: Mockito 단위 테스트 — `@ExtendWith(MockitoExtension.class)`, repository/encoder는 `@Mock`, mapper는 `@Spy`
  - **controller**: `@WebMvcTest` 슬라이스 테스트 — service는 `@MockitoBean`, `SecurityConfig`를 `@Import`해서 시큐리티 규칙 포함, MockMvc로 상태 코드·응답 JSON·에러 형식 검증
- 테스트 클래스명은 `{대상 클래스}Test`, 메서드명은 `{기능}_{시나리오}` 형식으로 작성한다.
- **새 기능은 위 세 계층(dto·service·controller)까지만 쓴다.** 리포지토리 테스트와 통합 테스트는
  기본으로 만들지 않는다 — 테스트가 많아 개발 속도가 떨어진다는 팀 결정이다.
  DB 를 띄우는 테스트는 그것 없이는 검증이 불가능할 때만 만들고, 만들기 전에 팀에 알린다.
  - 이미 있는 지갑·가족 쪽 리포지토리·통합 테스트는 그대로 둔다 (동시성·잠금 검증이 목적이라 대체가 없다).
  - 엔티티에 계산 로직이 있으면 스프링 없는 단위 테스트로 덮는다 (`PointWalletTest`, `ChoreTest`).
  - 대신 스키마와 엔티티가 어긋나는 것은 테스트가 아니라 `ddl-auto: validate` 로 잡는다.
### DB 두 개를 역할로 나눠 쓴다

| | 로컬 도커 DB | 공용 EC2 DB |
|---|---|---|
| 용도 | **테스트 전용** (`./gradlew test`) | **QA 전용** — 데이터를 쌓아 두고 실제 기능 확인 |
| 접속 | `localhost:5433`, 각자 것 | `ssh -N A505` 터널 → `localhost:5432` |
| 설정 | `src/test/resources/application.yaml` (기본값 내장) | `.env` → `src/main/resources/application.yaml` |
| 데이터 | 언제 날아가도 됨 | **테스트로 건드리지 않는다** |

테스트 설정은 `.env`를 **일부러 읽지 않는다.** `.env`는 앱을 공용 DB에 붙이는 설정이라,
물려받으면 테스트 뒷정리가 QA 데이터를 지운다. 그 사고를 구조적으로 막으려고 경로를 끊었다.
테스트 쪽 접속 정보를 바꿔야 하면 `TEST_POSTGRES_*` 환경변수를 쓴다.

포트가 5433인 이유는 5432를 위 ssh 터널이 쓰기 때문이다. 갈라 둬서 둘을 동시에 켜 둘 수 있다.

### 테스트 DB

- H2 같은 인메모리 DB를 쓰지 않고 **PostgreSQL**을 쓴다.
  행 잠금(`SELECT ... FOR UPDATE`), `TIMESTAMPTZ` 환산, 커서 정렬, CHECK·부분 유니크 제약처럼
  H2의 PostgreSQL MODE가 흉내 내지 못하는 동작이 검증 대상이라, H2로 통과해도 배포에서 깨진다.
- 설정은 `src/test/resources/application.yaml` **하나**다. 테스트 클래스패스가 메인 설정보다
  앞서므로 이 파일이 테스트 전체의 설정이 된다. 프로파일 전용 파일은 없다
  (일부 테스트의 `@ActiveProfiles("test")`는 통합 테스트를 표시하는 관례로 남아 있다).
- 실행 전에 로컬 인프라를 띄운다: `docker compose -f docker-compose.local.yml up -d`
- `@DataJpaTest`는 기본적으로 데이터소스를 임베디드 DB로 갈아끼운다. 임베디드 드라이버가
  클래스패스에 없으므로 `@AutoConfigureTestDatabase(replace = NONE)`을 붙인다.
- 뒷정리(`WalletTestFixture.cleanUp`)는 **테스트가 만든 회원과 거기 딸린 행만** 지운다.
  판별 기준은 이메일 `{loginId}@autotest.local`이고, 집계(`countTransactions`)도 같은 범위다.
  - **DB에 붙는 테스트를 새로 쓸 때는 회원 이메일을 `@autotest.local`로 맞춘다.**

### 스키마

- 원본은 **`backend/docker/initdb/01-schema.sql` 하나**다. 노션의 DDL과 같은 내용이어야 하고,
  노션을 고쳤으면 이 파일도 같은 커밋에서 고친다.
- `ddl-auto: none`이다. Hibernate는 스키마를 만들지도 고치지도 않는다.
  **엔티티에 컬럼을 추가했으면 이 파일에도 추가한다.**
- 도커 postgres는 데이터 볼륨이 **비어 있을 때만** 이 스크립트를 실행한다.
  스키마를 고친 뒤에는 볼륨을 지우고 다시 띄워야 반영된다:

  ```bash
  docker compose -f docker-compose.local.yml down -v && docker compose -f docker-compose.local.yml up -d
  ```

- 공용 EC2 DB에는 자동으로 반영되지 않는다. 필요하면 인프라 담당자와 협의해 수동 실행한다
  (스크립트 맨 위 `DROP`이 데이터를 전부 날리므로 그냥 붙여넣지 않는다).
- 예전에 있던 `src/test/resources/schema.sql`은 없앴다. `ddl-auto: update` 시절의 잔재로
  지갑 범위 6개 테이블만 담고 enum 컬럼을 `VARCHAR`로 선언해 실제 스키마와 어긋나 있었다.

## 규칙

- 네이밍/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- API 명세는 노션(작업 > API 명세)이 유일한 원본이다. docs/API.md는 관리하지 않는다 (갱신 금지).
- DB/아키텍처 변경 시 [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)를 함께 갱신한다.
- 환경변수는 `.env`(gitignore 대상)로 관리하고 `.env.example`을 최신 상태로 유지한다.
