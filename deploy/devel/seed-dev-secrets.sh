#!/bin/sh
set -eu

# Seeds fake placeholder values for the three ExternalSecrets that can't sync
# from Vault without org-level access (see CLAUDE.md's Okteto migration
# blocker list). Safe to re-run any time, including after `make okteto-deploy`:
# it never touches a Secret that an ExternalSecret has already synced for
# real, and it never deletes/recreates the ExternalSecret objects themselves.
#
# Refuses to run anywhere but your own personal Okteto namespace -- this
# writes deliberately fake credentials and must never touch a shared
# staging/prod namespace.
#
# NSO_PASSWORD (in oscars-backend-secrets) and the NSO-side `oscars` AAA
# user's password (in the nso chart's own add-users secret) are two copies
# of the same credential and MUST match, or oscars-backend gets 401s from
# NSO's RESTCONF API even though every pod looks healthy -- verified live:
# re-seeding NSO's CDB from scratch re-creates the `oscars` AAA user from
# whatever OSCARS_PASSWORD is in that secret at boot time, independent of
# whatever NSO_PASSWORD happens to be set to in oscars-backend-secrets.

ctx=$(okteto context show -o json)
ns=$(printf '%s' "$ctx" | jq -r '.namespace // empty')
personal=$(printf '%s' "$ctx" | jq -r '.personalNamespace // empty')
if [ -z "$ns" ] || [ -z "$personal" ] || [ "$ns" != "$personal" ]; then
  echo "refusing to run: current Okteto namespace '$ns' is not your personal namespace '$personal'." >&2
  echo "This seeds FAKE placeholder secrets and must never run against a shared namespace." >&2
  exit 1
fi

POSTGRES_PASSWORD_PLACEHOLDER="devel-placeholder"
NSO_PASSWORD_PLACEHOLDER="devel-placeholder"

sync_status() {
  kubectl get externalsecret "$1" -o jsonpath='{.status.conditions[0].reason}' 2>/dev/null || true
}

seed_secret() {
  es_name="$1"; secret_name="$2"; shift 2
  reason=$(sync_status "$es_name")
  if [ "$reason" = "SecretSynced" ]; then
    echo "ok: $secret_name already has real data synced from Vault via $es_name, leaving it alone"
    return 0
  fi
  echo "seeding placeholder $secret_name (ExternalSecret $es_name status: ${reason:-missing})"
  kubectl create secret generic "$secret_name" "$@" --dry-run=client -o yaml | kubectl apply -f -
}

# oscars-backend-secrets is the Secret name the backend's ExternalSecret
# (oscars-backend-external-secret) targets -- not the same name as the
# ExternalSecret object itself.
seed_secret oscars-backend-external-secret oscars-backend-secrets \
  --from-literal=POSTGRES_PASSWORD="$POSTGRES_PASSWORD_PLACEHOLDER" \
  --from-literal=ESDB_API_KEY=devel-placeholder \
  --from-literal=NSO_USERNAME=oscars \
  --from-literal=NSO_PASSWORD="$NSO_PASSWORD_PLACEHOLDER"

# Here the ExternalSecret and the Secret it owns share a name. That name is
# coupled to okteto.yaml's hardcoded `helm upgrade ocd-stack ...` release name.
seed_secret ocd-stack-oscars-db-external-secret ocd-stack-oscars-db-external-secret \
  --from-literal=POSTGRES_PASSWORD="$POSTGRES_PASSWORD_PLACEHOLDER"

# The nso chart's own add-users secret provisions the real NSO-side AAA user
# OSCARS authenticates as. ADMIN_*/ORCH_* belong to NSO's own admin user and
# to orchestrator (whose own ExternalSecret may already be syncing real Vault
# data) -- only ever touch OSCARS_*, and only replace the whole secret
# (including its templated 20-add_users.xml) when it isn't already synced.
nso_users_es="ocd-stack-nso-external-secret-add-users"
nso_users_secret="ocd-stack-nso-external-secret-add-users"
reason=$(sync_status "$nso_users_es")
if [ "$reason" = "SecretSynced" ]; then
  echo "ok: $nso_users_secret already has real data synced from Vault via $nso_users_es, leaving it alone"
else
  admin_user=$(kubectl get secret "$nso_users_secret" -o jsonpath='{.data.ADMIN_USERNAME}' 2>/dev/null | base64 -d)
  admin_pass=$(kubectl get secret "$nso_users_secret" -o jsonpath='{.data.ADMIN_PASSWORD}' 2>/dev/null | base64 -d)
  orch_user=$(kubectl get secret "$nso_users_secret" -o jsonpath='{.data.ORCH_USERNAME}' 2>/dev/null | base64 -d)
  orch_pass=$(kubectl get secret "$nso_users_secret" -o jsonpath='{.data.ORCH_PASSWORD}' 2>/dev/null | base64 -d)
  if [ -z "$admin_user" ] || [ -z "$orch_user" ]; then
    echo "refusing to seed $nso_users_secret: couldn't read existing ADMIN_*/ORCH_* values to preserve them (secret missing or malformed)." >&2
    exit 1
  fi
  echo "seeding placeholder $nso_users_secret OSCARS_* fields (ExternalSecret $nso_users_es status: ${reason:-missing}), preserving existing ADMIN_*/ORCH_*"
  add_users_xml=$(cat <<XML
<config xmlns="http://tail-f.com/ns/config/1.0">
  <nacm xmlns="urn:ietf:params:xml:ns:yang:ietf-netconf-acm">
    <groups>
      <group>
        <name>ncsadmin</name>
        <user-name>${admin_user}</user-name>
        <user-name>${orch_user}</user-name>
        <user-name>oscars</user-name>
      </group>
    </groups>
  </nacm>
  <aaa xmlns="http://tail-f.com/ns/aaa/1.1">
    <authentication>
      <users>
        <user>
          <name>${admin_user}</name>
          <uid>1000</uid>
          <gid>1000</gid>
          <password>${admin_pass}</password>
          <homedir></homedir>
          <ssh_keydir></ssh_keydir>
        </user>
        <user>
          <name>${orch_user}</name>
          <uid>1001</uid>
          <gid>1001</gid>
          <password>${orch_pass}</password>
          <homedir></homedir>
          <ssh_keydir></ssh_keydir>
        </user>
        <user>
          <name>oscars</name>
          <uid>1002</uid>
          <gid>1002</gid>
          <password>${NSO_PASSWORD_PLACEHOLDER}</password>
          <homedir></homedir>
          <ssh_keydir></ssh_keydir>
        </user>
      </users>
    </authentication>
  </aaa>
</config>
XML
)
  kubectl create secret generic "$nso_users_secret" \
    --from-literal=ADMIN_USERNAME="$admin_user" \
    --from-literal=ADMIN_PASSWORD="$admin_pass" \
    --from-literal=ORCH_USERNAME="$orch_user" \
    --from-literal=ORCH_PASSWORD="$orch_pass" \
    --from-literal=OSCARS_USERNAME=oscars \
    --from-literal=OSCARS_PASSWORD="$NSO_PASSWORD_PLACEHOLDER" \
    --from-literal=20-add_users.xml="$add_users_xml" \
    --dry-run=client -o yaml | kubectl apply -f -
  echo "note: NSO only re-reads 20-add_users.xml into its AAA config on boot -- if NSO is already running, restart its pod for the oscars user's new password to take effect."
fi

echo "done -- safe to re-run any time (e.g. after 'make okteto-deploy'); becomes a no-op once Vault access works."
