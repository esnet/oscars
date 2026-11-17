FROM wharf.es.net/dockerhub-proxy/library/amazoncorretto:25-alpine

ARG FILENAME="recording.jfr"
ARG WEB_TARGET_HOST=oscars-backend
ENV FILENAME=${FILENAME}
ENV WEB_TARGET_HOST=${WEB_TARGET_HOST}

RUN echo "JFR filename is ${FILENAME}}"

RUN sed -i '2s/^# *//' /etc/apk/repositories
RUN apk update
RUN apk add wget unzip openjdk21

RUN mkdir /app
WORKDIR /app

COPY backend/bin/sjk-plus-0.23.jar /app/sjk.jar
#RUN wget https://repo1.maven.org/maven2/org/gridkit/jvmtool/sjk-plus/0.23/sjk-plus-0.23.jar
#RUN mv sjk-plus-0.23.jar sjk.jar

# JMeter, used by the 500_loadtest.sh profiling script to generate real load
# during the JFR recording window instead of just sleeping (see 001_delay.sh).
RUN wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip && \
    unzip apache-jmeter-5.6.3.zip && \
    mv apache-jmeter-5.6.3 /usr/bin/apache-jmeter-5.6.3
ENV JMETER_HOME /usr/bin/apache-jmeter-5.6.3
ENV PATH $JMETER_HOME/bin:$PATH
COPY backend/src/test/resources/load-testing.jmx /app/load-testing.jmx
COPY backend/src/test/resources/load-testing-pairs.csv /app/load-testing-pairs.csv

COPY backend/bin/profiling.sh /app/profiling.sh
COPY backend/src/test/profiling /app/profiling
CMD ./profiling.sh