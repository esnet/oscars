#!/usr/bin/env ash
#export DELAY_MIN=1m
#export DURATION_MIN=1m
#export TARGET_HOST=oscars-backend:1099

echo "Attempt JMX connection to remote java app. Delay recording for ${DELAY_MIN}, recording until JFR.stop is issued. Target Host is ${TARGET_HOST}"
sleep ${DELAY_MIN}

cd /app/profiling
java -jar /app/sjk.jar jcmd -s ${TARGET_HOST} -c JFR.start name="oscars-snapshot" filename=${FILENAME}
# Run all scripts

for file in $(ls -tr); do
    if [ -f "$file" ]; then
        extension="${file##*.}"
        if [ "$extension" = "sh" ]; then
            echo -e "\t...RUN ${file}"
            sh "${file}"
        fi
    fi
done

java -jar /app/sjk.jar jcmd -s ${TARGET_HOST} -c JFR.stop name="oscars-snapshot"
java -jar /app/sjk.jar jcmd -s ${TARGET_HOST} -c JFR.dump name="oscars-snapshot" filename=${FILENAME} && echo "Done."

cd /app
