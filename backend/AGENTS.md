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

## 규칙

- 네이밍/커밋 컨벤션은 [../docs/CONVENTIONS.md](../docs/CONVENTIONS.md)를 따른다.
- API 스펙 변경 시 [../docs/API.md](../docs/API.md)를 함께 갱신한다.
- DB/아키텍처 변경 시 [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)를 함께 갱신한다.
- 환경변수는 `.env`(gitignore 대상)로 관리하고 `.env.example`을 최신 상태로 유지한다.
