# Git Commit Message Convention

> Internal commit-message standard for this project.
> Format follows **Conventional Commits 1.0** (https://www.conventionalcommits.org/).
> All commit messages must be written in **English**.

## 1. Format

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The **first line is the most important** (50 chars or fewer).
Detailed rules for each part are below.

---

## 2. Allowed Types

Every commit MUST start with one of these types:

| Type       | When to use                                                            | Versioning effect         |
|------------|------------------------------------------------------------------------|---------------------------|
| `feat`     | A new feature visible to the user                                      | minor bump (1.2->1.3)     |
| `fix`      | A bug fix                                                              | patch bump (1.2.3->1.2.4) |
| `perf`     | A performance improvement with no API change                           | patch bump                |
| `refactor` | A code change that neither fixes a bug nor adds a feature              | none                      |
| `docs`     | Documentation only changes                                             | none                      |
| `style`    | Whitespace, formatting, missing semicolons, no logic change            | none                      |
| `test`     | Adding or correcting tests, no production code change                  | none                      |
| `build`    | Build system, dependency, Gradle/Kotlin/Compose toolchain changes      | none                      |
| `ci`       | CI configuration files and scripts (e.g. `.github/workflows/`)         | none                      |
| `chore`    | Other routine tasks: deps bump, cleanup, tooling, no production change | none                      |
| `revert`   | Reverts a previous commit (must include footer `Reverts: <hash>`)      | depends on reverted       |

### Breaking change marker

Append `!` after the type/scope to flag a breaking change:

```
feat(api)!: drop legacy login endpoint
```

A commit is **also** breaking if its footer contains `BREAKING CHANGE: <description>` — both forms are recognized by tooling.

---

## 3. Scope (optional but recommended)

The area of the codebase affected. Lower-case, hyphen-separated, no spaces.

Common scopes in this project (extend as the project grows):

```
auth, asset, library, http, launch, ui, backend, build, ci, deps, tests
```

Rules:
- Omit the scope if the change spans more than two areas or no specific module.
- Do NOT use PascalCase, spaces, or non-ASCII characters in the scope.

---

## 4. Subject (the first line)

Mandatory rules:

1. Imperative mood, present tense: "add", not "added" or "adds".
2. Lower-case first letter.
3. No trailing period (`.`).
4. Maximum 50 characters; aim for 72.
5. Be specific. Describe the **what**, not the **how**.

| Good                                           | Bad                         |
|------------------------------------------------|-----------------------------|
| `fix(http): resume corrupts on ignored Range`  | `Fixed http bug`            |
| `feat(auth): add Microsoft OAuth login`        | `Login stuff`               |
| `perf(library): download 20 jars concurrently` | `Optimize library download` |

---

## 5. Body (optional)

Use the body to explain **why** the change is necessary, not what the code does — the diff already shows that.

- Wrap lines at 72 characters.
- Separate paragraphs with blank lines.
- Bullet lists start with `-`.

The body explains motivation and trade-offs. It can mention the symptom the user saw, the root cause, and the chosen fix, in that order.

---

## 6. Footer (optional)

Used for two purposes, in any combination:

1. Reference issues / PRs:
   ```
   Closes: #123
   Refs: #456
   ```
2. Breaking-change description (only when not using the `!` marker):
   ```
   BREAKING CHANGE: removed `LauncherBackend.startMicrosoftLoginUrl`
   ```
3. Revert references:
   ```
   Reverts: 14ef777
   ```

---

## 7. Recipes by Scenario

Below are the exact commit-message templates this project uses. Use them verbatim.

### 7.1 Routine dependency upgrade (no behaviour change)

```
chore: bump <lib> <old> -> <new>
```

Example:
```
chore: bump commons-compress 1.26.2 -> 1.27.1
```

### 7.2 Build-toolchain upgrade

Use `build` for things that affect what can be built: Gradle wrapper, Kotlin
compiler, Compose plugin, JDK target.

```
build: bump <tool> <old> -> <new>
```

Example:
```
build: bump Compose Multiplatform 1.10.x -> 1.11.1
```

### 7.3 Edit the version catalog / build script without changing logic

```
chore: remove unused <thing>
```

Example:
```
chore: remove unused navigationRuntimeDesktop version ref
```

### 7.4 Pure code refactor (zero behaviour change)

Use `<scope>` of the module most touched; omit it if the refactor spans modules.

```
refactor(<scope>): <verb> <object>
```

Example:
```
refactor(asset): extract mirror-fallback into a helper
```

### 7.5 Bug fix

```
fix(<scope>): <symptom or root cause>
```

Body should briefly state the symptom users saw, the root cause, and the fix.

Example:
```
fix(http): resume corrupts on server-ignored Range

When the server returns 200 instead of 206 the partial file was
opened with APPEND, producing a duplicated, corrupt file that
could never pass SHA-1 check. Now TRUNCATE_EXISTING is used for
fresh downloads; APPEND only on real 206 resume.
```

### 7.6 New feature

```
feat(<scope>): <user-visible behaviour>
```

Example:
```
feat(auth): use approved Material Launcher Azure app id
```

### 7.7 Performance

```
perf(<scope>): <what became faster>
```

Example:
```
perf(library): download 20 libraries concurrently
```

---

## 8. Anti-patterns

Do **NOT** do any of the following:

1. **Do not mix a bug fix into a refactor commit.** They are tracked separately.
   Split before committing.
2. **Do not write "WIP", "tmp", "x", "asdf", "fix bug", "update"** as the
   subject. Rebase and rewrite before sharing the branch.
3. **Do not use past tense or third-person.** "Added", "adds" -> "add".
4. **Do not exceed 50 characters on the subject line.** CI / tooling
   truncates longer subjects.
5. **Do not write the body in non-English.** Consistency over time zone.
6. **Do not sign commits with `Signed-off-by` trailers unless the project
   requires DCO.** Add the trailer only when asked.
7. **Do not leave stray `Co-Authored-By: Claude` or `Co-Authored-By: ChatGPT`
   trailers** unless explicitly requested. AI assistance is welcome but
   does not need a co-author credit.

---

## 9. Validation

CI may enforce this convention via `commitlint` and a project-side
`commitlint.config.js`. Locally, before pushing, run:

```bash
npx commitlint --from=HEAD~5 --to=HEAD --verbose
```

Any commit that fails will be listed with the violated rule.
