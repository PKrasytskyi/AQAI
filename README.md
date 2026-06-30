# AI QA Automation Platform

AI-assisted Java platform for requirement-driven Selenium + TestNG automation.

The project reads requirements, discovers UI pages, maps page capabilities, enriches page knowledge, builds deterministic prompts, and validates generated Page Object / test artifacts through quality gates.

## What This Platform Does

- Loads requirements from files or URLs.
- Normalizes requirements into canonical test cases.
- Discovers UI pages with Selenium and maps page capabilities.
- Scores locator quality and rejects weak or unsafe locator evidence.
- Persists page knowledge into Neo4j and Qdrant when enabled.
- Uses AI as an enrichment and prompt-assist layer, not as the source of truth.
- Produces deterministic Page Object prompts for review.
- Separates unresolved expected results into `target/ai-run/need-review`.
- Generates run quality summaries and artifact diffs between runs.

## Current Architecture

The active workflow is:

```text
RequirementDocument
  -> NormalizedRequirementBundle
  -> PageModelBundle
  -> MappedUiKnowledge
  -> Expected Result Enrichment
  -> PageModel Enrichment
  -> AiContextPackage
  -> Deterministic Page Object Prompts
  -> Quality Summary / Artifact Diff
```

The runtime now uses dependency-based orchestration: agents declare typed input/output artifacts, `AgentOrchestrator` builds a DAG, and `PipelineArtifactStore` is the primary in-run artifact registry. `WorkflowState` remains as the run envelope/read model for audit, failures, and artifact references while the remaining writer/reporting code is being simplified.

## Main Modules

- `ua.demo.agentlab.app` - runner entry points and workflow wiring.
- `ua.demo.agentlab.requirements` - requirement loading and normalization.
- `ua.demo.agentlab.testcase` - canonical test case generation.
- `ua.demo.agentlab.ui.discovery` - UI discovery, PageModel building, locator scoring, and mapping.
- `ua.demo.agentlab.ui.catalog` - confirmed page candidates, capabilities, and page registry.
- `ua.demo.agentlab.ai.context` - target-aware prompt context assembly and retrieval filtering.
- `ua.demo.agentlab.ai.pageenrichment` - page-scoped enrichment over mapper output.
- `ua.demo.agentlab.ai.expectationenrichment` - deterministic/AI expected-result resolution.
- `ua.demo.agentlab.ai.ui` - Page Object and UI test prompt/spec generation.
- `ua.demo.agentlab.ai.quality` - run quality summary and artifact diff.
- `ua.demo.agentlab.core.ui` - Selenium base page/test helpers used by generated code.
- `ua.demo.agentlab.core.api` - RestAssured API runtime helper used by generated API clients.
- `ua.demo.agentlab.core.config` - repository-safe runtime configuration reader with environment/JVM placeholder resolution.

### Compatibility Facades

The preferred package namespace for platform-owned code is `ua.demo.agentlab...`.
Two root-package facades remain only for backward compatibility with older generated examples:

- `core.ApiManager` delegates to `ua.demo.agentlab.core.api.ApiManager`.
- `config.ConfigReader` delegates to `ua.demo.agentlab.core.config.ConfigReader`.

New generated API code imports the `ua.demo.agentlab.core.*` classes directly.

## Requirements

- Java 17
- Maven 3.9+
- Chrome/ChromeDriver compatible with Selenium when browser discovery is enabled
- Optional: Docker for Neo4j and Qdrant
- Optional: `OPENAI_API_KEY` for AI enrichment

## Configuration

Default project configuration is in:

```text
src/main/resources/framework.properties
src/main/resources/test-data.properties
src/main/resources/framework.example.properties
```

`framework.properties` is intentionally safe to keep in the repository: secret values are expressed as `${ENV_VAR}` placeholders and resolved at runtime from JVM properties, environment variables, or the loaded properties file. Do not commit local secrets. Use environment variables or ignored local override files for private values:

```powershell
$env:OPENAI_API_KEY="..."
$env:RAG_OPENAI_API_KEY="..."
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="..."
$env:API_AUTH_TOKEN="..."
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
```

Ignored local override examples:

```text
framework-local.properties
test-data-local.properties
application-local.properties
.env
```

## Knowledge Stores

The platform can use:

- Neo4j for graph page knowledge.
- Qdrant for vector retrieval.

Start both with:

