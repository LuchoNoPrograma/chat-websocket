FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package
#
# Package stage
#

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/chat-websocket-0.0.1-SNAPSHOT.jar app.jar
RUN useradd --system --no-create-home appuser
USER appuser
EXPOSE 7071
ENTRYPOINT ["sh", "-c", "exec java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar"]
