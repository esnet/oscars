FROM wharf.es.net/dockerhub-proxy/library/maven:3.9.9-amazoncorretto-23-debian  AS builder

WORKDIR /build/backend
COPY backend/.remoteRepositoryFilters .remoteRepositoryFilters
COPY backend/pom.xml pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn  \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:resolve-plugins  \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline -B \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

RUN --mount=type=cache,target=/root/.m2 mvn  \
    package --fail-never \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

# build and package spring app
COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn compile --offline
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests --offline

# copy / extract jar file
ARG JAR_FILE=target/*.jar
RUN mv ${JAR_FILE} backend.jar
RUN java -Djarmode=layertools -jar backend.jar extract

# 2. run stage
FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:23-alpine
RUN addgroup -S oscars && adduser -S oscars -G oscars
RUN mkdir -p /app
RUN chown oscars -R /app

USER oscars
WORKDIR /app
RUN mkdir -p /app/log
COPY --from=builder /build/backend/dependencies/ ./
COPY --from=builder /build/backend/spring-boot-loader ./
COPY --from=builder /build/backend/snapshot-dependencies/ ./
COPY --from=builder /build/backend/application/ ./

# run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher", "--spring.config.location=/app/config/application.properties"]
