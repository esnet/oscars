#!/usr/bin/env sh
export OSCARS_PID=`ps -ef | grep "oscars" | grep -v grep | awk '{print $2}'`
jcmd ${OSCARS_PID} JFR.start delay=0s duration=3m filename=oscars-profile.jfr
