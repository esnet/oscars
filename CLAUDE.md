# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

OSCARS (On-demand Secure Circuits and Advance Reservation System) is ESnet's network circuit reservation system. It validates and holds network resources (ports/VLANs/bandwidth), computes paths across the topology, and provisions circuits on network devices via Cisco NSO.

Two main modules:
- **`backend/`** — Spring Boot (Java 25) application: reservation logic, path computation, topology, southbound provisioning, REST/GraphQL/SOAP APIs. Actively developed; this is the system of record for L2VPN/reservation logic.
- **`frontend/`** — React 16 + MobX + webpack 4 web UI (`frontend/README.md` has frontend-specific build/IDE notes). **Legacy and effectively frozen** (last commit Sep 2025, vs. ongoing backend work) — superseded by the "LSP Builder" module in the sibling `../es-enterprise` monorepo (Next.js/React 18, under `apps/ese-web/src/pages/oscars`), part of the "OSCARS 2.0" initiative. That app talks to this repo's `oscars-backend` REST API directly (gated by `OSCARS_ENABLED`/`NEXT_PUBLIC_OSCARS_ENABLED` feature flags and `OSCARS_GROUPS` JWT group membership) — it does not replace or call into this repo's `frontend/`. New frontend work should generally go there, not in `frontend/`, unless told otherwise.

Local development uses Docker Compose via a separate `ocd-stack` sibling repo (expected at `../ocd-stack`), referenced by the top-level `Makefile`. OrbStack is used instead of Docker Desktop.

### Local dev via ocd-stack

`ocd-stack` is ESnet's shared Docker Compose orchestration repo (no app code of its own) that wires together Postgres, Redis, Elasticsearch, ESDB, Orchestrator, NSO, Namesurfer, Prefix Manager, and OSCARS as sibling checkouts. The oscars `Makefile` pulls in `ocd-stack/makefiles/base.mk` and scopes it with `--profile oscars`.

