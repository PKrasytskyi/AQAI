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

## Future CI/CD Expansion

Recommended next steps:

1. Add a dedicated PR quality summary artifact for API/UI generation reports.
2. Add `mvn test-compile` to the default CI once generated compile fixtures are fully stable.
3. Add a scheduled dependency/security scan.
4. Add an optional UI prompt generation smoke job that runs only with safe test credentials.
5. Add release packaging once the API/UI generation contracts stabilize.
