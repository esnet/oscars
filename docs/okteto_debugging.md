# Debugging `oscars-backend` on Okteto

This covers two things:
1. Getting your own personal Okteto namespace so you aren't stepping on anyone else's deployment.
2. Attaching a remote debugger (IntelliJ, VSCode, or NetBeans) to `oscars-backend` running there, with
   hot-reload so you don't need to rebuild/redeploy on every code change.

See `CLAUDE.md`'s "Okteto/Kubernetes migration" section for the broader migration status and
blockers (Vault secrets, etc.) — this doc assumes those are either already resolved for your
namespace, or you're using scratch/demo secret values for a personal sandbox (never do this in a
shared staging/prod namespace).

## 1. Get your own Okteto namespace

Install the Okteto CLI if you don't have it (`brew install okteto` on macOS), then authenticate
against the cluster:

```bash
okteto context use https://okteto.ocd-staging.es.net
```

This opens a browser login and creates a personal namespace for you automatically (usually your
username). Confirm which namespace you're pointed at:

```bash
okteto context show
```

If you need a second namespace, or someone else already created one for you under a different
name:

```bash
okteto namespace create <name>   # creates and switches to it
okteto namespace use <name>      # switches to an existing one
```

Everything below assumes your current Okteto context/namespace is your own personal one, not a
shared namespace someone else is actively using.

## 2. Deploy the stack into your namespace

From the repo root:

```bash
make okteto-deploy
```

