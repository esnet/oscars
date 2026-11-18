FROM wharf.es.net/dockerhub-proxy/library/maven:3.10.0-amazoncorretto-27-debian AS builder

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
    -Daether.dependencyCollector.impl=bf -Daether.dependencyCollector.bf.threads=10 \
    -Daether.remoteRepositoryFilter.groupId=true  \
    -Daether.remoteRepositoryFilter.groupId.basedir=/build/backend/.remoteRepositoryFilters

# another layer that downloads and resolves stuff from maven with a goal of `package`
RUN --mount=type=cache,target=/root/.m2 mvn  \
    package --fail-never  \
    -Daether.dependencyCollector.impl=bf -Daether.dependencyCollector.bf.threads=10 \
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

# 3. dev stage - used only by Okteto's inner dev loop, never for real deploys
FROM builder AS dev

RUN apt-get update \
    && apt-get install -y --no-install-recommends entr \
    && rm -rf /var/lib/apt/lists/*

# BuildKit cache mounts (--mount=type=cache) never land in committed image
# layers, so a plain `FROM builder` has an EMPTY ~/.m2 at runtime even though
# the image built fine. Materialize it into a real layer.
RUN --mount=type=cache,target=/root/.m2 \
    mkdir -p /opt/m2-repo && cp -a /root/.m2/. /opt/m2-repo/
ENV MAVEN_OPTS="${MAVEN_OPTS} -Dmaven.repo.local=/opt/m2-repo"

COPY deploy/devel/dev-entrypoint.sh /usr/local/bin/dev-entrypoint.sh
RUN chmod +x /usr/local/bin/dev-entrypoint.sh

EXPOSE 9201
CMD ["/usr/local/bin/dev-entrypoint.sh"]

# 2. run stage
FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:27-alpine
RUN addgroup -S oscars && adduser -S oscars -G oscars
RUN mkdir -p /app

RUN chown oscars -R /app
USER oscars

# for development we copy config
WORKDIR /app
RUN mkdir -p /app/log
RUN mkdir -p -m 760 /app/profiling

COPY ./backend/config ./config
COPY --from=builder /build/backend/layers/dependencies/ ./
COPY --from=builder /build/backend/layers/spring-boot-loader ./
COPY --from=builder /build/backend/layers/snapshot-dependencies/ ./
COPY --from=builder /build/backend/layers/application/ ./

# Debugger port
EXPOSE 9201

# run the application with debug and profiling options enabled
ENTRYPOINT sh -c 'java ${JAVA_OPTS} -jar backend.jar'
