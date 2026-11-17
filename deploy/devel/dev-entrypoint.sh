#!/bin/sh
set -eu
cd /build/backend

# fork=true (default) is required for devtools' restart-classloader mechanism.
# JAVA_OPTS (carrying the JDWP flag) must be routed into the *forked* app JVM
# via spring-boot.run.jvmArguments -- plain env JAVA_OPTS isn't auto-forwarded.
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=${JAVA_OPTS:-}" &
APP_PID=$!

# Recompile on every .java change. devtools (on the classpath) polls
# target/classes and triggers an in-JVM restart once new class files land --
# this loop's only job is to produce those class files.
# Deliberately NOT --offline here (unlike the image-build steps, which stay
# untouched): a developer adding a new dependency mid-session should get a
# real download attempt rather than a hard failure.
(
  while true; do
    find src -name '*.java' | entr -d mvn -q compile
  done
) &
WATCH_PID=$!

trap 'kill "$APP_PID" "$WATCH_PID" 2>/dev/null' TERM INT
wait "$APP_PID"
