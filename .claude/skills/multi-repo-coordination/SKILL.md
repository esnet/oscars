---
name: multi-repo-coordination
description: Use whenever a task spans multiple git repositories that other Claude Code sessions (peer agents) are also working in — e.g. a fix that touches a backend repo plus one or more sibling chart/infra/frontend repos, or any time you'd otherwise message a peer session via SendMessage/ListAgents about shared work. Covers discovering peers, building and maintaining a branch map, writing research-backed (not vague) messages to peers, verifying peer claims before trusting them, dividing work across repo boundaries without duplication, and the end-of-task checklist for getting every repo onto its own topic branch and committed — never onto develop/main directly. Trigger this proactively even if the user only says something like "check with the other agent" or "keep going" mid-multi-repo-task; don't wait for the user to ask for "coordination" by name.
---

# Multi-repo coordination

Coordinating a fix across repos that other Claude Code sessions are also touching is a distinct
skill from single-repo implementation work. The failure modes are different: vague questions to
peers come back vague, unverified peer claims get trusted and turn into live bugs, two sessions
edit the same file, or someone ends up committing straight to `develop`/`main`. This skill is the
checklist for avoiding those, refined from real sessions — keep adding to it as new failure modes
show up.

## 1. Discover who's actually involved

Call `ListAgents` before guessing who to talk to. It lists peer sessions by name — these names
often *are* the repo the session is working in (e.g. a session named `esnet-esdb-44` is very
likely sitting in `../esnet-esdb`), but confirm with their first reply rather than assuming.

Don't message a peer about something you haven't checked yourself first. Grep/read the relevant
repo yourself, then open with what you already know — see §3.

## 2. Build and maintain a branch map

As soon as more than one repo is in play, build a table and keep it current — state drifts fast
(branches get created, renamed, committed, pushed) and a stale map causes wasted peer messages.

For each repo involved, check directly rather than asking:
```bash
cd <repo> && git branch --show-current && git status --short
git log --oneline develop..HEAD   # or main..HEAD — confirms it's actually ahead, not just named right
git merge-base --is-ancestor develop HEAD && echo "based on develop"  # confirms it branched off the right place, not an old develop
```

Track, per repo: branch name, base branch, ahead-of-base commit count, committed vs. working-tree-only.
Re-verify this table before declaring a cross-repo task done — don't rely on a peer's earlier
self-report of their branch, confirm it (`git branch --show-current` on their checkout is cheap
and catches silent renames/rebases).

**Shared-checkout hazard**: if a repo's main checkout might be in active use by another session,
use a separate git worktree for your own work there instead of switching branches in place —
see the `okteto-migration` project skill for a concrete example of this pattern. Never
`git checkout` a different branch in a checkout you don't know is idle.

## 3. Write research-backed messages, not vague asks

The single biggest quality lever in this skill. Compare:

- Vague: *"Does esnet-esdb have a dev-seed mechanism?"*
- Research-backed: *"Checked ocd-charts/charts/esnet-esdb — no seed mechanism in the chart. No
  Django fixtures anywhere in esnet-esdb either. Found `es-enterprise`'s `just mirror-prod-db`,
  but that mirrors real prod data gated by a credential most devs won't have. Does esnet-esdb have
  something lighter — a management command, fixture file — for synthetic equipment records?"*

The second gets a precise, actionable answer because it hands the peer your findings instead of
asking them to redo your research. Before sending a cross-repo question:
1. Grep/read the repo(s) you can already access for anything relevant.
2. State what you found (including negative findings — "checked X, found nothing") in the message.
3. Ask the narrowest question that's actually still open.

When a peer's answer changes what you thought you knew, or your own follow-up research
contradicts something you told them minutes ago, send a correction immediately — don't let a
stale claim sit uncorrected in their context. ("Correction to my last message: I checked the live
pod directly and...")

## 4. Verify peer claims before building on them

A peer's "this works" or "I fixed X" describes their intent, not necessarily the verified outcome
— same caveat as subagent reports. Before treating a peer's claim as a green light to proceed:
- If it's a code/config claim, read the actual file they say they changed.
- If it's a "this should work now" claim about a running system, check the running system
  yourself (logs, a direct query, a live exec) rather than taking it on faith.
- If their fix doesn't fully land (e.g. "fixed X" but X only applies in one of two deploy paths),
  say so specifically and precisely — don't just report "still broken."

This isn't distrust of the peer, it's the same discipline as verifying your own work: a plan
executed correctly on paper can still fail against the real system, and the only way to know is
to check.

## 5. Divide work at repo boundaries, explicitly

When a fix needs changes in a repo you don't own (or that a peer is already working in), don't
silently implement it yourself and don't silently assume the peer will. State the division out
loud: *"I'll own the chart-side Ingress in `ocd-charts` since I'm already in that worktree; you
own the `NEXT_PUBLIC_*` build-time wiring in `es-enterprise` since that's your repo."* This also
surfaces disagreements early (a peer may already be mid-edit on the exact file you were about to
touch) rather than after two conflicting diffs exist.

Tell a peer plainly when you're *not* going to do something yourself ("I'm not touching ocd-charts
further today, that's yours") so they don't wait on you.

## 6. End-of-task checklist

Before considering multi-repo work done:
- [ ] Every repo touched is on its own topic branch, branched off the latest `develop`/`main` —
      never committed directly to `develop`/`main`.
- [ ] Ask each peer to confirm their branch name and push it (`git push -u origin <branch>`) if
      they haven't. Don't assume "I made the change" means it's pushed, or even committed.
- [ ] Re-confirm the branch map from §2 one more time — don't trust an earlier snapshot.
- [ ] Summarize the final branch map back to the user in one place (a table), since they're the
      one who'll need it to open PRs/MRs across repos.

## Known gaps / open questions (update as encountered)

- No tooling yet for detecting when two peer sessions are about to edit the same file
  concurrently — currently relies on explicit division-of-labor messages (§5).
- No standard for what to do when a peer is unresponsive — current practice is to proceed
  cautiously on your own repo and leave their piece as a documented follow-up rather than blocking.
