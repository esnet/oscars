FROM wharf.es.net/dockerhub-proxy/library/maven:3.9.9-amazoncorretto-23-debian  AS builder

ARG JAVA_OPTS=""
ARG MAVEN_OPTS=""

ENV JAVA_OPTS=${JAVA_OPTS}
ENV MAVEN_OPTS=${MAVEN_OPTS}
ENV DEBIAN_FRONTEND=noninteractive

WORKDIR /build/backend
COPY backend/.remoteRepositoryFilters .remoteRepositoryFilters
COPY backend/pom.xml pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn  \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:resolve-plugins  \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline  \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

RUN --mount=type=cache,target=/root/.m2 mvn  \
    package --fail-never  \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

# build and package spring app
COPY backend/src ./src
COPY backend/config ./config
RUN --mount=type=cache,target=/root/.m2 mvn compile --offline
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests --offline

# copy / extract jar file
ARG JAR_FILE=target/*.jar
RUN mv ${JAR_FILE} backend.jar
RUN java $JAVA_OPTS -Djarmode=layertools -jar backend.jar extract

FROM builder AS test
WORKDIR /build/backend
RUN --mount=type=cache,target=/root/.m2 mvn test

# 2. run stage
FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:23-alpine as runner
RUN sed -i '2s/^# *//' /etc/apk/repositories
RUN apk update
RUN apk add wget unzip openjdk21
RUN addgroup -S oscars && adduser -S oscars -G oscars
RUN mkdir -p /app

RUN chown oscars -R /app
USER oscars

# for development we copy config
WORKDIR /app
RUN mkdir -p /app/log
COPY ./backend/config ./config
COPY --from=builder /build/backend/dependencies/ ./
COPY --from=builder /build/backend/spring-boot-loader ./
COPY --from=builder /build/backend/snapshot-dependencies/ ./
COPY --from=builder /build/backend/application/ ./

# Debugger port
EXPOSE 9201

# run the application with debug and profiling options enabled
ENTRYPOINT sh -c 'java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher'

FROM runner AS profile
WORKDIR /app
COPY backend/bin/profiling-start.sh /app/profiling-start.sh
COPY backend/bin/profiling-stop.sh /app/profiling-stop.sh
ENTRYPOINT ./profiling-start.sh && sleep 3m && ./profiling-stop.sh
