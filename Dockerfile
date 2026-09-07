FROM maven:3.8.3-openjdk-17 AS build
COPY . .
RUN mvn clean package
#
# Package stage
#

FROM openjdk:17-jdk-slim
COPY --from=build /target/chat-websocket-0.0.1-SNAPSHOT.jar app.jar
RUN useradd --system --no-create-home appuser
USER appuser
EXPOSE 7071
ENTRYPOINT ["sh", "-c", "exec java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar"]
