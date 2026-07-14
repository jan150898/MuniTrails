# Simple Docker build (no fancy dependency steps) to avoid flaky build cache commits

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./

# Copy source and build in one layer (avoids multi-stage cache finalize issues)
COPY src ./src
RUN mvn -q -DskipTests package


FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

