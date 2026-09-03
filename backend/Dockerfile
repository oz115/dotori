# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# 의존성 캐시를 활용하기 위해 gradle 설정 파일만 먼저 복사한다
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies || true

# 소스 복사 후 빌드 (테스트는 CI 단계에서 별도로 돌리므로 여기선 생략)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S dotori && adduser -S dotori -G dotori
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown dotori:dotori app.jar
USER dotori

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
