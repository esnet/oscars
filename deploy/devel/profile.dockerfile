FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:23-alpine
RUN sed -i '2s/^# *//' /etc/apk/repositories
RUN apk update
RUN apk add wget unzip openjdk21

RUN mkdir /app
WORKDIR /app
COPY backend/bin/sjk-plus-0.23.jar /app/sjk.jar
#RUN wget https://repo1.maven.org/maven2/org/gridkit/jvmtool/sjk-plus/0.23/sjk-plus-0.23.jar
#RUN mv sjk-plus-0.23.jar sjk.jar
COPY backend/bin/profiling.sh /app/profiling.sh
CMD ./profiling.sh