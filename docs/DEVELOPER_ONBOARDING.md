# Developer Onboarding and Delivery Guide

> **Living document.** Update this file in the same change set whenever a supported run mode, required configuration key, quality gate, artifact path, or branch/release process changes.
>
> Last reviewed: 2026-07-15
> Stable integration baseline: `dev` and `main` contain the Login -> Dashboard -> User Menu -> Logout happy path.

## 1. Platform Boundary

AgentLab is a requirement-driven Java automation platform. It turns requirement evidence and browser/API discovery into typed contracts, then generates deterministic automation artifacts behind executable quality gates.

The platform uses an LLM only for bounded JSON planning/enrichment. It does **not** let an LLM write Java source directly:

```text
requirements + discovery + knowledge
  -> typed evidence and POM contract JSON
  -> deterministic Java writer
  -> compile, review, smoke, and feedback
```

The current stable vertical slice is capability-based:

```text
AUTHENTICATION
  -> AUTHENTICATED_AREA
  -> USER_MENU
  -> LOGOUT
```

The OrangeHRM profile demonstrates that slice. SPA inventory, targeted verification, component flows, module navigation, filters, tables, and modals are active stabilization work. They must not be presented as a release guarantee until their own evidence, compile, and live-smoke gates are green.

## 2. Prerequisites

- Java 17
- Maven 3.9+
- Chrome/Chromedriver compatible with the local Selenium setup for UI discovery
- Docker Desktop only when Neo4j/Qdrant-backed knowledge is required
- Optional OpenAI API key for AI enrichment/POM contract planning
- A dedicated, non-production account for authenticated discovery and live smoke

Run the unit suite before changing workflow behavior:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test
```

Platform unit tests are under:

```text
src/test/java/unit/tests
```

## 3. Project Profile and Configuration

The platform reads its runtime identity from a YAML project profile. The checked-in example is:

```text
src/main/resources/profiles/orangehrm.project-profile.yaml
```

`framework.properties` provides the default profile path:

```properties
project.profile.file=profiles/orangehrm.project-profile.yaml
```

The profile owns the application base URL, explicit routes, authentication settings, requirements file, AI/knowledge switches, artifact reuse, and SPA discovery configuration. Do not hardcode product routes or page names in workflow code.

Secrets and environment-specific values belong in environment variables or JVM properties, never in committed profiles:

```text
OPENAI_API_KEY
RAG_OPENAI_API_KEY
KNOWLEDGE_GRAPH_NEO4J_PASSWORD
API_AUTH_TOKEN
TEST_VALID_USERNAME
TEST_VALID_PASSWORD
KNOWLEDGE_DB_STATUS
```

`KNOWLEDGE_DB_STATUS=false` disables RAG, Neo4j, Qdrant, and artifact reuse together for a clean no-DB comparison run. `true` enables the configured knowledge integrations; reachable services and required keys are still necessary.

## 4. Run Modes

### Deterministic UI discovery

Use a requirement file explicitly when checking a new fixture:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=requirements/valid-login-requirement.md"
```

### AI POM-contract run

This can call the LLM for enrichment and `pom-contract-v1` planning. Java Page Object output remains deterministic.

```powershell
$env:OPENAI_API_KEY="..."
$env:TEST_VALID_USERNAME="..."
$env:TEST_VALID_PASSWORD="..."
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="..."

mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md"
```

### No-DB comparison run

```powershell
$env:KNOWLEDGE_DB_STATUS="false"
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md"
```

### DB-backed run

Start Neo4j and Qdrant first. See [Graph and Vector DB Setup](GRAPH_AND_VECTOR_DB_SETUP.md).

```powershell
$env:KNOWLEDGE_DB_STATUS="true"
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md"
```

### API demo

```powershell
$env:API_AUTH_TOKEN="..."
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--api requirements/valid-author.md"
```

The API path is an MVP. It is safe for endpoint/spec generation and controlled CRUD demo flows; it is not a general production API test generator yet.

## 5. Where to Review a Run

Start with the compact reports, in this order:

1. `target/ai-run/run-summary.md`
2. `target/ai-run/quality/run-quality-summary.json`
3. `target/ai-run/page-objects/`
4. `target/ai-run/validation/`
5. `target/ai-run/need-review/`

For SPA investigation, use:

```text
target/discovery/spa-inventory.json
target/discovery/component-interaction-graph.json
target/discovery/spa-targeted-verification.json
target/discovery/spa-live-targeted-verification.json
target/discovery/spa-structured-behavior-bindings.json
```

Only enable verbose diagnostics when a compact report is insufficient:

