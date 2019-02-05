# Updating OSCARS
This file contains instructions for updating an existing installation of OSCARS from a previous version to a newer version. 

Instructions include config file changes, database schema changes, etc.

## 1.0.24 to 1.0.25:

### Config file changes

- Topology devices.json file now supports these fields per device entry:

```
  "location": "a string",
  "location_id": integer,
  "longitude": double,
  "latitude": double
```

### Database schema changes:

In the `oscars_backend` database:

```sql
alter table device insert column location varchar(255);
alter table device insert column location_id int;
alter table device insert column latitude double;
alter table device insert column longitude double;

update device set location = '', location_id = 0, latitude = 0, longitude = 0;
```