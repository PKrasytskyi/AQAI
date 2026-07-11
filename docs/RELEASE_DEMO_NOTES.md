# Release / Demo Notes

## Snapshot

This snapshot is prepared as a public preview of the AgentLab automation-generation platform.

Highlights:

- typed/DAG workflow orchestration;
- deterministic UI Page Object prompt generation;
- `pom-contract-v1` Page Object contract planning;
- deterministic Page Object Java writer from validated contracts;
- generated Page Object source persistence followed by compile, review, smoke, and runtime-feedback stages;
- mapper knowledge split into raw, curated, and prompt-ready evidence;
- environment-based secret configuration;
- AI/RAG/knowledge-store integrations are demo-enabled in `framework.properties`, with secrets supplied through environment/JVM configuration;
- unit tests under standard Maven test source root: `src/test/java/unit/tests`;
- API MVP with endpoint evidence, client/DTO/test specs, quality gates, and RestAssured/TestNG writer;
- API CRUD demo mode.

## UI / POM Contract Demo Run

Use this run when you want to inspect LoginPage golden-slice discovery, deterministic POM prompts, `pom-contract-v1`, AI enrichment artifacts, and deterministic Page Object Java output.

```powershell
$env:OPENAI_API_KEY="..."
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="local-neo4j-password"

mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md"
```

For a clean no-DB/no-RAG comparison run, disable RAG and knowledge-store switches explicitly:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md" "-Drag.enabled=false" "-Dknowledge.graph.enabled=false" "-Dknowledge.vector.enabled=false"
```

Main artifacts:

```text
target/ai-run/run-summary.md
target/ai-run/page-objects/
target/ai-run/debug/          # only when ai.debug.artifacts=true
target/ai-run/enrichment/
target/ai-run/quality/
target/ai-run/need-review/
```

Expected current behavior:

- `LoginPage` should use confirmed username/password/login-button locators.
- `LoginPage` should be eligible for Neo4j page knowledge cache reuse after a stable run.
- `DashboardPage` should be discovered after authentication.
- `DashboardPage` should include confirmed route evidence plus user-menu trigger and logout-link evidence when the authenticated page is reachable.
- `DashboardPage` should expose `openUserMenu()` and `logout()` from the POM contract when those locators are confirmed.
- Dashboard heading assertions should remain coverage gaps if no confirmed heading locator exists.

Known demo boundary:

- `target/ai-run/validation/generated-ui-smoke-result.json` represents generated-source smoke validation over persisted POM files and compile/review readiness.
- `target/ai-run/validation/live-ui-smoke-result.json` represents the profile/capability-driven live browser smoke path for the authentication/logout vertical slice when `ui.live-smoke.enabled=true`.
- Broader live smoke scenarios beyond authentication/logout remain future hardening work.

## API CRUD Demo Run

Use this run when you want to generate API clients, DTOs, and RestAssured/TestNG tests from endpoint evidence without running UI discovery.

```powershell
$env:API_AUTH_TOKEN="..."

mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--api requirements/valid-author.md"
```

The current `framework.properties` GoRest seed includes:

```text
GET    /public/v2/users
GET    /public/v2/users/{id}
POST   /public/v2/users
PUT    /public/v2/users/{id}
PATCH  /public/v2/users/{id}
DELETE /public/v2/users/{id}
```

When the full CRUD endpoint set is present, the API layer generates a controlled CRUD flow:

```text
create -> read created id -> update -> patch -> delete
```

Generated API sources are written through the quality gate and ignored by Git:

```text
src/main/java/ua/demo/agentlab/api/generated/
src/test/java/ua/demo/agentlab/api/generated/
```

## Verification

Before pushing or demoing:

```powershell
rg -n 'BEGIN .*PRIVATE KEY' src docker-compose*.yml
rg -n 'sk-' src/main/resources src/test/resources docker-compose*.yml
rg -n 'password\s*=\s*[^${].+|token\s*=\s*[^${].+' src/main/resources src/test/resources docker-compose*.yml
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test
```