```text
-Dai.debug.artifacts=true
```

This writes raw context, scope traces, and pipeline snapshots under `target/ai-run/debug/`.

## 6. Debugging Rules

- A `CANDIDATE`, `FALLBACK`, `DEGRADED`, or `NEEDS_REVIEW` locator is not valid POM evidence.
- Only `CONFIRMED_LOCATOR` evidence may enter `Allowed locators` and a POM contract.
- A missing route, component root, postcondition, or prerequisite must create a focused coverage gap/need-review item, not a guessed method or locator.
- For SPA flows, verify the dependency graph, for example `openUserMenu -> logout visible -> logout`, before judging a child locator in isolation.
- Treat a Qdrant miss as observable optional degradation. It is not equivalent to a Neo4j stable-cache hit.
- A quality score is credible only when its compile, review, smoke, and DB status artifacts are present.

## 7. Documentation Map

Authoritative operational documents:

| Document | Use it for | Update trigger |
|---|---|---|
| [README](../README.md) | repository overview and first commands | public-facing workflow or setup changes |
| This document | onboarding, delivery process, supported run modes | every runtime/process change |
| [Architecture Guide](../ARCHITECTURE.md) | high-level contracts and module boundaries | architecture boundary changes |
| [Current AI UI Architecture](CURRENT_AI_UI_ARCHITECTURE.md) | detailed UI/AI/knowledge pipeline | UI/AI/DB contract changes |
| [SPA Semantic Understanding Engine](SPA_SEMANTIC_UNDERSTANDING_ENGINE.md) | SPA inventory, verification, and lifecycle | SPA capability changes |
| [API Layer](API_LAYER.md) | API MVP and CRUD path | API pipeline changes |
| [Roadmap](../ROADMAP.md) | release gates and future work | milestone status changes |
| [GitHub Actions CI/CD](GITHUB_ACTIONS_CI_CD.md) | CI behavior | workflow or branch trigger changes |

The following documents are historical or deep-reference material, not the primary onboarding path: `AI_POM_FIX_PLAN.md`, `TARGET_ARCHITECTURE.md`, `TEMPLATE_LAYER_CHANGES.md`, `UI_AND_OPENAI_PLAN.md`, and the early RAG/MCP design notes. Preserve them for decision history until their useful content is merged into current architecture documents.

## 8. Branch and Release Policy

`main` and `dev` are protected stable branches:

- `main` is the public/release baseline.
- `dev` is the stable integration baseline. It currently carries the Login/Logout happy path.
- Do not develop new functionality directly on `main` or `dev`.

Start every new unit of work from the current `dev` branch:

```powershell
git switch dev
git pull --ff-only
git switch -c feature/<area>-<short-description>
```

Use one of these prefixes:

```text
feature/   new capability
fix/       defect correction
refactor/  behavior-preserving restructuring
docs/      documentation-only change
hotfix/    urgent correction from main
```

Delivery flow:

```text
feature/fix/refactor/docs branch
  -> focused tests and documentation update
  -> pull request / review into dev
  -> dev integration verification
  -> release pull request from dev into main
```

For an urgent public issue, branch `hotfix/...` from `main`, merge it to `main`, then merge or cherry-pick the same correction back into `dev`.

Before requesting merge into `dev`:

- keep the branch focused and short-lived;
- run the relevant unit suite and any affected compile/smoke gate;
- review `git status --short --untracked-files=all`;
- update this guide, README, architecture docs, or roadmap when their claims change;
- do not add runtime artifacts, local DB data, credentials, or generated local sources.

Recommended GitHub protection rules:

- require pull requests for `main` and `dev`;
- require the `Build and Unit Tests` check;
- block force pushes and direct deletion;
- require one approval for `main` release merges.

## 9. Definition of Done for a Platform Change

A change is ready for integration only when:

1. Its typed contract and quality gate behavior are covered by focused tests.
2. Its configuration is profile-driven and secret-safe.
3. It does not introduce project-specific routes, page names, locators, or fallback actions.
4. Its expected run artifact and its failure/need-review artifact are documented.
5. The relevant documentation and roadmap status are updated.
6. It has passed the appropriate compile/review/smoke scope for its risk level.

## 10. Maintaining This Guide

At the end of each completed milestone, update:

- the stable baseline and known limitations in section 1;
- commands and environment variables in sections 3-4;
- artifact paths and debugging guidance in sections 5-6;
- documentation ownership in section 7;
- branch/release rules if the repository governance changes.

Do not turn this into a changelog. Put historical decisions in the roadmap or a dedicated architecture decision record; keep this guide focused on what a contributor must do today.
