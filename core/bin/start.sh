#!/bin/bash
function tabname {
  echo -n -e "\033]0;$1\007"
}
tabname "oscars core"

java -jar target/core-0.7.0.jar
