# Release / Demo Notes

## Snapshot

This snapshot is prepared as a public preview of the AgentLab automation-generation platform.

Highlights:

- typed/DAG workflow orchestration;
- deterministic UI Page Object prompt generation;
- mapper knowledge split into raw, curated, and prompt-ready evidence;
- environment-based secret configuration;
- dedicated unit-test source root: `src/test/ua.demo.agentlab/unity`;
- API MVP with endpoint evidence, client/DTO/test specs, quality gates, and RestAssured/TestNG writer;
- API CRUD demo mode.

## UI Prompt Run

Use this run when you want to inspect deterministic POM prompts and AI enrichment artifacts.

```powershell
$env:OPENAI_API_KEY="..."
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="local-neo4j-password"

mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-author.md"
```

Main artifacts:

```text
target/ai-run/page-object-spec/
target/ai-run/context/
target/ai-run/enrichment/
target/ai-run/quality/
target/ai-run/need-review/
```

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
