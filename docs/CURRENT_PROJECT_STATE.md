# Current Project State

Date: 2026-06-30

This document summarizes the current state of `agent_base_testing_framework` for engineers reviewing the repository before a public snapshot.

## 1. What The Project Is

AgentLab is a requirement-driven automation generation platform. It is not only a Selenium test framework and not a free-form LLM code writer. The platform builds typed evidence from requirements, UI discovery, API endpoint discovery, policy, and knowledge stores, then produces deterministic artifacts guarded by quality gates.

## 2. Current Architecture State

The workflow core is dependency-based:

- agents declare `requires()` and `produces()` artifacts;
- `AgentOrchestrator` builds a DAG;
- `PipelineAgent<I, O>` executes with typed input and a `WorkflowRunEnvelope`;
- `PipelineArtifactStore` is the in-run source of typed business artifacts;
- `WorkflowState` remains as an envelope/read model for audit, failures, generated artifact references, and reporting.

The old numeric `order()` execution model and `WorkflowStatePipelineAdapter` have been removed from runtime orchestration.

## 3. Implemented Functional Areas

### Requirements

- file/URL requirement loading;
- rule-based normalization;
- expected-result extraction from assertion-oriented sections;
- canonical test case generation.

### UI Mapper And Prompt Workflow

- Selenium discovery;
- PageModel building;
- locator origin/stability scoring;
- confirmed page registry;
- capability-based page naming;
- raw/curated/prompt-ready knowledge separation;
- Neo4j/Qdrant persistence and retrieval with namespace metadata;
- PageModel enrichment through AI when enabled;
- deterministic POM prompt generation;
- prompt quality linter;
- run quality summary and artifact diff.

### API MVP

- endpoint evidence from OpenAPI, network scan, and `project.api.endpoint-seed`;
- canonical API test cases;
- typed API assertion contracts;
- `ApiClientSpec`, `ApiDtoSpec`, and `ApiTestSpec`;
- RestAssured/TestNG writer;
- API quality gate;
- controlled source persistence after quality gate.

Default API test generation is conservative: confirmed GET collection endpoints only. Path-parameter and mutation tests require explicit scenario data and auth/data-factory policy.

### AI Boundary

AI currently participates in enrichment and prompt assistance. Direct LLM-backed Java writing is intentionally disabled in the active AI prompt workflow.

## 4. Important Runtime Artifacts

Generated runtime artifacts are ignored by Git and written under `target/`, especially:

- `target/ai-run/`
- `target/ai-run-history/`
- `target/discovery/`
- `target/rag/`

Need-review expected-result cases are written separately under `target/ai-run/need-review`.

## 5. Test Layout

Unit tests live in:

```text
src/test/ua.demo.agentlab/unity
```

`src/test/java` is intentionally left available for future integration tests and generated-test compile fixtures.

## 6. Configuration And Secrets

`framework.properties` is repository-safe and uses environment/JVM placeholders for secrets:

- `OPENAI_API_KEY`
- `RAG_OPENAI_API_KEY`
- `KNOWLEDGE_GRAPH_NEO4J_PASSWORD`
- `API_AUTH_TOKEN`
- `TEST_VALID_USERNAME`
- `TEST_VALID_PASSWORD`

Local secret override files are ignored by `.gitignore`.

## 7. Current Public-Repo Readiness

Strengths:

- architecture has moved to typed/DAG orchestration;
- tests are green;
- secret-bearing config has been replaced by placeholders;
- generated/runtime artifacts are ignored;
- root package API/config classes are now deprecated compatibility facades.

Remaining public-readiness work:

- keep the current large change set as one controlled commit boundary;
- verify no accidental runtime artifacts are staged;
- continue shrinking `WorkflowState` and `StageOutputPublisher` over time;
- add scenario data/data-factory support before enabling path-param or mutation API tests by default.
