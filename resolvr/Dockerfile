FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn --batch-mode verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/resolvr-1.0.0.jar app.jar
RUN addgroup -S resolvr && adduser -S -G resolvr resolvr
RUN mkdir -p /app/dev-credentials && chown resolvr:resolvr /app/dev-credentials && chmod 700 /app/dev-credentials
USER resolvr
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
