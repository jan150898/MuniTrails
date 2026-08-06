# Multi-stage Docker build for production deployment

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Install Cloud SQL Proxy
RUN apt-get update && apt-get install -y curl && \
    curl -o cloud-sql-proxy https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 && \
    chmod +x cloud-sql-proxy && \
    apt-get remove -y curl && apt-get clean

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Production profile: Use real PostgreSQL database
ENV SPRING_PROFILES_ACTIVE=prod

# Start both Cloud SQL Proxy and Spring Boot
CMD ["sh", "-c", "./cloud-sql-proxy project-d1b0d97e-f7aa-4f2f-b78:europe-west1:munitrails-db --port=5432 & java -jar app.jar"]
