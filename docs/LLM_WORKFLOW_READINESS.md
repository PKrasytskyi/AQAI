# LLM Workflow Readiness

> **Historical research note.** It predates typed/DAG runtime orchestration,
> Neo4j artifact reuse, and contract-first deterministic POM writing. Do not use
> it as a statement of current runtime readiness. Keep it only as design history
> until its still-relevant future-work items are merged into [Roadmap](../ROADMAP.md).

## Target Workflow

```text
User Story / Bug / Failure
        ->
Orchestrator
        ->
Task Classification
        ->
RAG Query Builder
        ->
Vector DB: semantic search
        ->
Graph DB: dependency expansion
        ->
Metadata Filter
        ->
Re-ranker
        ->
Prompt Builder
        ->
LLM
        ->
MCP Tool Execution
```

## Current Readiness Assessment

### 1. User Story / Bug / Failure intake

Status: `partial`

What exists:

- requirement file and URL intake through the requirement layer
- workflow state for normalized requirements, plans, UI discovery data, generated files
- MCP tools for reading logs, page source, screenshots, and Allure results

What is missing:

- unified intake contract for `story`, `bug`, and `runtime failure`
- dedicated failure-analysis model that turns artifacts into a repair task

### 2. Orchestrator

Status: `implemented for deterministic generation flow`

What exists:

- `AgentOrchestrator`
- `WorkflowState`
- ordered workflow agents in `DemoRunner`

What is missing:

- orchestrated LLM-first pipeline
- planner that can branch into generation, repair, or analysis mode

### 3. Task Classification

Status: `partial`

What exists:

- `QueryIntentResolver` for natural-language retrieval intent
- page classification in the Selenium discovery layer

What is missing:

- top-level task classifier for:
  - new test generation
  - bug reproduction
  - failure analysis
  - selector repair
  - test update

### 4. RAG Query Builder

Status: `partial to strong`

What exists:

- request enrichment in `ProjectContextRetriever`
- artifact-type aware retrieval query construction
- domain-term and qualifier extraction

What is missing:

- dedicated query builder contract separated from retriever
- multiple query strategies per task type

### 5. Vector DB: semantic search

Status: `implemented`

What exists:

- `CodeChunk`
- `CodebaseIndexer`
- `chunks.jsonl`
- Qdrant integration
- embeddings via OpenAI
- semantic retrieval via vector similarity

### 6. Graph DB: dependency expansion

Status: `partially implemented`

What exists:

- local in-memory project code graph
- `EXTENDS`, `IMPLEMENTS`, `USES`, `DECLARES`, `CALLS` heuristics
- graph-based context expansion

What is missing:

- persistent external graph database
- richer relationship extraction with AST-level precision
- runtime dependency evidence from executed tests

### 7. Metadata Filter

Status: `implemented in heuristic form`

What exists:

- `ArtifactType`
- `ChunkMetadata`
- `IndexedArtifact`
- artifact-aware context selection

What is missing:

- explicit standalone metadata filter stage
- configurable policy-based include/exclude rules

### 8. Re-ranker

Status: `implemented in heuristic form`

What exists:

- `ContextAssembler` scores by:
  - semantic score
  - artifact type
  - domain-term overlap
  - qualifier overlap

What is missing:

- standalone reranker contract
- cross-encoder or LLM-based reranking
- policy-aware reranking

### 9. Prompt Builder

Status: `implemented`

What exists:

- `PromptBuilder`
- `ProjectStylePromptBuilder`
- prompt includes:
  - intent
  - artifact metadata
  - project support types
  - retrieved code/style context

### 10. LLM

Status: `partial`

What exists:

- OpenAI embedding client
- OpenAI responses generation client
- generation through `RagConsoleRunner`

What is missing:

- production LLM integration inside the main orchestrated pipeline
- structured generation outputs such as file plans or patch plans
- retry and fallback policies

### 11. MCP Tool Execution

Status: `implemented as execution layer, not yet fully wired after LLM`

What exists:

- MCP tool contracts
- file tools
- Maven test execution tool
- artifact-reading tools
- local MCP execution agent

What is missing:

- planner-to-MCP action plan
- automatic post-LLM write and execution loop
- human approval gate before file mutations

## Practical Closeness To Target Workflow

### Architecture closeness

Estimated closeness: `70%`

Reason:

- all major layers now exist in some form
- semantic retrieval exists
- graph expansion exists
- prompt building exists
- LLM generation exists
- MCP execution exists

### Production workflow closeness

Estimated closeness: `40%`

Reason:

