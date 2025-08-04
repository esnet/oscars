#!/usr/bin/env sh
export OSCARS_PID=`ps -ef | grep "oscars" | grep -v grep | awk '{print $2}'`
jcmd ${OSCARS_PID} JFR.stop name=oscars-profile.jfr
