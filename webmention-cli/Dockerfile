FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build

COPY pom.xml .
COPY webmention-core/pom.xml ./webmention-core/
COPY webmention-cli/pom.xml ./webmention-cli/
COPY webmention-javalin/pom.xml ./webmention-javalin/
COPY webmention-service/pom.xml ./webmention-service/

RUN mvn dependency:go-offline -e -B -DexcludeArtifactIds=webmention-core,webmention-cli,webmention-javalin,webmention-service

COPY . .
RUN mvn clean package -DskipTests -Dgpg.skip

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder --chown=1000:1000 /build/webmention-cli/target/webmention-cli-*.jar app.jar

USER 1000

ENTRYPOINT ["java", "-jar", "app.jar"]

CMD ["--help"]