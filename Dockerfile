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

EXPOSE 8080

# Production profile: Use real PostgreSQL database
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher &>/dev/null || exit 1

ENTRYPOINT ["java","-Xmx512m","-Xms256m","-jar","app.jar"]
