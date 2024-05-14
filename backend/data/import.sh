#!/usr/bin/env sh
echo "clearing existing oscars-db database"
docker cp clear.sql oscars-db:/tmp/clear.sql
docker exec -it oscars-db /usr/local/bin/psql -Uoscars -f /tmp/clear.sql

echo "importing database backup"
docker cp dumpall.sql oscars-db:/tmp/dumpall.sql
docker exec -it oscars-db /usr/local/bin/psql -Uoscars -f /tmp/dumpall.sql
docker exec -it oscars-db  /usr/local/bin/psql -Uoscars -c "ALTER USER oscars PASSWORD 'oscars';"