(equivalent to running `okteto deploy` directly — the `make` target is just a documented shortcut,
see the Makefile's own `make help` for all available targets, Okteto and otherwise)

This builds and pushes the production `oscars-backend` image (per `okteto.yaml`'s `build.oscars-backend`
key) and runs the `helm upgrade ocd-stack ...` command in `okteto.yaml`'s `deploy.commands`, which
brings up the full `ocd-stack` umbrella (NSO, ESDB, orchestrator, etc.) alongside `oscars`/`oscars-db`
in your namespace.

First boot is slow — NSO in particular takes several minutes to cold-start (loading ~480 NED
packages with no warm cache). Check pod status with:

```bash
kubectl get pods
```

If `oscars-backend-external-secret` or `ocd-stack-oscars-db-external-secret` show `SecretSyncedError`
in `kubectl get externalsecret`, Vault doesn't have real values populated for your identity yet (see
CLAUDE.md's blocker list) — this is expected for most new developers on day one, since the Vault
keys involved require org-level access. Run:

```bash
make okteto-seed-secrets
```

This creates `oscars-backend-secrets` (keys: `POSTGRES_PASSWORD`, `ESDB_API_KEY`, `NSO_USERNAME`,
`NSO_PASSWORD`) and `ocd-stack-oscars-db-external-secret` (key: `POSTGRES_PASSWORD`, matching the
first so backend↔DB auth actually works) with safe, clearly-fake placeholder values — but only for
whichever of the two hasn't already synced real data from Vault, so it never overwrites a real
secret. It refuses to run against anything but your own personal Okteto namespace, and it's safe to
re-run any time (e.g. after `make okteto-deploy`, which otherwise re-applies the still-failing
`ExternalSecret` on every redeploy) — once Vault access works for your identity, this becomes a
no-op and your real synced secrets are left untouched. **Never do this against a shared staging/prod
namespace** — see the project's own no-placeholder-secrets convention.

## 3. Enter dev mode with debugging + hot-reload

```bash
make okteto-up
```

This swaps the running `oscars-backend` pod for a dev-mode container (built from the `dev` stage in
`deploy/devel/backend.dockerfile`, via the `oscars-backend-dev` build key in `okteto.yaml`) that:

- Runs `deploy/devel/dev-entrypoint.sh`, which starts the app via `mvn spring-boot:run` with the JDWP
  debug agent attached on port **9201** (`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:9201`
  — the same flag `docker-compose.yml` uses for local dev, so debugging feels the same whether you're
  local or in Okteto).
- Syncs your local `backend/src` into the container live. An `entr`-driven loop recompiles on every
  `.java` save, and `spring-boot-devtools` (on the classpath in this dev image only — excluded from
  the real production jar, see CLAUDE.md) picks up the recompiled classes and restarts the Spring
  context in-place, without a full JVM/pod restart.
- Forwards three ports to your machine: `9201` (JDWP debug), `8201` (web API), `8211` (actuator —
  `curl http://localhost:8211/actuator/health` to confirm the app is up).

Leave this running in its own terminal. Edit files under `backend/src/...` normally; watch the
`okteto up` log stream for `mvn compile` output and a devtools restart message after each save.

**Known rough edge**: `spring-boot-devtools`' restart-classloader mechanism hasn't been validated
against this app's CXF-generated SOAP proxy classes or the OpenTelemetry auto-instrumentation
starter. If you see restarts hang, throw `ClassNotFoundException`, or just not pick up changes, that's
the suspected cause — file an issue/ping the Okteto migration work, and in the meantime you can get a
slower-but-reliable loop by killing `okteto up` and running it again (full JVM restart) after each
change instead of relying on the in-place restart.

To exit dev mode and restore the normal pod:

```bash
make okteto-down
```

## 4. Attach your IDE's debugger

All three IDEs below connect to **`localhost:9201`** (the forwarded JDWP port) using a remote/socket
attach — none of them need anything installed in the cluster; the debug agent is already running
inside the container via `dev-entrypoint.sh`.

### IntelliJ IDEA

1. Run → Edit Configurations → **+** → Remote JVM Debug.
2. Host: `localhost`, Port: `9201`. Leave the rest at IntelliJ's defaults (it auto-fills the correct
   `-agentlib:jdwp` command-line, matching what's already running — you don't need to copy it
   anywhere, that's just FYI for comparison).
3. Run this configuration in Debug mode. It should connect immediately since the JVM is already
   listening (`server=y`, `suspend=n` — it never blocks waiting for you).
4. Set breakpoints in `backend/src/main/java/...` as usual.

### Visual Studio Code

Requires the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
(provides the `java` debug type used below). `.vscode/` is gitignored in this repo, so there's no
shared committed config to copy — add this configuration to your own local
`.vscode/launch.json` (or create the file if it doesn't exist):

```json
{
    "type": "java",
    "name": "Attach to Okteto (oscars-backend)",
    "request": "attach",
    "hostName": "localhost",
    "port": 9201,
    "projectName": "backend"
}
```

Run it from the "Run and Debug" panel. Breakpoints in `backend/src/main/java/...` will hit the same
way they would for a local launch config.

### NetBeans

1. Debug menu → Attach Debugger...
2. Connector: **Socket Attach**, Transport: `dt_socket` (auto-filled), Host: `localhost`, Port: `9201`.
3. Click OK. NetBeans attaches and shows the debug toolbar once connected.
4. Set breakpoints in the `backend` project's sources as usual.

## 5. Common gotchas

- **Port 9201 already in use locally**: you likely have a local (non-Okteto) `oscars-backend` running
  via `docker-compose.yml` or `bin/start.sh` with the same debug port forwarded/bound. Stop one or the
  other before attaching.
- **Debugger connects but breakpoints never hit**: confirm you're editing the same `backend/src` tree
  `okteto up` is syncing from (i.e. this checkout, not a different worktree or clone) — the running
  classes come from whatever got synced and recompiled in the container, not from wherever your IDE
  thinks the "real" source lives.
- **A fresh dependency you just added to `pom.xml` fails to resolve inside the dev container**: the
  dev image's Maven cache is pre-warmed only with what was resolved at image-build time; the
  in-container `mvn compile`/`spring-boot:run` loop is *not* run with `--offline`, so it should fetch
  new dependencies on its own as long as the Okteto namespace has network egress to Maven Central /
  `gitlab.es.net`. If that egress is locked down in your cluster, you'll need to re-run
  `make okteto-build-dev` after changing `pom.xml` instead.
- **This all works with a scratch/demo secrets setup, but real deploys still need real Vault
  values** — see CLAUDE.md's "Okteto/Kubernetes migration" section for the current blocker list.
