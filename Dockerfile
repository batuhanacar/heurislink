FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY . .

RUN ./gradlew bootJar


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN groupadd --gid 10001 heurislink \
    && useradd \
        --uid 10001 \
        --gid 10001 \
        --no-create-home \
        --shell /usr/sbin/nologin \
        heurislink

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]