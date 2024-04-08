Generate a dump on production with
```
pg_dumpall -Uoscars > dumpall.sql
```

Bring it over to this directory and use import.sh to replace the local DB with it.

You should only do this with `startup.standalone = true` in `backend/config/application.properties` 