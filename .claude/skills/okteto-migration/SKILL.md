---
name: okteto-migration
description: Resume work on the OSCARS → Okteto/Kubernetes migration (Jira PLATFORM-1013). Use when asked to continue, review, or extend the oscars/oscars-db Helm charts, okteto.yaml, or related Vault/NSO wiring, so the current state doesn't need rediscovering from scratch.
---

# OSCARS Okteto migration — resume point

Full narrative context lives in `CLAUDE.md`'s "Okteto/Kubernetes migration" section and in this
session's memory (`project-oscars-okteto-migration`, `feedback-no-placeholder-secrets-in-staging-prod`).
**Read those first** — this skill is the action checklist, not the explanation.

## Before doing anything: check what's actually true right now

State drifts (branches get pushed, Vault gets populated, MRs get opened). Don't assume the memory
file is current — verify fast:

```bash
cd /Users/aalbino/Projects/oscars && git log --oneline -1 migrate-oscars-okteto && git status --short
cd /Users/aalbino/Projects/ocd-charts-oscars-subchart 2>/dev/null && git log --oneline -1 feat/OCD-926-oscars-subchart && git status --short
# If that worktree doesn't exist anymore, check whether it was merged/cleaned up before recreating it.
```

If a GitHub PR (oscars) or GitLab MR (ocd-charts) already exists for these branches, don't re-plan —
go straight to addressing review feedback or the specific next action.

## Repos and branches involved

See the project-level `multi-repo-coordination` skill for the general discipline (branch-map
tracking, peer messaging, work division) this migration has relied on — this table is the
migration-specific snapshot, not a substitute for re-verifying it live (`git branch --show-current`
+ `git status --short` in each repo; state drifts).

