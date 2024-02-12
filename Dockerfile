FROM maven:3.8.3-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests
#
# Package stage
#

FROM openjdk:17-jdk-slim
COPY --from=build /target/chat-websocket-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT java -DSPRING_PROGFILES_ACTIVE=${SPRING_PROFILES_ACTIVE} -DDB_URI=${DB_URI} -DDB_NAME=${DB_NAME} -jar app.jar