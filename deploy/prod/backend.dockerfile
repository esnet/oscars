FROM maven:3.9.2  AS builder

WORKDIR /build
COPY pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn org.apache.maven.plugins:maven-dependency-plugin:3.5.0:resolve-plugins org.apache.maven.plugins:maven-dependency-plugin:3.5.0:go-offline  -B
RUN --mount=type=cache,target=/root/.m2 mvn verify --fail-never

# build and package spring app
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn compile --offline
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests --offline

# copy / extract jar file
ARG JAR_FILE=target/*.jar
RUN mv ${JAR_FILE} backend.jar
RUN java -Djarmode=layertools -jar backend.jar extract

# 2. run stage
FROM bellsoft/liberica-openjdk-alpine-musl:17
RUN addgroup -S oscars && adduser -S oscars -G oscars
RUN mkdir -p /app
RUN mkdir -p /app/log
RUN chown oscars -R /app

USER oscars
WORKDIR /app
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher", "--spring.config.location=/app/config/application.properties"]
