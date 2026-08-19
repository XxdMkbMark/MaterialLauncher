# Git Commit Message Convention

Format follows **Conventional Commits 1.0**. All messages in **English**.

## Format

<type>(<scope>): <subject>

<body>


Subject ≤ 50 chars (hard cap 72). Body only when *why* isn't obvious from the diff.

## Types

`feat` · `fix` · `perf` · `refactor` · `docs` · `test` · `build` · `ci` · `chore` · `revert`

Breaking change: append `!` after type/scope — `feat(api)!: drop legacy login`.

## Scope (optional)

Lower-case module: `auth`, `asset`, `library`, `http`, `launch`, `ui`, `backend`, `build`, `deps`, `tests`. Omit if it spans more than two areas.

## Subject rules

1. Imperative, present tense — "add", not "added".
2. No trailing period.
3. Lower-case first letter.
4. Specific — *what*, not *how*.

## Two-tier workflow

**Local commits (reading / experimenting):** don't owe this convention anything. Use a throwaway alias:

```
git config alias.wip '!git add -A && git commit -m'
git wip "read InstanceStore ctor"
```

These stay local and never get pushed.

**Shared commits (push / PR):** before pushing, `git rebase -i <last-good>`, squash WIPs, rewrite subjects per the 4 rules above. Skip the body unless the *why* matters.

## Anti-patterns

1. No `WIP` / `tmp` / `update` / `fix bug` in published subjects — squash and rewrite before push.
2. Don't mix a bug fix into a refactor commit — split them.

## Validation

No CI enforcement. Optionally check before push:

```
npx commitlint --from=<last-good> --to=HEAD
```