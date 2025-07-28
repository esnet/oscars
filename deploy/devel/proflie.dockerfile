FROM oscars-backend-image
# Profiler port
EXPOSE 1099

# run the application
RUN sh -c 'java "$JAVA_OPTS" \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=1099 \
-Dcom.sun.management.jmxremote.rmi.port=1099 \
-Djava.rmi.server.hostname="$HOSTNAME" \
-Dcom.sun.management.jmxremote.local.only=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
org.springframework.boot.loader.launch.JarLauncher'

ENTRYPOINT visualvm --