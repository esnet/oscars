#!/usr/bin/env ash
echo -e "\tSTART load generation against ${WEB_TARGET_HOST} for the duration of this JFR recording."

mkdir -p /app/loadtest-output
jmeter -n -t /app/load-testing.jmx -Jhost="${WEB_TARGET_HOST}" \
    -JthreadCount="${LOADTEST_THREAD_COUNT:-5}" \
    -Jduration="${LOADTEST_DURATION:-60}" \
    -JrampTime="${LOADTEST_RAMP_TIME:-5}" \
    -JthinkTimeMs="${LOADTEST_THINK_TIME_MS:-200}" \
    -l /app/loadtest-output/loadtest-results.csv \
    -e -o /app/loadtest-output/loadtest-report

echo -e "\tEND load generation."
