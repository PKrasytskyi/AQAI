# GitHub Actions CI/CD Preparation

This repository is prepared for GitHub Actions through `.github/workflows/ci.yml`.

The current workflow is intentionally CI-safe:

- it does not run live UI browser discovery;
- it does not execute generated API tests against a live service by default;
- it validates Java compilation and the unit test suite;
- it can run the API demo generation path manually through `workflow_dispatch`.

## Default CI

Default triggers:

- push to `main`;
- push to `dev`;
- pull request to `dev` or `main`.

`dev` is the integration branch and `main` is the public/release branch. New
work should reach either branch only through a short-lived feature/fix/refactor
branch and pull request. See [Developer Onboarding and Delivery Guide](DEVELOPER_ONBOARDING.md).

Default job:

```text
build-test
  -> checkout
  -> setup Java 17
  -> Maven compile
  -> Maven test
  -> upload Surefire reports on failure
```

Commands:

```bash
mvn --batch-mode --no-transfer-progress compile
mvn --batch-mode --no-transfer-progress test
```

The Maven Surefire configuration excludes generated API/UI tests from normal unit test execution:

```text
**/api/generated/**
**/ui/generated/**
```

This keeps CI deterministic and prevents accidental live API/UI calls.

## Manual API Demo Compile Gate

The workflow includes an optional manual job:

```text
api-demo-compile
  -> generate API demo sources
  -> compile generated API sources and generated tests
  -> upload demo artifacts
```

Run it from GitHub Actions with:

```text
Run workflow -> run_api_demo = true
```

Command executed by the job:

```bash
mvn --batch-mode --no-transfer-progress exec:java "-Dexec.args=--api requirements/valid-author.md"
mvn --batch-mode --no-transfer-progress test-compile
```

This validates that the API generation layer can produce compile-checkable RestAssured/TestNG clients, DTOs, and tests.

## Required Secrets

Default CI does not require secrets.

Optional API demo generation may use:

```text
API_AUTH_TOKEN
```

Add it under:

```text
GitHub repository -> Settings -> Secrets and variables -> Actions -> New repository secret
```

Do not commit real API tokens into `framework.properties`.

## Build Week Container Delivery

`.github/workflows/publish-build-week-image.yml` publishes the immutable Build Week runtime image to GitHub Container Registry on `main`, version tags, or manual dispatch.

The image is tagged with:

```text
ghcr.io/<repository-owner>/aqai-build-week:2026
ghcr.io/<repository-owner>/aqai-build-week:sha-<commit>
```

Set the published package visibility to **Public** in GitHub Packages so reviewers can pull it anonymously. The image contains Java 17, Chromium, ChromeDriver, Maven dependencies, and compiled AQAI classes. It does not contain API keys, credentials, or populated knowledge stores.

The image targets `linux/amd64` and is intended for Docker Engine or Docker Desktop running Linux containers on Windows, macOS, and Linux.

For the reproducible cold/warm knowledge demo, use:

```bash
docker compose -f docker-compose.build-week.yml down -v
docker compose -f docker-compose.build-week.yml up --pull always
```

The Compose workflow starts fresh Neo4j/Qdrant volumes, performs a seed run, then performs a measured reuse run. It writes both summaries into the ignored local `build-week-artifacts/` directory.

## Future CI/CD Expansion

Recommended next steps:

1. Add a dedicated PR quality summary artifact for API/UI generation reports.
2. Add `mvn test-compile` to the default CI once generated compile fixtures are fully stable.
3. Add a scheduled dependency/security scan.
4. Add an optional UI prompt generation smoke job that runs only with safe test credentials.
5. Add release packaging once the API/UI generation contracts stabilize.