- the layers are present, but not yet fully connected into one end-to-end controlled loop
- task classification is still thin
- graph expansion is heuristic, not a real Graph DB
- LLM is not yet the default orchestrated generation engine
- MCP is not yet driven by a reviewed action plan from planner output

## Current Project State Summary

### Strong areas

- Selenium/TestNG core and template layer
- UI discovery with Selenium crawling and evidence capture
- requirement normalization
- rule-based planning
- RAG indexing and hybrid retrieval
- MCP execution abstraction

### Medium areas

- universalization beyond one project
- artifact classification quality
- graph relationship extraction precision
- prompt quality under different task types

### Weak or not-yet-finished areas

- production-ready LLM orchestration
- automatic mutation flow from LLM to MCP
- bug/failure repair loop
- API generation branch
- explicit planner layer

## Recommended Expansion Options

### Option A: Safe LLM assistant mode

Use LLM only for generation output review, without automatic writes.

Flow:

```text
requirements -> hybrid retrieval -> prompt -> LLM -> human review
```

Best for:

- first real experiments
- prompt tuning
- checking retrieval quality

### Option B: Controlled write mode

Use LLM to generate candidate code, then convert it into reviewed MCP actions.

Flow:

```text
requirements -> hybrid retrieval -> prompt -> LLM
-> planner/file plan
-> human approval
-> MCP write tools
```

Best for:

- early internal automation
- low-risk generation of new files

### Option C: Repair mode

Use runtime evidence and failure artifacts to drive updates.

Flow:

```text
failed test -> logs/screenshots/page source
-> task classifier = repair
-> hybrid retrieval
-> prompt
-> LLM
-> MCP update test/page object
```

Best for:

- self-healing experiments
- selector repair research

### Option D: Full planner-driven orchestration

Add a dedicated planner that outputs structured action plans.

Flow:

```text
task -> planner -> retrieval plan -> hybrid retrieval
-> prompt builder -> LLM
-> action plan -> MCP execution -> validation -> artifact readback
```

Best for:

- long-term target architecture
- production-ready agent orchestration

## Preparation For First LLM Test Run

### Goal of the first run

Use LLM in a controlled and observable mode.

The first run should not immediately write files automatically.
It should prove:

- indexing works
- retrieval returns useful context
- prompt is coherent
- LLM output matches project style

### Recommended first-run mode

Use `RagConsoleRunner` in generate-only mode first.

This is safer than immediately wiring generation into file mutation.

### Required preconditions

#### 1. OpenAI credentials

Set:

- `OPENAI_API_KEY`

Optional:

- `RAG_OPENAI_API_KEY`

#### 2. Configure RAG mode

The checked-in demo profile enables RAG, but Qdrant/embedding retrieval still requires reachable services and credentials. Disable it explicitly for no-RAG comparison runs or keep it enabled when Qdrant and credentials are configured:

- set `rag.enabled` to `false` for no-RAG runs
- keep `rag.enabled=true` for DB/RAG demo runs

#### 3. Start Qdrant

Use:

```powershell
docker compose -f docker-compose.qdrant.yml up -d
```

#### 4. Pick a clean input task

Best first prompt:

```text
Generate a TestNG UI test for opening the collections page and verifying visible item cards
```

Avoid the first run with:

- large bug narratives
- multi-feature flows
- repair requests with missing artifacts

#### 5. Ensure project indexing scope is useful

Before the first run confirm that the project contains:

- core UI support classes
- templates
- policies
- generated or hand-written examples

Without style examples, output quality drops.

### Recommended first-run steps

#### Step 1. Compile the project

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test-compile
```

#### Step 2. Run indexing

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="index ."
```

#### Step 3. Run hybrid search

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="search . \"Generate negative login tests\""
```

Verify that results include:

- page object
- base class
- test class
- policy or test data support

#### Step 4. Run generation

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="generate . \"Generate negative login tests\""
```

#### Step 5. Review output manually

Check:

- style alignment
- package usage
- correct base classes
- correct assertions
- no invented framework abstractions

## What Must Be Done Before True LLM-Driven Test Run

To move from generation demo to first real LLM-driven test run, the minimum next work is:

1. Add `TaskClassifier`
2. Add `RagQueryBuilder` as a dedicated component
3. Add `Planner` that emits a structured file/action plan
4. Add human approval boundary before MCP writes
5. Connect `GenerateTestAgent` output to `CreatePageObjectTool` and `UpdateTestTool`
6. Add post-write compile/test execution through MCP
7. Add artifact readback loop after execution

## Recommended Immediate Next Step

The best immediate next step is:

`Planner layer with structured action plan`

That is the missing bridge between:

- LLM output
- MCP execution
- controlled end-to-end automation
