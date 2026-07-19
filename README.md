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
- Produces deterministic Page Object contract prompts and validated POM contracts for review.
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
  -> POM Contract Prompts
  -> pom-contract-v1 Validation
  -> Deterministic Page Object Java Writer
  -> Generated Source Persistence
  -> Compile / Review / Smoke Validation
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
- `ua.demo.agentlab.ai.ui` - structured POM contract planning and deterministic Page Object generation.
- `ua.demo.agentlab.ui.testcontract` - typed atomic UI test contracts, schema/semantic validation, deterministic TestNG generation, and source maps.
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

## Runtime Skills

LLM-facing task contracts live in:

```text
runtime-skills/
```

This package separates prompt/schema rules from Java orchestration code. The current skill set is:

- `pom-json-generation` - structured `pom-contract-v1` output for deterministic POM Java writing.
- `page-enrichment` - semantic enrichment over mapper-approved page evidence.
- `test-json-generation` - structured UI test spec JSON, not Java code.
- `self-healing-locator` - replacement locator review after runtime failures.
- `error-analysis` - root-cause classification for failed runs/tests.
- `bug-report-generation` - bug report drafts from validated evidence.

Each skill owns `skill.yaml`, `prompt.md`, `input-schema.json`, `output-schema.json`, `rules.md`, and examples. `RuntimeSkillPromptLoader` loads these contracts for POM JSON generation and page enrichment prompt assembly, while page-specific evidence remains dynamically assembled by the Java pipeline.

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
src/main/resources/profiles/orangehrm.project-profile.yaml
```

`framework.properties` is intentionally safe to keep in the repository: secret values are expressed as `${ENV_VAR}` placeholders and resolved at runtime from JVM properties, environment variables, or the loaded properties file. Do not commit local secrets. Use environment variables or ignored local override files for private values:

```powershell
$env:OPENAI_API_KEY="..."
$env:OPENAI_MODEL="gpt-5.6-luna"
$env:RAG_OPENAI_API_KEY="..."
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="..."
$env:API_AUTH_TOKEN="..."
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
```

Project-specific runtime values should live in a project profile:

```properties
project.profile.file=profiles/orangehrm.project-profile.yaml
```

The active profile can define project id/name/base URL, routes, UI runtime defaults, auth selectors/env names, AI switches, knowledge-store switches, and the default requirements file. Resolution order is:

```text
JVM system property > environment variable > project profile YAML > framework.properties > code fallback
```

If the CLI does not pass a requirement file, the runner uses `requirements.file` from the active project profile.

Generated UI sources are isolated per project. Unless a profile explicitly declares output packages, the platform derives them from `profileId + baseUrlHash`:

```text
ua.demo.agentlab.ui.generated.<generationNamespace>.pages
ua.demo.agentlab.ui.generated.<generationNamespace>.tests
```

The checked-in OpenAI generation default is `gpt-5.6-luna`. `OPENAI_MODEL` remains the runtime override
for accounts or environments that expose a different model identifier; RAG generation inherits the same
value unless `RAG_OPENAI_GENERATION_MODEL` is set explicitly.

Persistence writes `target/ai-run/validation/generated-source-manifest.json`. Compile, review, and generated-source smoke accept only files whose package, path, class name, and content hash belong to that current-run manifest. Maven compile is scoped to the manifest namespace, so equal names such as `LoginPage` or `REQ001...Test` from another project cannot collide with the active run.

Important runtime switches:

```properties
openai.enabled=true
ai.page-enrichment.llm.enabled=true
ai.page-object.llm.enabled=true
ai.ui-test.llm.enabled=false
rag.enabled=true
knowledge.graph.enabled=true
knowledge.vector.enabled=true
artifact.reuse.enabled=true
artifact.reuse.force-refresh=false
```

- `KNOWLEDGE_DB_STATUS=false` disables RAG, Neo4j, Qdrant, and artifact reuse for a no-DB comparison run.
- `KNOWLEDGE_DB_STATUS=true` enables RAG, Neo4j, Qdrant, and artifact reuse for a DB-backed run.
- Demo manifests use `runtime.databaseMode: environment-controlled`, so the same immutable demo input supports both runs. Fixed `without-db-baseline` and `with-db-required` modes remain available for dedicated manifests.
- `ai.page-enrichment.llm.enabled` controls whether page enrichment can call OpenAI.
- `rag.enabled` controls retrieval/indexing behavior and must not be treated as the page-enrichment switch.
- `knowledge.graph.enabled` and `knowledge.vector.enabled` control Neo4j/Qdrant persistence and retrieval availability.
- `artifact.reuse.enabled` allows DB-backed POM contract reuse by fingerprint before the POM LLM call; only contracts promoted to `STABLE` after writer, compile, review, and smoke validation are reusable.
- `artifact.reuse.flow-contract.enabled` persists evidence-gated, capability-based Flow Contracts to Neo4j. `ReusePlanner` may reuse only a Qdrant-ranked and Neo4j-confirmed stable flow as a precondition decision; it never injects raw flow text into a POM prompt.
- `semantic.reuse.enabled` is opt-in. It indexes only `CONFIRMED` Flow Contract summaries in Qdrant; Neo4j exact lookup and namespace checks remain the decision boundary.
- `artifact.reuse.pom-contract.enabled`, `artifact.reuse.flow-contract.enabled`, and `artifact.reuse.test-data.enabled` independently control reuse domains. `artifact.reuse.policy=strict` and `artifact.reuse.force-refresh=true` prevent accidental rollout or stale reuse.
- `artifact.reuse.force-refresh=true` bypasses reusable artifacts and forces a new POM contract generation.
- Run summaries expose `dbUsageMode`, `neo4jHit`, `qdrantHit`, `stableCacheUsed`, and page-enrichment counters so DB/no-DB behavior is visible in artifacts.

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

DB-backed runs are reported as:

- `with-db` when Neo4j hit, Qdrant hit, and stable cache reuse are all true.
- `without-db` when all three are false.
- `partial-db` for mixed states, for example Neo4j available but Qdrant disabled or embedding key missing.

Page enrichment cache reuse is tracked separately through:

- `pageEnrichmentGenerated`
- `pageEnrichmentCacheHits`
- `pageEnrichmentOpenAiCalls`

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
src/test/java/unit/tests
```

