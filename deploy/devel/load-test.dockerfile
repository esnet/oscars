FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:23-alpine
RUN apk --update add wget unzip openjdk21
RUN wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip && \
    unzip apache-jmeter-5.6.3.zip
RUN mv apache-jmeter-5.6.3 /usr/bin/apache-jmeter-5.6.3
ENV JMETER_HOME /usr/bin/apache-jmeter-5.6.3
ENV PATH $JMETER_HOME/bin:$PATH
ENV HEAP -Xms1g -Xmx1g -XX:MaxMetaspaceSize=256m
# Remote hosts, comma-delimited
RUN sed -i 's/remote_hosts=127.0.0.1/remote_hosts=oscars-backend.ocd-stack.orb.local:1099/g' ${JMETER_HOME}/bin/jmeter.properties

# ================================================================================
# Don't use GUI mode for load testing !, only for Test creation and Test debugging.
# For load testing, use CLI Mode (was NON GUI):
#    jmeter -n -t [jmx file] -l [results file] -e -o [Path to web report folder]
# & increase Java Heap to meet your test requirements:
#    Modify current env variable HEAP="-Xms1g -Xmx1g -XX:MaxMetaspaceSize=256m" in the jmeter batch file
# Check : https://jmeter.apache.org/usermanual/best-practices.html
# ================================================================================
RUN mkdir -p /app && cd /app
RUN mkdir -p /app/web-report
WORKDIR /app
COPY ../../backend/src/test/resources/load-testing.jmx ./load-testing.jmx
ENTRYPOINT jmeter -n -t ./load-testing.jmx -l load-testing-results.txt -e -o ./web-report

