# ── Build stage ──
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

# ── Runtime stage ──
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S hirehub && adduser -S hirehub -G hirehub

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown -R hirehub:hirehub /app
USER hirehub

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
