FROM wharf.es.net/dockerhub-proxy/library/maven:3.9.15-amazoncorretto-25-debian AS builder

ARG JAVA_OPTS=""
ARG MAVEN_OPTS=""

ENV JAVA_OPTS=${JAVA_OPTS}
ENV MAVEN_OPTS=${MAVEN_OPTS}
ENV DEBIAN_FRONTEND=noninteractive

WORKDIR /build/backend
COPY backend/.remoteRepositoryFilters .remoteRepositoryFilters
COPY backend/pom.xml pom.xml

# layer that downloads and resolves stuff from maven (including maven's own plugins and their dependencies)
# this has goals `resolve-plugins` and `go-offline`
RUN --mount=type=cache,target=/root/.m2 mvn  \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:resolve-plugins  \
    org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline  \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

# another layer that downloads and resolves stuff from maven with a goal of `package`
RUN --mount=type=cache,target=/root/.m2 mvn  \
    package --fail-never  \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

# now finally build and package spring app
COPY backend/src ./src

# layers that actually compile and package the project
RUN --mount=type=cache,target=/root/.m2 mvn compile --offline
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests --offline

# copy / extract jar file
ARG JAR_FILE=target/*.jar
RUN mv ${JAR_FILE} backend.jar
RUN java -Djarmode=tools -jar backend.jar extract --layers --destination layers

FROM builder AS test
WORKDIR /build/backend
COPY ./backend/config ./config
RUN --mount=type=cache,target=/root/.m2 mvn test