```powershell
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="local-neo4j-password"
docker compose -f docker-compose.knowledge.yml up -d
```

Neo4j schema:

```powershell
docker exec -it agentlab-neo4j cypher-shell -u neo4j -p $env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD -f /var/lib/neo4j/import/page-knowledge-schema.cypher
```

## Build And Test

Run tests:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test
```

Unit tests for the platform live in:

```text
src/test/ua.demo.agentlab/unity
```

`src/test/java` is reserved for future integration tests and generated-test compile fixtures.

Compile only:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" compile
```

## Run The Workflow

Deterministic run:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java
```

Run with a requirement file:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=requirements/medium-50-requirements.md"
```

AI enrichment / prompt-review run:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/medium-50-requirements.md"
```

Current AI mode records deterministic POM prompts and enrichment artifacts. Direct LLM test generation is intentionally disabled while enrichment, contracts, schemas, and quality gates are being stabilized.

API generation is currently demo-safe by default: endpoint discovery may produce client and DTO specs for the discovered surface, but runnable RestAssured/TestNG tests are generated only for confirmed GET collection endpoints until explicit scenario data and auth/data-factory policy are available for path-parameter and mutation flows.

API CRUD demo run:

```powershell
$env:API_AUTH_TOKEN="..."
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--api requirements/valid-author.md"
```

More details:

- [API Layer Documentation](docs/API_LAYER.md)
- [Release / Demo Notes](docs/RELEASE_DEMO_NOTES.md)

## Important Runtime Artifacts

Runtime artifacts are generated under `target/` and are ignored by Git:

```text
target/ai-run/context/
target/ai-run/enrichment/
target/ai-run/expectations/
target/ai-run/need-review/
target/ai-run/page-object-spec/
target/ai-run/quality/
target/ai-run-history/
target/discovery/
```

Key files:

- `target/ai-run/page-object-spec/*-prompt.txt` - deterministic POM prompts.
- `target/ai-run/page-object-spec/*-scope-trace.json` - page scope evidence.
- `target/ai-run/expectations/test-case-expected-results.json` - resolved expected results.
- `target/ai-run/need-review/expected-results-needs-review.json` - unresolved expected results for review.
- `target/ai-run/quality/run-quality-summary.json` - run-level quality score.
- `target/ai-run/quality/artifact-diff.json` - comparison against previous run artifacts.

## Quality Gates

The platform has executable checks for:

- stale locator candidates;
- external-origin locator evidence;
- cross-route evidence;
- weak URL nonblank assertions;
- missing expected values;
- prompt blocking issues;
- unresolved expected results;
- route collisions;
- low-confidence locators.

## Documentation

- [API Layer Documentation](docs/API_LAYER.md)
- [Current AI UI Architecture](docs/CURRENT_AI_UI_ARCHITECTURE.md)
- [Project Detailed Guide](docs/PROJECT_DETAILED_GUIDE.md)
- [AI POM Fix Plan](docs/AI_POM_FIX_PLAN.md)
- [Graph and Vector DB Setup](docs/GRAPH_AND_VECTOR_DB_SETUP.md)
- [RAG Layer Guide](docs/RAG_LAYER_GUIDE.md)
- [Target Architecture](docs/TARGET_ARCHITECTURE.md)
- [Roadmap](ROADMAP.md)

## Repository Hygiene

The repository intentionally excludes:

- Maven dependency caches;
- IDE metadata;
- local secrets;
- generated runtime artifacts;
- generated UI source produced by local workflow runs;
- local DB volumes.
- generated API/UI source from local demo runs.

This keeps GitHub focused on source code, requirements, docs, infrastructure definitions, and reproducible tests.

## Public Repository Boundary

Before publishing a new snapshot, the expected commit boundary is:

- source code under `src/main/java` and unit tests under `src/test/ua.demo.agentlab/unity`;
- curated requirements/docs/config examples;
- infrastructure definitions such as `docker-compose.knowledge.yml`;
- no `target/`, local DB volumes, IDE metadata, API keys, real credentials, or generated runtime artifacts.

Recommended pre-push checks:

```powershell
git status --short
rg -n 'BEGIN .*PRIVATE KEY' src docker-compose*.yml
rg -n 'sk-' src/main/resources src/test/resources docker-compose*.yml
rg -n 'password\s*=\s*[^${].+|token\s*=\s*[^${].+' src/main/resources src/test/resources docker-compose*.yml
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test
```