`src/test/java` is the standard Maven test source root. Generated UI/API compile fixtures also live under this root so their package names match their file paths.

Compile only:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" compile
```

## Run The Workflow

Build Week OrangeHRM requirements-to-execution demo:

```powershell
$env:OPENAI_API_KEY="..."
$env:OPENAI_MODEL="gpt-5.6-luna"
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
$env:KNOWLEDGE_DB_STATUS="false"

mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--demo orangehrm"
```

The command resolves the versioned demo manifest, generates namespaced Page Objects and TestNG tests, compiles and reviews them, runs source/live smoke, executes only current-run manifest-owned generated tests, and writes `target/ai-run/quality/build-week-demo-summary.{json,md}`. See [Build Week Demo](docs/BUILD_WEEK_DEMO.md).

Deterministic run:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java
```

Run with a requirement file:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=requirements/valid-login-requirement.md"
```

AI enrichment / prompt-review run:

```powershell
$env:OPENAI_API_KEY="..."
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="local-neo4j-password"

mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md"
```

The checked-in `framework.properties` is demo-oriented and enables AI/RAG/knowledge-store switches, while secrets still come from environment variables. For a clean no-DB/no-RAG comparison run, disable them explicitly with JVM properties:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md" "-Drag.enabled=false" "-Dknowledge.graph.enabled=false" "-Dknowledge.vector.enabled=false"
```

Equivalent one-switch form:

```powershell
$env:KNOWLEDGE_DB_STATUS="false"
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai"
```

Current AI mode records deterministic POM contract prompts and enrichment artifacts. When `ai.page-object.llm.enabled=true`, the LLM returns only `pom-contract-v1` JSON. It does not write Java bodies. Java Page Objects are produced by `DeterministicPomJavaWriter` from the validated contract.

