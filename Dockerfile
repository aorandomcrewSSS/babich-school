# Этап сборки
FROM openjdk:17-jdk-slim AS build

# Установка Maven
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Копируем pom.xml и загружаем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники и компилируем проект
COPY src /app/src
RUN mvn clean package -DskipTests

# Финальный образ
FROM openjdk:17-jdk-slim

# Устанавливаем PostgreSQL 15 client (для работы с pg_dump)
RUN apt-get update && apt-get install -y \
    curl \
    lsb-release \
    gnupg \
    apt-transport-https \
    ca-certificates && \
    curl https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - && \
    echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -c | awk '{print $2}')-pgdg main" | tee /etc/apt/sources.list.d/pgdg.list && \
    apt-get update && \
    apt-get install -y postgresql-client-15

WORKDIR /app

# Копируем собранный JAR файл в контейнер
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Экспонируем порт для доступа к приложению
EXPOSE 8080

# Запускаем Spring Boot приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
