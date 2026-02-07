# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# 1. Define the git tag as an argument. Defaults to main.
ARG GIT_TAG=main
ARG REPO_URL=https://github.com/Maritims/webmention.git

WORKDIR /build

# 2. Install git and clone the specific tag.
RUN apt-get update && apt-get install -y git && \
    git clone --branch ${GIT_TAG} --depth 1 ${REPO_URL} . && \
    mvn dependency:go-offline && \
    mvn clean package -DskipTests -Dgpg.skip

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# 1. Create a data directory and set ownership
# We do this as root before switching to USER 1000
RUN mkdir /app/data && chown 1000:1000 /app/data

# Copy with ownership directly to a specific UID
COPY --from=builder --chown=1000:1000 /build/webmention-service/target/webmention-service-*.jar app.jar

# Tell Podman to run as this UID
USER 1000

# 2. Use a Volume for persistence
VOLUME /app/data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]