# ---- Stage 1: Build ----
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# BUILD_PROFILE controls which Maven profile is active during build.
# dev  → application-dev.yml loaded, dev deps included
# prod → application-prod.yml loaded, prod-only settings
ARG BUILD_PROFILE=dev
ENV MAVEN_PROFILE=${BUILD_PROFILE}

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Download dependencies first (layer caching — only re-runs when pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -P${MAVEN_PROFILE} -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
