# Use Maven with JDK 21 to build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache dependencies: Only re-run if pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Use a slim JRE for the final image
FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app

# 1. Create a data directory and set ownership
# We do this as root before switching to USER 1000
RUN mkdir /app/data && chown 1000:1000 /app/data

# Copy with ownership directly to a specific UID
COPY --from=build --chown=1000:1000 /app/target/webmention-service-*.jar app.jar

# Tell Podman to run as this UID
USER 1000

# 2. Use a Volume for persistence
VOLUME /app/data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]