# TESTING

Testing suite(s) for `oscars/`

## Back-end (JUnit, Cucumber)

Run the following docker compose service to run tests on the oscars-backend.

```shell
docker compose build --progress=plain --no-cache oscars-backend-test
```

Alternatively, we can also run `docker build` directly by using the following command:

```shell
docker build -t oscars-backend --progress=plain --no-cache --target=test --build-arg JAVA_OPTS="-XX:UseSVE=0" --build-arg MAVEN_OPTS="-XX:UseSVE=0" -f ../oscars/deploy/devel/backend.dockerfile .
```

And lastly, if you prefer to run `mvn test` directly, you can simply run the following from within the `backend/` directory

```shell
# Run all tests
mvn test

# Run only tests with @RestNsoSync tag
mvn test -Dcucumber.filter.tags="@RestNsoSync"

# Test all tests with tag @RestNsoSync, but exlcude some other tag(s)
mvn test -Dcucumber.filter.tags="@RestNsoSync and not @Unit"
```

## Load testing

A JMeter-based load test (`backend/src/test/resources/load-testing.jmx`) can be run locally against a dockerized `oscars-backend`:

```shell
make loadtest
```

This builds and runs `docker-compose.loadtest.yml`'s `oscars-load-test` service, which spins up `oscars-db` + `oscars-backend` (with `NSO_SYNC` disabled, so southbound NSO calls are stubbed out, and `AUTH_OAUTH_ENABLED=false` so load-testing doesn't require minting JWTs) and runs the JMeter plan against it. Results (a CSV and an HTML report) land in `./backend/load-testing/`.

The same job is also available as a manual-only `backend:loadtest` stage in GitLab CI — it never runs automatically; trigger it from the pipeline UI when you want results.

The plan runs three concurrent scenarios, each paced with a configurable think-time delay so they don't starve each other's server threads:
- **NSI Reserve** — the original NSI SOAP `reserve()` smoke test, now parameterized per iteration from `backend/src/test/resources/load-testing-pairs.csv` instead of a single hardcoded STP pair.
- **PCE Paths** — `POST /api/pce/paths`, exercising path computation (`YenkEngine`) over the full topology.
- **REST Hold and Commit** — the `generateId` → `hold` → `commit` → `release` REST flow. The CSV only has a handful of device/port/VLAN pairs, so at higher concurrency some `hold` calls will legitimately fail with "vlan not available" when two threads race for the same pair — that's expected contention, not a bug.

Override defaults with JMeter `-J` properties, e.g.:

```shell
docker compose -f docker-compose.loadtest.yml run --rm oscars-load-test \
  jmeter -n -t ./load-testing.jmx -Jhost=oscars-backend \
  -JthreadCount=10 -Jduration=120 -JrampTime=10 -JthinkTimeMs=100 \
  -l /app/load-testing/results.csv -e -o /app/load-testing/report
```

### Server-side metrics during a load test

`oscars-backend` exposes Actuator/Micrometer metrics on a separate management port (`8211` by default, override with `OSCARS_BACKEND_MANAGEMENT_PORT`) so they never share the `/api`/`/protected`/`/admin` security filter chains and aren't reachable on the main web port at all:

- `http://localhost:8211/actuator/health`
- `http://localhost:8211/actuator/prometheus` — Prometheus-formatted metrics: JVM/GC, HikariCP connection pool, Caffeine cache sizes, per-endpoint `http_server_requests_seconds` timings, and two OSCARS-specific timers:
  - `oscars_pce_calculate_paths_seconds` / `oscars_pce_calculate_all_paths_seconds` — time spent in `PceService`'s path computation (`YenkEngine`), the likely CPU-heaviest code path.
  - `oscars_nso_process_task_seconds{commandType="BUILD|DISMANTLE|REDEPLOY"}` — time spent in `NsoAdapter.processTask` compiling and (dry-run) pushing southbound config.

During a `make loadtest` run, scrape `/actuator/prometheus` (e.g. with `watch curl` or a local Prometheus instance) to correlate these with the JMeter report for the same time window. In production/staging this traffic can also be sent to the existing OpenTelemetry/Stardust pipeline by flipping `otel.sdk.disabled=false` and uncommenting the exporter properties in `backend/config/application.properties` for a given run.