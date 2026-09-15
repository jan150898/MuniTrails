# Multi-stage Docker build for production deployment
# Uses Docker Hardened Images (DHI) for enhanced security

FROM dhi.io/eclipse-temurin:17-jdk-alpine3.23-dev AS build
WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy and build
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime stage: minimal JRE image
FROM dhi.io/eclipse-temurin:17.0-alpine3.23
WORKDIR /app

# Copy compiled JAR from build stage
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/target/classes/com/example/trails/HealthcheckProbe.class /app/healthcheck-classes/com/example/trails/HealthcheckProbe.class

EXPOSE 8080

# Production profile: Use real PostgreSQL database
ENV SPRING_PROFILES_ACTIVE=prod

# Use the JDK-only probe because the minimal runtime image has no shell or curl.
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD ["java", "-cp", "/app/healthcheck-classes", "com.example.trails.HealthcheckProbe"]

ENTRYPOINT ["java","-Xmx512m","-Xms256m","-jar","app.jar"]
