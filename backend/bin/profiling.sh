#!/usr/bin/env ash
#export DELAY_MIN=1m
#export DURATION_MIN=1m
#export TARGET_HOST=oscars-backend:1099
echo "Attempt JMX connection to remote java app. Delay recording for ${DELAY_MIN}, record for ${DURATION_MIN}"
sleep ${DELAY_MIN}
java -jar sjk.jar jcmd -s ${TARGET_HOST} -c JFR.start name=oscars-snapshot filename=recording.jfr
sleep ${DURATION_MIN}
java -jar sjk.jar jcmd -s ${TARGET_HOST} -c JFR.stop name=oscars-snapshot
java -jar sjk.jar jcmd -s ${TARGET_HOST} -c JFR.dump name=oscars-snapshot filename=recording.jfr && echo "Done."