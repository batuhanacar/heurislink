FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY . .

RUN ./gradlew clean bootJar --no-daemon


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/heurislink-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]