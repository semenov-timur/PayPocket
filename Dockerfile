# ==========================================
# Этап 1: СБОРКА
# ==========================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon

COPY src/ src/

RUN ./gradlew bootJar --no-daemon

# ==========================================
# Этап 2: ЗАПУСК
# ==========================================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]