| Repo | Branch | Base | Notes |
|---|---|---|---|
| `oscars` (this repo) | `migrate-oscars-okteto` | `oscars-testing` (itself current with `develop`) | `okteto.yaml` lives here; pushed to origin |
| `ocd-charts` (my worktree) | `feat/OCD-926-oscars-subchart` | `claude-enabled` (NOT `develop`) | `charts/oscars/`, `charts/nso/`, `charts/esnet-esdb/`; use a **separate git worktree**, not the shared checkout — another session/user may be actively using `../ocd-charts`'s main checkout |
| `ocd-charts` (peer's separate checkout, `/Users/aalbino/Projects/ocd-charts`) | `fix/oscars-okteto-namespace-url` | `develop` | es-enterprise's `OSCARS_API` computed-URL wiring — a different branch/base than my worktree above, same repo; don't confuse the two or assume they need to merge together |
| `esnet-esdb` (peer `esnet-esdb-44`) | `feat/esdb-okteto-load-test-data` | `develop` | Their repo's own `okteto.yaml` `LOAD_TEST_DATA` wiring (dev-mode only) — separate from the `ocd-charts`-side `esnet-esdb` chart fix below |

**Do not `git checkout` inside `../ocd-charts`'s primary checkout.** If you need to touch that repo and
don't already have `../ocd-charts-oscars-subchart` (or equivalent) as a worktree, create one:
```bash
cd ../ocd-charts && git worktree add ../ocd-charts-oscars-subchart feat/OCD-926-oscars-subchart
```

## Validating ocd-charts changes (cheap, do this instead of re-deriving Helm conventions)

```bash
cd ../ocd-charts-oscars-subchart
helm dependency update charts/oscars && helm lint charts/oscars
helm dependency update charts/oscars-db && helm lint charts/oscars-db
helm template test-release charts/oscars -n x >/dev/null && echo ok
helm template test-release charts/oscars-db -n x >/dev/null && echo ok
# Full drift check against the rest of the umbrella (only needed after a real content change):
uv sync  # once, if .venv doesn't exist
just chart-baseline --name <name> && <make your edit> && just chart-check --against <name>
```

A clean run shows `BYTE-IDENTICAL every render matches the baseline` for every pre-existing chart
plus your new/changed chart(s) rendering `ok`. Don't re-read `docs/values-cheatsheet.md` or
`docs/migration-runbook.md` end-to-end again unless something doesn't match this skill's summary below —
they're long and mostly already distilled here and in memory.

## Known decisions (don't re-litigate — see memory for the "why")

- Backend-only: no `frontend/` chart/okteto work.
- No Ingress yet.
- `okteto.yaml` `name:` field is `oscars`.
- Probes hit `/actuator/health` on port 8211 (management port, from the `oscars-testing` branch's
  Actuator work — already inherited since `migrate-oscars-okteto` is stacked on it).
- `startup.standalone=false`; no `TOPO_OSCARS_URL` (dead config).
- `oscars-db` Postgres: `15-alpine` (matches `esdb-db`/`orch-db`, not this repo's own compose file).
- `nsi.jks` is wired in (NSI SOAP matters — it's the real SENSE→OSCARS integration path), but **its
  credential must go through Vault/ExternalSecret, never baked into the chart's bundled ConfigMap
  files** — see the no-placeholder-secrets memory before touching this.

## Debugging / hot-reload via `okteto up` (implemented)

`docs/okteto_debugging.md` is the full walkthrough (personal namespace setup, `okteto up
oscars-backend`, IntelliJ/VSCode/NetBeans attach steps) — point developers there instead of
re-explaining it. Day-to-day commands go through `make okteto-deploy`/`okteto-up`/`okteto-down`/
`okteto-build-dev` (see the root `Makefile`), not raw `okteto`/`docker` invocations — this repo
deliberately stayed on `make` rather than adding a `Justfile` (see commit `30098994`'s message; `just`
is reserved for `ocd-charts`' infra-tier chart recipes, not this repo). The short version, for your
own reference:

- `deploy/devel/backend.dockerfile` has a `dev` stage (`FROM builder`) wired up via the
  `oscars-backend-dev` build key in `okteto.yaml` — kept **separate** from the `oscars-backend` key
  used by `deploy.commands`, so a plain `okteto deploy` never ships the heavy Maven/JDK dev image.
- `deploy/devel/dev-entrypoint.sh` runs `mvn spring-boot:run` with the repo's existing JDWP flag
  (port 9201, same string as `docker-compose.yml`) plus an `entr`-driven recompile-on-save loop.
  `spring-boot-devtools` (added to `backend/pom.xml`, `optional=true`, auto-excluded from the
  production fat jar by `spring-boot-maven-plugin`'s `repackage` goal — verified via `jar`
  inspection) picks up recompiled classes and restarts the Spring context in-place.
- Verified end-to-end via direct `docker build`/`docker run` (not yet via a live `okteto up` against
  a real namespace — that's the next thing to confirm if resuming this).
- Known open risk: devtools' restart-classloader hasn't been validated against this app's
  CXF-generated SOAP proxies or the OpenTelemetry auto-instrumentation starter present in the pom.

## Still-open blockers (not resolvable by code/research alone)

1. Vault key `oscars`: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `ESDB_API_KEY`, and the `nsi.jks`
   keystore password/credential once that wiring is finalized. Confirmed directly (2026-11-18):
   the user's own Vault identity has zero access (`default` policy only, explicit `deny` on
   `kv/data/oscars`/`kv/data/nso`) — this needs an actual admin grant, not a config fix. In the
   meantime, `make okteto-seed-secrets` (see below) covers personal sandboxes.
2. Vault key `nso`: `OSCARS_USERNAME`, `OSCARS_PASSWORD` (chart-side wiring already landed and
   verified — see `ec31a43` on `claude-enabled` in `ocd-charts`).
3. Pushing branches + opening PR (GitHub, `oscars`, title `type: [OS-XXX] ...` per
   `docs/pull_request_template.md`) and MR (GitLab, `ocd-charts`) — **target `oscars-testing`, not
   `develop`**, per explicit user instruction. `oscars` has been pushed; `ocd-charts` MR still
   pending as of 2026-11-18.
4. **ESDB equipment state mismatch** (new, 2026-11-18): `generate_netlab --environment virtlab`
   always seeds equipment as "Planned" (hardcoded in esnet-esdb, no override), but OSCARS filters
   for "Production" (`EsdbEquipProxy`, now resolved by name instead of hardcoded UUID — see
   project memory `project-oscars-okteto-migration` for the full fix list — but the name "Production"
   still won't match "Planned"). A personal Okteto namespace's topology will report 0 devices
   until this is separately resolved, even with every other fix in place. Left as a follow-up.

## Full reproducibility fix list (2026-11-18)

NSO seed data, NSO boot-validation ordering, a `NSO_URI` double-`/restconf` bug, a `bbl` seed NPE,
ESDB's `LOAD_TEST_DATA` wiring, OSCARS's hardcoded ESDB UUIDs, and a Java 25→27 bump were all
found and fixed verifying this migration end-to-end in a live namespace (not just `helm template`).
Full details, file-by-file, are in project memory `project-oscars-okteto-migration` — don't
re-derive any of this from scratch, read that memory first if resuming.

## Token-efficiency notes for this specific task

- The Helm chart conventions (chart tiers, `global.*` scope, `svc-suffix` collision trap, NSO/ESDB
  asymmetry) are fully captured in this skill + memory. Re-reading `ocd-charts/CLAUDE.md` or the two
  docs files cover-to-cover again is redundant unless you hit something genuinely not covered here.
- Prefer `helm lint`/`helm template`/`just chart-check` (fast, deterministic) over spawning a research
  subagent to "explore chart patterns" again — that exploration already happened once at real cost.
- If another session (e.g. one working directly in `../ocd-charts`'s main checkout) is involved, check
  `ListAgents` for a peer before re-deriving something they already told you — search this
  conversation's own cross-session messages first if resuming mid-conversation.
