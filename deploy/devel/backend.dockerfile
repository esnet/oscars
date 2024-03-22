FROM maven:3.9.5  AS builder

WORKDIR /build/backend
COPY backend/.remoteRepositoryFilters .remoteRepositoryFilters
COPY backend/pom.xml pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn  \
    org.apache.maven.plugins:maven-dependency-plugin:3.5.0:resolve-plugins  \
    org.apache.maven.plugins:maven-dependency-plugin:3.5.0:go-offline  \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

RUN --mount=type=cache,target=/root/.m2 mvn  \
    package --fail-never  \
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
FROM bellsoft/liberica-openjdk-centos:20
RUN groupadd oscars && useradd -g oscars oscars
RUN mkdir -p /app
RUN mkdir -p /app/config
RUN mkdir -p /app/log
RUN chown oscars -R /app
USER oscars

# for development we copy config
WORKDIR /app
COPY ./backend/config ./config
COPY --from=builder /build/backend/dependencies/ ./
COPY --from=builder /build/backend/spring-boot-loader ./
COPY --from=builder /build/backend/snapshot-dependencies/ ./
COPY --from=builder /build/backend/application/ ./

# run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
