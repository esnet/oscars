FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:23-alpine

ARG TARGET_HOST=oscars-backend:1099
ARG FILENAME=load-testing-results.csv
ARG WEB_REPORT_DIR=load-testing-web

RUN apk --update add wget unzip openjdk21
RUN wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip && \
    unzip apache-jmeter-5.6.3.zip
RUN mv apache-jmeter-5.6.3 /usr/bin/apache-jmeter-5.6.3
ENV JMETER_HOME /usr/bin/apache-jmeter-5.6.3
ENV PATH $JMETER_HOME/bin:$PATH
ENV HEAP -Xms1g -Xmx1g -XX:MaxMetaspaceSize=256m
ENV TARGET_HOST=${TARGET_HOST}
ENV FILENAME=${FILENAME}
ENV WEB_REPORT_DIR=${WEB_REPORT_DIR}

# Remote hosts, comma-delimited
RUN sed -i "s/remote_hosts=127.0.0.1/remote_hosts=${TARGET_HOST}/g" ${JMETER_HOME}/bin/jmeter.properties

# ================================================================================
# Don't use GUI mode for load testing !, only for Test creation and Test debugging.
# For load testing, use CLI Mode (was NON GUI):
#    jmeter -n -t [jmx file] -l [results file] -e -o [Path to web report folder]
# & increase Java Heap to meet your test requirements:
#    Modify current env variable HEAP="-Xms1g -Xmx1g -XX:MaxMetaspaceSize=256m" in the jmeter batch file
# Check : https://jmeter.apache.org/usermanual/best-practices.html
# ================================================================================
RUN mkdir -p /app && cd /app
RUN mkdir -p /app/load-testing
WORKDIR /app
COPY ../../backend/src/test/resources/load-testing.jmx ./load-testing.jmx
# VOLUME [ "/app/${WEB_REPORT_DIR}" ]
ENTRYPOINT jmeter -n -t ./load-testing.jmx -l "/app/load-testing/${FILENAME}" -e -o "/app/load-testing/${WEB_REPORT_DIR}"

