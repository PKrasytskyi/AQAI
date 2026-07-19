# OpenAI Build Week Demo

This demo closes one immutable OrangeHRM requirement bundle through generated and executed TestNG tests:

```text
demo manifest
  -> project profile and requirements
  -> normalized requirements and canonical scenarios
  -> confirmed UI catalog
  -> PomContractSpec
  -> deterministic Page Object Java
  -> UiTestContractBundle
  -> deterministic TestNG Java
  -> current-run source manifest
  -> compile, review, source smoke, live smoke
  -> manifest-owned generated TestNG execution
  -> Build Week summary
```

## Required Environment

```powershell
$env:OPENAI_API_KEY="..."
$env:OPENAI_MODEL="gpt-5.6-luna"
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
$env:KNOWLEDGE_DB_STATUS="false"
```

For a DB-backed run, start Neo4j and Qdrant, provide `KNOWLEDGE_GRAPH_NEO4J_PASSWORD`, and set `KNOWLEDGE_DB_STATUS=true`.

## One Command

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--demo orangehrm"
```

`--demo orangehrm` resolves exactly one versioned manifest under `demo/`. The manifest selects the project profile, requirement fixture, expected scenario IDs, POM names, generated TestNG classes, schemas, DB mode, AI boundary, and final state. Secrets are names only in the manifest and values only in environment variables.

The process exits with a Maven failure when preflight, quality, contract, compile, review, smoke, generated-test execution, or terminal manifest acceptance fails.

## Primary Evidence

Review these files first:

```text
target/ai-run/quality/demo-input-readiness.json
target/ai-run/quality/confirmed-ui-catalog.json
target/ai-run/validation/generated-source-manifest.json
target/ai-run/validation/generated-code-compile-result.json
target/ai-run/validation/generated-code-review-result.json
target/ai-run/validation/generated-ui-smoke-result.json
target/ai-run/validation/live-ui-smoke-result.json
target/ai-run/validation/generated-tests-execution-result.json
target/ai-run/quality/build-week-demo-summary.json
target/ai-run/quality/build-week-demo-summary.md
```

The execution artifact contains one result per generated scenario with requirement IDs, generated source path, duration, failure summary, source-map action/assertion IDs, and Surefire evidence path. Only `UI_TEST` classes owned by the current-run `GeneratedSourceManifest` are selected.

## AI Boundary

OpenAI is used only for semantic page enrichment and `PomContractSpec` planning. The checked-in default model identifier is `gpt-5.6-luna`, with `OPENAI_MODEL` as the runtime override. Requirements normalization, evidence filtering and promotion, Page Object Java, test contract assembly, TestNG Java, compile, review, smoke, and execution are deterministic.

Codex is the repository-aware engineering layer. `AGENTS.md` and the mandatory UI platform architecture rules constrain code evolution so raw or unverified UI evidence cannot reach a POM contract, generated Java, or an executable test.

At completion, the console prints a compact nine-stage progress view. Full discovery, prompt, compile, review, smoke, and TestNG execution diagnostics remain in artifacts rather than being collapsed into an optimistic console status.

## GitHub Actions

Run **Build Week OrangeHRM Demo** manually from Actions. Choose `without-db` or `with-db`. Configure repository secrets:

```text
OPENAI_API_KEY
TEST_VALID_USERNAME
TEST_VALID_PASSWORD
KNOWLEDGE_GRAPH_NEO4J_PASSWORD   # with-db only
```

`without-db` performs one cold baseline run. It uploads `without-db-summary.md` and `.json` beside the regular runtime evidence.

`with-db` is intentionally a two-run workflow on the same clean GitHub runner:

```text
Neo4j + Qdrant start empty
  -> Run 1: knowledge seed
  -> persistence, promotion, and stable artifact write
  -> Run 2: measured knowledge reuse
```

The services and `target/ai-run-history/stable` are preserved between those two runs. The measured reuse summary is therefore expected to show cache/registry hits and avoided LLM calls only when the seed evidence passed the normal promotion, compile, review, smoke, and runtime-feedback gates. The uploads include separate `with-db-seed-summary.md` and `with-db-reuse-summary.md` artifacts, plus the seed evidence snapshot, final reuse evidence, stable artifact store, discovery evidence, Surefire reports, and namespaced generated sources even when a gate fails.
