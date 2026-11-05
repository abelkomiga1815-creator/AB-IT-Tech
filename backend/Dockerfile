# Build stage: compiles the application (tests are run separately: mvn clean test)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Run stage: only the JRE and the jar, running as an unprivileged user
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app \
 && mkdir -p /data/documents && chown -R app:app /data
USER app
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
EXPOSE 8080
# All settings (database, JWT secret, AI key, ...) come from environment variables at run time.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
