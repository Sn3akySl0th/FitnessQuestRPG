# Git Branch and Release Governance

## Source of truth

Before any Git operation, inspect the current repository state rather than relying on prior chat context:

```powershell
git status --short
git branch --show-current
git log --graph --decorate --oneline --all -n 30
git fetch origin --prune
git status -sb
```

Never claim the working tree is clean if `git status --short` has output.

Never commit, merge, rebase, push, tag, delete a branch, or create a release unless the user explicitly asks for that action in the current conversation.

## Branch purposes

| Branch or ref | Purpose | Rules |
|---|---|---|
| `main` | Integration branch and remote-backed source for internal testing | Keep stable. Merge approved work here only after validation. Do not force-push. Do not make speculative changes directly on `main`. |
| `feature/*` | Isolated feature development | One focused change set per branch. Keep the branch intact after merge unless the user explicitly authorizes deletion. Merge into `main` only after review and validation. |
| `release/*` | Stabilization branch for a named testing or release phase | Do not add new features without explicit approval. Allow only targeted bug fixes, release configuration, validation, and documentation changes. |
| `pre-internal-progression-merge` | Rollback point immediately before the progression/schema v30 merge | Treat as immutable. Never move, delete, retag, or overwrite it. |
| `pre-release-freeze` | Historical rollback/freeze point before release hardening | Treat as immutable. Never move, delete, retag, or overwrite it. |
| `pre-closed-beta-freeze` | Historical rollback/freeze point before closed-beta stabilization | Treat as immutable. Never move, delete, retag, or overwrite it. |
| `feature/progression-schema-v30` | Preserved source branch for the progression/schema v30 implementation | Treat as preserved. Do not delete, force-push, or repurpose it without explicit authorization. |

## Current protected history

As of the progression integration:

- `main` / `origin/main`: merge commit `7e7ffc2` — `merge: progression schema v30 for internal testing`
- `feature/progression-schema-v30`: `3342ff2` — `feat(progression): gear reforge, procedural combat traits & Room schema v30`
- `pre-internal-progression-merge`: `ad5d1f0` — `merge: closed-beta stabilization and UX safeguards`
- `pre-release-freeze`: `e186ea7`
- `pre-closed-beta-freeze`: `e885d8c`

These SHAs are historical reference points, not permission to rewrite history.

## Required preflight for changes

Before editing code:

1. Run `git status --short` and report any existing modifications or untracked files.
2. Confirm the current branch and its relation to `origin`.
3. Do not mix unrelated changes into an existing merge, feature branch, or release branch.
4. Do not stage generated files, local configuration, credentials, screenshots, logs, APKs, or scratch files unless explicitly requested.
5. If a merge is in progress, do not commit until:
   - all conflicts are resolved,
   - `git diff --staged --check` is clean,
   - staged-file scope has been reviewed,
   - the user explicitly approves the merge commit.

## Required preflight for commits

Before proposing a commit:

```powershell
git status
git diff --check
git diff --cached --check
git diff --staged --name-status
git diff --staged --stat
```

Report:

- Modified, added, deleted, and untracked files.
- Whether any merge or rebase is in progress.
- Whether whitespace checks are clean.
- The exact commit message proposed.
- Whether the commit includes only the requested scope.

Wait for explicit approval before running `git commit`.

## Merge protocol

For merges into `main`:

1. Fetch remote state first: `git fetch origin --prune`.
2. Confirm `main` is clean and synchronized or explain the divergence.
3. Create a rollback tag only if explicitly requested.
4. Merge without rewriting published history.
5. Resolve conflicts minimally; preserve both independently required behaviors.
6. Re-run staged checks and relevant tests.
7. Ask for explicit approval before creating the merge commit.
8. After committing, verify:
   - merge commit SHA,
   - `git status --short`,
   - graph history,
   - feature branch preservation,
   - rollback-tag integrity.

Never use `git reset --hard`, `git push --force`, `git rebase` on published branches, or branch/tag deletion without explicit approval.

## Push protocol

A push is a separate approval step from a commit.

Before requesting push approval, run:

```powershell
git log --oneline origin/main..main
git merge-base --is-ancestor origin/main main
git status --short
```

Report every commit that will be published, oldest to newest.

Push only after explicit approval. Use normal fast-forward push only:

```powershell
git push origin main
```

Never use `--force`, `--force-with-lease`, or tag pushes unless explicitly authorized.

After pushing, verify:

```powershell
git status -sb
git ls-remote --heads origin main
```

## Build and release protocol

Building, signing, version changes, tag creation, and publishing are separate actions. Do not treat a successful merge or push as authorization to release.

Before any release build:

1. Confirm the requested variant and target platform.
2. Confirm version name and version code.
3. Confirm the exact tests/build tasks to run.
4. Confirm the workspace is clean.
5. Ask for explicit approval before creating tags, signing artifacts, or publishing.

Do not create release tags, GitHub releases, Play artifacts, APKs, AABs, or changelog entries without explicit authorization.

## Handling accidental files

If an unexpected untracked file appears:

1. Do not stage it.
2. Identify its exact path and inspect its contents safely.
3. Report what it contains.
4. Ask before deleting, moving, or adding it to `.gitignore`.
5. After approved cleanup, verify `git status --short` is empty.

## Reporting standard

For every Git operation, report only facts verified by command output. Include:

- Current branch and HEAD SHA.
- Clean/dirty status, including untracked files.
- Exact commands run.
- Commit, merge, push, tag, or release SHA/identifier when applicable.
- What was deliberately not done, such as “no push,” “no force push,” or “no release artifacts created.”