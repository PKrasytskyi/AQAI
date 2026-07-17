# Contributing

Thank you for your interest in improving AI QA Automation Platform.

This repository is an engineering demo and research-oriented automation platform. Contributions should keep the project reproducible, secret-safe, and easy to run locally.

## Ground Rules

- Do not commit API keys, passwords, tokens, browser profiles, local database volumes, or generated runtime artifacts.
- Keep generated output under `target/` or ignored generated source folders.
- Keep default configuration repository-safe. Optional integrations such as OpenAI, Qdrant, and Neo4j should be opt-in.
- Prefer deterministic Java validation and typed contracts over prompt-only rules.
- Add or update unit tests for mapper, prompt, quality gate, schema, or orchestration changes.
- Keep changes scoped. Avoid broad refactors mixed with feature work unless the refactor is required.

## Development Setup

Requirements:

- Java 17
- Maven 3.9+
- Optional Docker for Neo4j and Qdrant
- Optional environment variables for AI or authenticated demo runs

Run the unit test suite:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test
```

Unit tests live under:

```text
src/test/java/unit/tests
```

## Configuration

Use environment variables, JVM properties, or ignored local override files for secrets:

```text
OPENAI_API_KEY
RAG_OPENAI_API_KEY
KNOWLEDGE_GRAPH_NEO4J_PASSWORD
API_AUTH_TOKEN
TEST_VALID_USERNAME
TEST_VALID_PASSWORD
```

Do not replace placeholders in committed properties files with real credentials.

## Pull Request Checklist

Before opening a pull request:

- `mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test` passes.
- `git status --short --untracked-files=all` contains only intentional changes.
- No files under `target/`, local DB folders, Maven caches, or IDE metadata are included.
- Public configuration defaults remain safe for a first clone.
- README/docs are updated when behavior or commands change.

## Branching And Integration

`main` and `dev` are stable branches. `main` is the public/release baseline;
`dev` is the stable integration baseline. Do not commit new feature work directly
to either branch.

Create a short-lived branch from the current `dev` branch for every change:

```powershell
git switch dev
git pull --ff-only
git switch -c feature/<area>-<short-description>
```

Use `feature/`, `fix/`, `refactor/`, or `docs/` prefixes. Merge reviewed,
validated work into `dev`; promote `dev` to `main` only through a release pull
request. Create urgent `hotfix/` branches from `main`, then merge the same fix
back into `dev`.

The detailed operating procedure is in
[Developer Onboarding and Delivery Guide](docs/DEVELOPER_ONBOARDING.md).

## Commit Style

Use concise, descriptive commit messages:

```text
Add runtime evidence semantic graph
Fix prompt locator filtering
Document API demo flow
```

Prefer one coherent change per commit.
