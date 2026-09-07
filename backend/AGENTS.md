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
### 테스트 DB

- 테스트는 H2 같은 인메모리 DB를 쓰지 않고 **애플리케이션과 같은 PostgreSQL**을 쓴다.
  행 잠금(`SELECT ... FOR UPDATE`), `TIMESTAMPTZ` 환산, 커서 정렬, CHECK·부분 유니크 제약처럼
  H2의 PostgreSQL MODE가 흉내 내지 못하는 동작이 검증 대상이라, H2로 통과해도 배포에서 깨진다.
- 설정은 `src/test/resources/application.yaml` **하나**다. 테스트 클래스패스가 메인 설정보다
  앞서므로 이 파일이 테스트 전체의 설정이 된다. 프로파일 전용 파일은 없다
  (일부 테스트의 `@ActiveProfiles("test")`는 통합 테스트를 표시하는 관례로 남아 있다).
- 실행 전에 DB로 가는 터널을 먼저 연다: `ssh -N A505`
- 스키마는 `src/test/resources/schema.sql`이 `CREATE TABLE IF NOT EXISTS` + 제약조건 재적용으로 맞춘다.
  `ddl-auto: none`이므로 **자기 도메인 테이블이 필요하면 이 파일에 직접 추가한다.**
  지금은 지갑 범위 테이블만 있어서, 다른 도메인은 EC2 DB에 이미 만들어진 테이블에 의존한다
  (= 완전히 새 DB에서는 깨진다).
- `@DataJpaTest`는 기본적으로 데이터소스를 임베디드 DB로 갈아끼운다. 임베디드 드라이버가
  클래스패스에 없으므로 `@AutoConfigureTestDatabase(replace = NONE)`을 붙인다.
- 뒷정리(`WalletTestFixture.cleanUp`)는 **테스트가 만든 회원과 거기 딸린 행만** 지운다.
  판별 기준은 이메일 `{loginId}@dotori.test`이고, 집계(`countTransactions`)도 같은 범위다.
  DB를 공유하므로 테이블을 통째로 비우면 안 된다 — 예전에는 `DELETE FROM users`로 싹 지웠는데,
  그러려면 `users`를 참조하는 테이블을 전부 먼저 비워야 해서 남의 도메인 데이터(연동 계좌 등)까지
  딸려 나갔고, 다른 도메인이 `users` 참조 테이블을 추가할 때마다 삭제 목록이 낡아
  FK 위반(23503)으로 지갑 테스트가 통째로 깨졌다.
  - **DB에 붙는 테스트를 새로 쓸 때는 회원 이메일을 `@dotori.test`로 맞춘다.**
    그래야 뒷정리 대상에 들어가고, 남의 데이터와 섞이지 않는다.

## 규칙

- 네이밍/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- API 명세는 노션(작업 > API 명세)이 유일한 원본이다. docs/API.md는 관리하지 않는다 (갱신 금지).
- DB/아키텍처 변경 시 [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)를 함께 갱신한다.
- 환경변수는 `.env`(gitignore 대상)로 관리하고 `.env.example`을 최신 상태로 유지한다.
