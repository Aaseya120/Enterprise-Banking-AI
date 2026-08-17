# Generic Dockerfile for any module in this repo. Build from the repo root with:
#   docker build -f deployment/docker/Dockerfile \
#     --build-arg MODULE_PATH=banking-services/account-service \
#     --build-arg JAR_NAME=account-service \
#     -t bank/account-service .
#
# (docker-compose.yml already does this for every service.)

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common ./common
COPY infrastructure ./infrastructure
COPY banking-services ./banking-services
COPY ai-platform ./ai-platform
ARG MODULE_PATH
RUN mvn -q -pl ${MODULE_PATH} -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
ARG MODULE_PATH
ARG JAR_NAME
WORKDIR /app
COPY --from=build /workspace/${MODULE_PATH}/target/${JAR_NAME}.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