The current golden UI slice is `requirements/valid-login-requirement.md`: LoginPage discovery, confirmed username/password/login-button locators, DashboardPage discovery, confirmed dashboard route, user-menu trigger and logout link evidence, Neo4j/Qdrant knowledge use when enabled, `pom-contract-v1`, deterministic POM generation, source persistence, compile/review, and generated-source smoke validation.

Current golden-slice limitations:

- Dashboard heading evidence is not forced when no confirmed heading locator exists; it remains a coverage gap.
- The generated-source smoke gate validates generated POM files, compile/review readiness, and structural interaction contracts. The profile/capability-driven live browser smoke additionally proves `open login -> authenticate -> dashboard route -> open user menu -> logout visible -> login route` when the relevant evidence and credentials are available.
- Run quality score is intentionally conservative: it should exceed 90 only when confirmed locators, compile, review, and smoke evidence are all strong.

POM prompts are compact by default: they contain the page capability contract, page-owned required actions/assertions, allowed locators, baseline API signatures, and the `pom-contract-v1` output schema. Full diagnostic prompt evidence can be enabled with `-Dai.page-object.prompt.mode=debug` or `-Dai.prompt.debug=true`.

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
target/ai-run/debug/
target/ai-run/enrichment/
target/ai-run/expectations/
target/ai-run/need-review/
target/ai-run/page-objects/
target/ai-run/quality/
target/ai-run/run-summary.json
target/ai-run/run-summary.md
target/ai-run-history/
target/discovery/
```

Key files:

- `target/ai-run/run-summary.md` - compact run review summary.
- `target/ai-run-history/last-10-runs.md` - one final-state table for the latest ten completed runs: DB retrieval, POM/flow reuse, LLM calls, lifecycle, compile/review, smoke, and feedback status. It is updated after the validation and DB-feedback stages.
- `target/ai-run/page-objects/*-prompt.txt` - deterministic `pom-contract-v1` POM prompts.
- `target/ai-run/page-objects/*-pom-contract.json` - validated POM contracts returned by the LLM when POM LLM mode is enabled.
- `target/ai-run/expectations/test-case-expected-results.json` - resolved expected results.
- `target/ai-run/need-review/expected-results-needs-review.json` - unresolved expected results for review.
- `target/ai-run/quality/run-quality-summary.json` - run-level quality score.
- `target/ai-run/quality/artifact-diff.json` - comparison against previous run artifacts.
- `target/ai-run/debug/**` - scope traces, prompt traces, raw context packages, and pipeline snapshots when `ai.debug.artifacts=true`.

Rebuild the table without running discovery or the LLM:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.mainClass=ua.demo.agentlab.app.RunHistoryStatisticsRunner"
```

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

- [Developer Onboarding and Delivery Guide](docs/DEVELOPER_ONBOARDING.md)
- [License](LICENSE)
- [Contributing](CONTRIBUTING.md)
- [Security Policy](SECURITY.md)
- [API Layer Documentation](docs/API_LAYER.md)
- [GitHub Actions CI/CD](docs/GITHUB_ACTIONS_CI_CD.md)
- [Current AI UI Architecture](docs/CURRENT_AI_UI_ARCHITECTURE.md)
- [Project Detailed Guide](docs/PROJECT_DETAILED_GUIDE.md)
- [AI POM Fix Plan](docs/AI_POM_FIX_PLAN.md)
- [Graph and Vector DB Setup](docs/GRAPH_AND_VECTOR_DB_SETUP.md)
- [RAG Layer Guide](docs/RAG_LAYER_GUIDE.md)
- [Target Architecture](docs/TARGET_ARCHITECTURE.md)
- [Roadmap](ROADMAP.md)

Development happens on short-lived `feature/`, `fix/`, `refactor/`, or `docs/`
branches created from `dev`. `dev` is the stable integration baseline and
`main` is the public/release baseline; see the onboarding guide for the merge
and hotfix policy.

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

- source code under `src/main/java` and unit tests under `src/test/java/unit/tests`;
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