- In local dev, `oscars-backend` builds from this repo using `deploy/devel/backend.dockerfile`; `oscars-frontend` builds from `frontend/` using `deploy/devel/frontend.dockerfile` and bind-mounts `frontend/src`/`frontend/public` for hot reload.
- `oscars-backend` depends on `oscars-db` and `nso` being up, and talks to `esdb` — bringing up a narrower `oscars_only` profile without NSO/ESDB live will not fully function if you're exercising southbound/ESDB-backed features. Use the `oscars` (or `all`) profile for that.
- Backend env comes from `ocd-stack/config/oscars/backend.env` plus shared `private.env`/`config/base.env`/`config/otel.env` in ocd-stack (not from this repo's `.env`/`example.env`, which are for the standalone `docker-compose.yml` in this repo).
- Common targets (from either repo, once wired up): `make start` / `make stop` / `make restart` / `make logs` / `make ps` / `make recreate` (destroys volumes) / `make renew` (pull+rebuild+recreate); ocd-stack also provides `make oscars-psql` to open a psql shell into `oscars-db`.
- The `topo-net` external Docker network referenced in this repo's `docker-compose.yml` is declared here, not created by ocd-stack — ensure it exists (`docker network create topo-net`) before bringing services up standalone.
- `ocd-stack`'s own `make setup` expects `../oscars` to be a sibling checkout (cloned from `git@github.com:esnet/oscars.git`).

### Okteto/Kubernetes migration (in progress)

This repo, along with its sibling repos, is being migrated off the `ocd-stack` Docker Compose workflow onto [Okteto](https://www.okteto.com/docs/)-managed Kubernetes. Tracked in Jira under epic **PLATFORM-1013** ("Deploy Okteto for OSCARS"), child stories **OCD-926** ("K8: OSCARS can be deployed via chart"), **OCD-927** ("K8: OSCARS can be developed via Okteto"), **OCD-924** ("K8: OSCARS 'make' command parity"). Two earlier attempts at the same epic, **POR-109**/**POR-110**, were cancelled before `PLATFORM-1013` was created; an earlier dev-only investigation (**OS-377**, May 2025, branch `os-377-oscars-okteto`) predates the current Helm-chart convention (raw `kubectl apply` of hand-written manifests) and isn't a starting point to resume from.

**Current state**: `okteto.yaml` exists at this repo's root (on branch `migrate-oscars-okteto`), and `oscars`/`oscars-db` Helm charts exist in `../ocd-charts` (on branch `feat/OCD-926-oscars-subchart` off `claude-enabled`, following that repo's current per-chart-standalone-renderable convention — see its own `CLAUDE.md`/`docs/values-cheatsheet.md` for the chart-tier/values-layering rules). Both are unmerged as of this writing. **Blocking prerequisites before this actually works end-to-end**:
1. Vault key `oscars` needs `POSTGRES_USER`/`POSTGRES_PASSWORD`/`ESDB_API_KEY` populated.
2. Vault key `nso` needs `OSCARS_USERNAME`/`OSCARS_PASSWORD` added (alongside existing `ADMIN_USERNAME`/`ORCH_USERNAME`), **and** a companion edit to `ocd-charts/charts/nso/templates/add-users-externalsecret.yaml` to actually provision an NSO AAA user for those credentials — this touches a chart OSCARS doesn't own and needs sign-off from whoever owns the `nso` chart.
3. `TOPO_OSCARS_URL`/`topo.url` is intentionally **not** carried into the new chart — it's dead config (`TopoProperties.getUrl()` is never called anywhere in the backend); the chart instead runs with `startup.standalone=false`, which needs live NSO/ESDB reachability (both deployed in the same `ocd-stack` umbrella) rather than a bundled `topology.json`.

Any staging/production deploy automation work done before this lands (e.g. the Ansible/AAP scaffold under `deploy/`, modeled on `esnet-esdb/docs/release_process.md`'s pattern) should be treated as provisional, since this Okteto/Helm path is the one actually intended to replace it.

## Common commands

### Backend (run from `backend/`)

```bash
# Build without running tests
mvn -DskipTests package

# Run all tests (JUnit + Cucumber BDD)
mvn test

# Run only tests tagged @RestNsoSync
mvn test -Dcucumber.filter.tags="@RestNsoSync"

# Run all tests except a given tag
mvn test -Dcucumber.filter.tags="@RestNsoSync and not @Unit"

# Install (build + test) into local m2 repo
mvn install

# First-time DB setup (requires local Postgres running)
./bin/install_db.sh

# Start the backend directly (non-docker)
./bin/start.sh
```

Cucumber test profiles are defined in `backend/pom.xml` via Maven profiles (`unit`, `wip`, `noTest`), each setting `cucumber.options` (default tag filter is `@unit --strict`). Feature files live under `backend/src/test/resources/net/es/oscars/cuke/*.feature`; step definitions are in `backend/src/test/java/net/es/oscars/cuke/`. The Cucumber JUnit runner is `CucumberTest`, context-configured through `AbstractBackendTest`.

Via Docker (matches CI):
```bash
docker compose build --progress=plain --no-cache oscars-backend-test
```

### Frontend (run from `frontend/`)

```bash
npm install
npm run start     # webpack-dev-server with hot reload
npm run build     # production webpack build
npm run lint      # eslint --fix on src/
npm run format    # prettier then eslint
npm test          # mocha test/spec.js
```

### Versioning

When bumping the project version, update it in **all** of:
- top-level module `pom.xml` files (backend, and any nsi/pss/shared/migration modules if present)
- `frontend/package.json`

(`frontend/package-lock.json` is regenerated automatically by `mvn package` in the frontend module — don't hand-edit its version.)

## Architecture (backend)

Request flow: **Web API** (REST v2 / GraphQL / "simple" API) → **`resv.svc.ConnService`** (reservation lifecycle) → **`pce.PceService`** (path computation) → **`sb.nso`** southbound layer → **Cisco NSO** (device provisioning). Topology state is populated from ESDB and reconciled against live NSO state by scheduled jobs in `task/`.

- **`resv/svc/ConnService`** — the central service almost everything depends on. Owns the connection/reservation state machine (hold → commit → release/archive) over the junction/fixture/pipe data model, validates schedule/bandwidth/VLAN/project-id constraints (`resv/svc/validators`, `resv/svc/comparisons`), and tracks southbound deployment state. Used directly by REST, GraphQL, NSI, and scheduled tasks.

- **`pce/`** (Path Computation Engine) — `PceService` is the facade: given an A/Z pair, bandwidth, and schedule, it reads baseline + currently-available topology from `TopologyStore`/`ResvService` and delegates graph search to the `Engine` interface. `YenkEngine` runs Yen's k-shortest-paths (via jgrapht) over a graph built by `YenkGraphFactory` from the live `Topology`. `PceLibrary` holds static graph/ERO helper functions. The PCE only computes candidate paths (shortest, least-hops, fits-bandwidth, widest, etc.) — it never holds resources.

- **`sb/nso/`** (southbound NSO integration) — `NsoAdapter` translates an OSCARS `Connection` into Cisco NSO service model objects (LSPs, VPLS, dismantle/redeploy payloads) and reads current NSO-side service inventory. `NsoProxy` is the low-level RESTCONF/YANG client that talks to the real NSO server. `sb.SouthboundQueuer`/`SouthboundTask`/`SouthboundPeriodicChecker` dispatch provisioning work asynchronously and track `DeploymentState` on connections. In short: `ConnService` decides *what* to deploy, `NsoAdapter` builds the payload, `NsoProxy` pushes it.

- **`esdb/ESDBProxy`** — GraphQL client to ESDB, ESnet's external equipment/inventory database (NOT a local DB). Feeds org, equipment/interface, VLAN, and bandwidth-utilization data into OSCARS's topology. The ESDB GraphQL schema/query documents are managed via `backend/graphql.config.yml` and `backend/src/main/resources/graphql-documents/*.graphql` (distinct from OSCARS's own served GraphQL schema at `backend/src/main/resources/graphql/schema.graphql`).

- **`topo/`** — topology domain model and services. `topo.svc.TopologyStore` is the authoritative in-memory topology cache (baseline + current availability) queried by PCE and `ConnService`; `topo.pop` builds/loads topology from config and checks consistency.

- **Scheduled tasks (`task/`)**, all `@Scheduled`:
  - `RefreshTopology` — periodically repopulates topology and evicts topology caches.
  - `TransitionStates` — drives the connection state machine: expires stale HELD connections, archives/dismantles RESERVED connections past end time, times out stale NSI requests.
  - `EsdbDataSync` — syncs bandwidth-utilization/VLAN data from ESDB into OSCARS state.
  - `NsoSyncReport` — compares live NSO state against expected RESERVED connections and reports discrepancies.
  - `EmptyConnectionListCache` — evicts the connection-list cache.

- **Web API surfaces (`web/`)**:
  - `web/rest/` and `web/rest/v2/` — REST controllers under `/api/...` (public), `/protected/...` (JWT required), `/admin/...` (admin JWT required). v2 adds `EseApiController` (ESE/L2VPN API), `TopoSearchController`, `SenseController` (SENSE event API).
  - `web/graphql/` — Spring GraphQL controllers (e.g. `ConnListController`) serving OSCARS's own schema.
  - `web/simple/` — a reduced-complexity DTO/API (`SimpleConnection`, `Pipe`, `Fixture`, `Junction`) as an alternative to the full junction/pipe/fixture REST model.

- **`nsi/`** — implements the OGF NSI (Network Service Interface) protocol so OSCARS can act as an NSI provider agent for interop with other R&E network NSAs. `nsi/svc` handles SOAP reserve/commit/abort/provision/release/terminate/query requests and delegates to `ConnService`; `nsi/ent` and `nsi/db` persist NSI-to-OSCARS connection ID mappings.

### Domain model (see `docs/API.md` for full request/response examples)

- A **junction** is a device (router/switch); a **fixture** is a customer-facing port+VLAN endpoint belonging to a junction; a **pipe** is an inter-junction tunnel carrying an **ERO** (Explicit Route Object).
- An ERO is an alternating device/port list (length ≡ 1 mod 3): `A, A:port, B:port, B, B:port, ..., Z:port, Z`.
- Reservation workflow: `POST /protected/hold` (repeatable — reposting with the same `connectionId` overwrites the prior hold; returns `heldUntil`, default a 15-minute hold) → `POST /protected/conn/commit`. Pathfinding (`POST /api/pce/paths`) is optional and doesn't hold anything itself.
- Auth: JWT via `POST /api/account/login`, passed as the `Authentication` header. Path prefix determines required privilege: `/api/` public, `/protected/` token required, `/admin/` admin token required.

## Load testing, profiling, and metrics

- `make loadtest` runs a JMeter-based load test (NSI SOAP reserve, PCE pathfinding, REST hold/commit/release) against a dockerized `oscars-backend`. See `TESTING.md` for scenario details and tuning via `-J` properties (thread count, duration, ramp time, think time). Also available as a manual-only `backend:loadtest` GitLab CI stage.
- `docker-compose.loadtest.yml`/`docker-compose.profiling.yml` run `oscars-backend` with `SPRING_PROFILES_ACTIVE=default` + `AUTH_OAUTH_ENABLED=false` (not `test` — `SecurityConfig` is `@Profile("default")` only, so `test` disables it entirely and Spring Security's default auto-config redirects every request to OAuth login). Keep this in mind if touching these compose files.
- `docker-compose.profiling.yml`'s `oscars-backend-profile` service runs JFR profiling, executing numbered scripts under `backend/src/test/profiling/` during the recording window; `500_loadtest.sh` runs the same JMeter plan there so a single profiling run produces both a JFR recording and a JMeter report together in `./backend/profiling/`. See `PROFILING.md`.
- `oscars-backend` exposes Actuator/Micrometer metrics (health, JVM/GC, HikariCP, per-endpoint timings, plus custom `oscars.pce.*`/`oscars.nso.*` timers) on a separate management port (`8211` by default, `management.server.port` in `config/application.properties`) — deliberately isolated from the `/api`/`/protected`/`/admin` security filter chains in `SecurityConfig`.
- `backend/src/main/java/net/es/oscars/topo/pop/TopoPopulator.java` reads `config/topology.json` via a `file:`-prefixed `@Value` `Resource` (not `ClassPathResource` — that file is copied onto disk next to the jar at build time, never packaged into it or placed on the classpath). Follow this same pattern (see `TopoGenerator`'s `customBwResource`) for any future file reads under `config/`.

### Staging deploy (provisional, not yet working)

`deploy/` has a skeleton Ansible setup (`ansible.cfg`, `staging` inventory, `deploy_staging_west.yml`, an empty `roles/oscars_deploy/` placeholder) and a `make deploy-staging` target, modeled on `esnet-esdb`'s working staging-deploy pattern. **This is not wired up to a real AAP job template and the role itself is an unimplemented placeholder** — two things block completing it:
1. OSCARS's actual target deployment mechanism on `oscars-staging.es.net` (docker-compose pulling the `backend:publish`-tagged image is the natural fit, but unconfirmed) — this determines the whole shape of `oscars_deploy`.
2. Whether registering a new AAP job template in barad-dur is self-service or requires an infra/INF team — not documented anywhere found so far.

Also note the Okteto migration above: `PLATFORM-1013`/`OCD-926` are actively planned (Highest priority, `Ready`) even though not yet started — if that work lands first, staging deploys may go through Okteto/Helm instead of this AAP/Ansible path.

## Contribution conventions

- Commit messages and PR titles use [Conventional Commits](https://www.conventionalcommits.org/): `type: [OS-XXX] Short description`, where `type` is one of `build|ci|chore|docs|feat|fix|perf|refactor|revert|style|test`, `OS-XXX` is the Jira issue, and breaking changes are prefixed `BREAKING CHANGE:`.
- PRs are **squash-merged** into `develop`; `develop` → `master` is a normal (non-squash) merge to preserve history.
- Each PR should be based on a Jira issue and developed on its own topic branch; the submitter is responsible for merging once approved.
- Before cutting a release, check `UPDATING.md` for any required config/schema migration steps, and add new ones there when a change requires existing installs to update config or the DB schema.
