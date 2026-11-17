---
name: load-testing-infra
description: Resume or extend OSCARS's load-testing/profiling/metrics infrastructure (JMeter, JFR profiling, Actuator). Use when asked to work on make loadtest, docker-compose.loadtest.yml/profiling.yml, backend/src/test/resources/load-testing.jmx, or Actuator/Micrometer metrics, so known fixes and conventions aren't re-discovered.
---

# OSCARS load-testing/profiling/metrics — resume point

Full detail is in `TESTING.md`, `PROFILING.md`, and `CLAUDE.md`'s "Load testing, profiling, and
metrics" section. This skill is the condensed action checklist.

## State

Branch `oscars-testing` (off `develop`, 6 commits, Tiers 1–4a of the original plan all done and
verified; Tier 4b — AAP/Ansible staging deploy — is a deliberately incomplete provisional scaffold,
see `deploy/roles/oscars_deploy/tasks/main.yml`). Check `git log --oneline oscars-testing` before
assuming this needs redoing.

## Two real bugs found and fixed here — don't rediscover them

1. **`docker-compose.loadtest.yml`/`docker-compose.profiling.yml` must use
   `SPRING_PROFILES_ACTIVE: default`, never `test`.** `SecurityConfig` is `@Profile("default")` only;
   under `test` it never registers and Spring Security's default auto-config redirects every request
   (even `/api/**`) to an OAuth2 login flow. Pair with `AUTH_OAUTH_ENABLED: "false"` to also open up
   `/protected/**` for load-test purposes.
2. **`TopoPopulator` reads `config/topology.json` via a `file:`-prefixed `@Value` `Resource`, not
   `ClassPathResource`.** The file is `COPY`'d onto disk next to the jar at build time, never packaged
   into it or placed on the classpath — `ClassPathResource` silently fails there. Follow this same
   `file:` pattern for any new file read under `config/` (see `TopoGenerator`'s `customBwResource` for
   another example).

## Verifying changes (cheap, avoid re-discovering the Docker/Maven setup)

```bash
# Full local JMeter run against a dockerized backend:
make loadtest   # report lands in ./backend/load-testing/

# Faster iteration on a single piece (skip the full load test):
docker compose -f docker-compose.loadtest.yml up -d oscars-db oscars-backend
curl -s http://localhost:8211/actuator/health   # confirm Actuator/profile fix is live
docker compose -f docker-compose.loadtest.yml down

# Full Cucumber suite, matches CI exactly (uses the real JDK 25 image — don't try this with a
# locally-installed JDK, version mismatches cause unrelated compile failures):
docker build --progress=plain --target=test -f deploy/devel/backend.test.dockerfile -t oscars-backend-test-verify .
# then `docker rmi oscars-backend-test-verify` when done to avoid leaving stray images around.
```

A healthy baseline is **61/61 Cucumber tests passing** — if you see a different count after a change,
something in the config/property wiring broke, not the test logic itself (this has been the pattern
every time so far).

## Known-good JMeter scenario design (`backend/src/test/resources/load-testing.jmx`)

- Three Thread Groups (NSI Reserve, PCE Paths, REST Hold/Commit/Release), each with its own
  `ConstantTimer` think-time — **do not remove the think-time**, a zero-delay multi-thread-group test
  exhausts Jetty's thread pool and produces false failures on an otherwise-healthy backend (discovered
  the hard way; see commit history on `oscars-testing` for the original 90% error rate before the fix).
- Device pairs for the PCE/REST scenarios come from `backend/src/test/resources/load-testing-pairs.csv`
  — already validated against `backend/config/topology.json`. Don't invent new device/port/VLAN
  combinations by hand; if more are needed, validate them against a running backend first (hold+commit
  a test reservation) rather than assuming a URN exists in the topology.
- `/protected/hold` requires `connection_mtu` in the payload even though `docs/API.md`'s own examples
  omit it — the backend NPEs without it (a known, unfixed minor bug, not a loadtest issue).
- `/api/pce/paths` requires a non-empty `include` array even though it's documented as optional — same
  category of known, unfixed minor bug.

## Token-efficiency notes for this specific task

- Don't re-run the full docker build cycle to "double check" something already verified in this
  session or recorded here — trust the recorded result unless you've actually changed the relevant file.
- Prefer `docker compose ... up -d` + a single `curl`/`psql` check over a full `make loadtest` run when
  iterating on a config change — the full JMeter run is for final verification, not every edit.
