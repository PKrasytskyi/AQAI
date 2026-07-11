# AgentLab Roadmap

## Purpose

This document describes the technical evolution path for the project from the current demo workflow to a configurable AI-assisted test orchestration platform.

The target system should:

- ingest requirements from external systems such as Jira, TestRail, and CSV;
- normalize requirements into a stable internal model;
- split work into UI and API automation streams;
- generate tests using project templates and policy rules;
- validate generated code before it is accepted;
- analyze failed test executions using collected evidence;
- propose safe corrections with human approval.

---

## Target Outcome

The final platform should support the following end-to-end flow:

1. Receive requirements from one or more sources.
2. Normalize the requirements into a canonical internal representation.
3. Apply generation policies and framework rules.
4. Build functional test plans.
5. Split scenarios into UI and API automation plans.
6. Generate page objects, test classes, API clients, and assertion code from templates.
7. Validate generated artifacts against compile, review, and template rules.
8. Execute tests and collect evidence when failures happen.
9. Analyze failures and propose controlled fixes.
10. Apply approved fixes and rerun targeted tests.

---

## Primary Product Goal

The primary product goal is not to tune the framework for one demo site such as ParaBank.

The primary goal is to build a reusable onboarding-driven platform where a user can:

1. provide a project URL or environment entry point through configuration;
2. provide requirements and generation rules;
3. let the system normalize, classify, and discover the target UI;
4. generate Selenium-based UI automation through stable templates and policies;
5. validate the generated output;
6. later use OpenAI only as a controlled assistive layer rather than as the core engine.

In practical terms, the system should evolve toward:

- configurable project onboarding;
- reusable framework support and templates;
- UI discovery that is not hardcoded to ParaBank;
- deterministic generation first;
- AI-assisted planning and analysis second;
- human approval for risky changes.

---

## Minimal Realistic Product Scope

The realistic product promise for this repository is:

- not "give any URL and always get production-ready tests with no human input";
- but "give a project profile, URL, requirements, policy, and template set, then get generated test assets that are validated, traceable, and ready for human review or targeted execution".

This means the platform must eventually support four capability groups:

### Configurable Inputs

- project URL or environment entry point;
- auth and environment profile;
- selector and assertion policy;
- template set selection;
- framework and execution preferences;
- test data and secret sources.

### Automatic Discovery

- UI page and flow discovery;
- candidate locator extraction;
- page capability detection;
- scenario-to-page mapping support;
- target application structure hints.

### Deterministic Generation

- normalized requirement to scenario transformation;
- policy-aware template selection;
- page object generation;
- test generation;
- compile and review validation.

### Human-in-the-Loop Decisions

- approval for weak locator assumptions;
- approval for inferred or ambiguous assertions;
- approval for corrective or healing changes;
- approval for AI-influenced risky output.

---

## Current Delivery Focus

The immediate delivery focus is intentionally narrow:

1. finish one strong universal Selenium UI vertical slice;
2. remove ParaBank-specific thinking from the architecture, even if ParaBank remains a demo target;
3. stabilize template-driven generation and support contracts;
4. restore OpenAI only after deterministic UI generation is stable.

API, healing, approval, and broader execution flows should not overtake UI completion.

---

## Current Baseline

The project already contains a solid foundation:

- sequential orchestration through `WorkflowAgent` and `AgentOrchestrator`;
- requirement loading from file and URL;
- rule-based and OpenAI-backed functional planning;
- UI test planning and code generation;
- persistence of generated files;
- generated code review and compilation validation.

This means the roadmap does not start from zero. It extends the current architecture in controlled layers.

---

## Architecture Direction

The system should evolve through these logical layers:

1. Ingestion
2. Normalization
3. Policy
4. Planning
5. Generation
6. Validation
7. Execution feedback
8. Healing with approval

Each layer should remain explicit and testable. Agents should coordinate work, but domain rules should live in focused services and models.

---

## Phase 1: MVP Foundation

### Goal

Stabilize the core pipeline so that all future generation works from normalized requirements and explicit policies.

### Main Deliverables

- canonical requirement model;
- requirement normalization layer;
- first external ingestion path through CSV;
- policy model for generation rules;
- template registry contracts;
- UI and API planning split;
- compile and review gate as mandatory quality checks.

### Packages to Build

- `ua.demo.agentlab.requirements.normalization`
- `ua.demo.agentlab.requirements.normalization.model`
- `ua.demo.agentlab.requirements.normalization.agent`
- `ua.demo.agentlab.policy` 
- `ua.demo.agentlab.policy.model`
- `ua.demo.agentlab.policy.provider`
- `ua.demo.agentlab.templates`
- `ua.demo.agentlab.templates.ui`
- `ua.demo.agentlab.templates.assertions`
- `ua.demo.agentlab.planning.classification`
- `ua.demo.agentlab.api`
- `ua.demo.agentlab.api.model`
- `ua.demo.agentlab.api.agent`
- `ua.demo.agentlab.api.generator`
- `ua.demo.agentlab.api.writer`

### Key Classes

- `NormalizedRequirement`
- `NormalizedRequirementBundle`
- `SourceReference`
- `RequirementNormalizer`
- `RuleBasedRequirementNormalizer`
- `RequirementNormalizationAgent`
- `GenerationPolicy`
- `SelectorPolicy`
- `FrameworkPolicy`
- `NamingPolicy`
- `TemplateRegistry`
- `TemplateDescriptor`
- `UiApiSplitAgent`
- `ApiTestPlan`
- `ApiTestPlanAgent`

### Recommended Order Inside Phase 1

1. Implement requirement normalization.
2. Extend `WorkflowState` with normalized requirements.
3. Switch test planning to consume normalized requirements instead of raw text.
4. Add CSV requirement ingestion.
5. Add policy model and load a default policy.
6. Introduce template registry contracts.
7. Split planning into UI and API branches.
8. Add API planning skeleton.
9. Keep review and compile agents at the end of generation.

### Exit Criteria

- a requirement file or CSV source is normalized into a canonical model;
- the system can produce separate UI and API plans;
- generated code passes review and compile gates before acceptance.

---

## Phase 2: Project-Level Configurability

### Goal

Make the platform reusable across multiple projects with minimal code changes.

### Main Deliverables

- project profile configuration;
- framework selection through config;
- configurable template selection;
- configurable policy loading;
- traceability between requirements and generated artifacts;
- execution manifest for generated suites.

### Packages to Build

- `ua.demo.agentlab.config`
- `ua.demo.agentlab.trace`
- `ua.demo.agentlab.execution`
- `ua.demo.agentlab.execution.model`
- `ua.demo.agentlab.execution.agent`

### Key Classes

- `ProjectProfile`
- `FrameworkProfile`
- `OutputProfile`
- `GenerationPolicyProvider`
- `RequirementTrace`
- `GeneratedArtifactTrace`
- `ExecutionManifest`
- `GeneratedSuiteDescriptor`
- `TargetedRerunAgent`

### What Must Become Configurable

- source type and source mapping;
- output directories;
- UI framework choice;
- API framework choice;
- selector priority;
- assert style;
- naming conventions;
- approval modes;
- self-heal permissions;
- template set selection.

### Exit Criteria

- the same platform can run against more than one project profile;
- writers and policies are selected from config instead of hardcoded constructor wiring;
- every generated test can be traced back to the source requirement.

---

## Phase 3: Execution Feedback and Controlled Healing

### Goal

Support post-generation maintenance by analyzing failed tests and proposing safe corrections.

### Main Deliverables

- evidence collection model;
- failure analysis agent;
- healing proposal model;
- approval workflow;
- targeted rerun flow.

### Packages to Build

- `ua.demo.agentlab.failure`
- `ua.demo.agentlab.failure.model`
- `ua.demo.agentlab.failure.agent`
- `ua.demo.agentlab.failure.analysis`
- `ua.demo.agentlab.healing`
- `ua.demo.agentlab.healing.model`
- `ua.demo.agentlab.healing.agent`
- `ua.demo.agentlab.approval`

### Key Classes

- `FailureEvidence`
- `FailureClassifier`
- `SelectorFailureDetector`
- `AssertionFailureDetector`
- `TimingFailureDetector`
- `HealingProposal`
- `SelectorFixProposal`
- `AssertionFixProposal`
- `HumanApprovalRequest`
- `ApprovedChange`

### Required Evidence Types

- screenshot;
- page source;
- console logs;
- network logs;
- stack trace;
- locator used;
- failing step name;
- test identity;
- environment metadata.

### Healing Safety Rules

- never auto-apply silent fixes;
- always produce explanation plus diff;
- require human approval before changing generated code;
- rerun only impacted tests first;
- persist evidence and decision outcome.

### Exit Criteria

- failure analysis is evidence-based instead of heuristic guessing;
- the platform proposes fixes instead of directly mutating tests;
- approved changes can be reapplied and rerun safely.

---

## Cross-Cutting Rules

### Rule 1

Agents orchestrate steps, but domain logic lives in focused services.

### Rule 2

Every stage should write meaningful artifacts and findings into `WorkflowState`.

### Rule 3

Canonical normalized requirements become the primary input for all downstream planning.

### Rule 4

Policies and templates should be structured models, not only prompt text.

### Rule 5

Review, compile, and traceability are mandatory for generated code.

### Rule 6

Self-healing must always be human-supervised.

---

## Near-Term Backlog

This is the recommended next implementation sequence for the repository:

Status legend:

- `[x]` completed
- `[~]` in progress
- `[ ]` not started

1. [x] Add normalized requirement models.
2. [x] Add `RequirementNormalizer` and `RuleBasedRequirementNormalizer`.
3. [x] Add `RequirementNormalizationAgent`.
4. [x] Extend `WorkflowState` with normalized requirement storage.
5. [x] Refactor functional planning to use normalized requirements.
6. [ ] Add CSV source support.
7. [x] Add `GenerationPolicy` model.
8. [x] Add `TemplateRegistry` contract.
9. [ ] Add `UiApiSplitAgent`.
10. [ ] Add initial `ApiTestPlan` and `ApiTestPlanAgent`.

---

## Product-Level Task Track

This section captures the concrete implementation tasks required to move the project from a strong architectural MVP to a product-level UI generation platform with controlled OpenAI integration.

Status legend:

- `[x]` completed
- `[~]` in progress
- `[ ]` not started

### Track A. UI Layer to Product Level

1. [x] Stabilize support-layer ownership between runtime code and dedicated unit tests under `src/test/unit/tests`.
2. [x] Freeze stable contracts for `BasePage`, `BaseTest`, driver support, and test data support.
3. [ ] Regenerate all generated Selenium page objects from the latest templates.
4. [ ] Regenerate all generated Selenium/TestNG tests from the latest templates.
5. [ ] Remove or replace generated files that still depend on legacy contracts.
6. [x] Audit every `UiScenarioKind` for complete action coverage.
7. [x] Audit every `UiScenarioKind` for complete assertion coverage.
8. [ ] Audit cross-page scenarios for correct prerequisite page injection.
9. [ ] Strengthen assertion quality beyond weak URL-only or page-source-only checks where possible.
10. [ ] Strengthen page object generation so generated tests mostly call semantic page methods.
11. [ ] Reduce direct low-level Selenium actions in generated tests.
12. [ ] Improve locator naming and semantic method naming consistency in generated pages.
13. [x] Add explicit guardrails for unsupported locator strategies in the template layer.
14. [x] Verify that typed test data is used consistently for registration, transfer, and bill pay flows.
15. [ ] Add traceability from normalized requirement to UI scenario id.
16. [ ] Add traceability from UI scenario id to generated page object and generated test file.
17. [ ] Expose UI traceability summary in workflow artifacts and reporting.
18. [x] Reach stable `mvn compile`.
19. [x] Reach stable `mvn test-compile`.
20. [x] Execute a targeted generated UI smoke suite successfully.
21. [ ] Validate at least 3-5 generated end-to-end UI flows against the demo target.
22. [ ] Add evidence collection foundation for UI test failures: screenshot, page source, stack trace, environment metadata.
23. [x] Keep compile and review gates mandatory for generated UI code.

### Track B. OpenAI to Product Level

1. [ ] Restore the OpenAI SDK dependency only after the deterministic UI layer is stable.
2. [~] Re-enable `OpenAiTestPlanGenerator` as an isolated strategy implementation behind existing contracts.
3. [x] Keep non-AI workflow fully functional without OpenAI dependencies at runtime.
4. [x] Add startup validation for `OPENAI_API_KEY`.
5. [ ] Add startup validation for model selection and AI feature flags.
6. [x] Add clear failure messaging when AI mode is requested but not available.
7. [ ] Restrict initial OpenAI usage to planning and analysis tasks.
8. [ ] Prevent OpenAI from generating full framework plumbing that is already covered by templates.
9. [ ] Pass normalized requirements instead of raw requirement text into AI planning flows.
10. [ ] Pass active `GenerationPolicy` into AI-enabled planning flows.
11. [ ] Pass template metadata from `TemplateRegistry` into AI-enabled planning flows.
12. [ ] Pass project context hints from `ProjectContextScanner` into AI-enabled planning flows.
13. [ ] Require structured AI outputs instead of free-form text for planning results.
14. [ ] Mark AI-influenced outputs explicitly in workflow artifacts.
15. [~] Keep compile and review gates mandatory for AI-influenced generated code.
16. [ ] Add human approval as a mandatory gate for future healing or corrective AI changes.
17. [ ] Extend OpenAI later to failure analysis only after evidence collection is stable.

### Track C. Production Readiness Rules

1. [ ] Prevent `WorkflowState` from becoming a god object by introducing clearer typed ownership boundaries.
2. [ ] Introduce a dedicated traceability model instead of relying only on string artifacts.
3. [~] Introduce project-level profile/config abstractions for reusable multi-project setup.
4. [ ] Introduce template versioning or template set identification for controlled evolution.
5. [ ] Add contract tests for template-driven generation.
6. [ ] Add regression checks for generated UI artifacts after template changes.
7. [x] Keep deterministic generation logic separate from AI-assisted logic.
8. [ ] Do not expand the API/healing branches until one Selenium UI vertical slice is fully stable end to end.

### Track D. Universal Onboarding Implementation Plan

This track is the step-by-step implementation plan for the current main objective: finish a reusable UI platform first, then reconnect AI around it.

1. [x] Decouple UI planning from `ParaBankPageCatalog` by introducing a generic `UiPageCatalog` strategy for project-specific page maps.
2. [x] Add a generic project profile model that captures base URL, auth type, selector policy, framework policy, output settings, and test data source.
3. [x] Add a UI discovery layer that can inspect a target site and propose pages, links, forms, and candidate locators.
4. [x] Introduce a canonical page/flow model that sits between raw discovery and generated page objects.
5. [x] Refactor UI planning so that scenario-to-page mapping can come from discovery + profile, not only from hardcoded keyword rules.
6. [x] Refactor templates so they consume generic page/flow metadata instead of assuming one demo target.
7. [x] Stabilize one end-to-end onboarding flow: `project profile -> requirements -> discovery -> UI plan -> generated code -> compile -> smoke run`.
8. [ ] Restore OpenAI only after step 7 is stable and connect it first to planning, ambiguity detection, and controlled discovery assistance.
9. [ ] Keep AI out of framework plumbing generation when deterministic templates already cover the need.
10. [ ] Add human approval gates for ambiguous locator selection, inferred assertions, and healing proposals.

### Track E. DB-Backed Artifact Reuse and QA Knowledge Graph

This track captures the next DB evolution step: move from using Neo4j/Qdrant mainly as a page knowledge cache to using Neo4j as an artifact registry for validated generated outputs.

#### Goal

For identical stable inputs, the platform should skip unnecessary LLM calls and reuse already validated artifacts:

`same input + same page/context/config/schema -> stable POM contract exists -> skip LLM -> reuse artifact -> collect saved token metrics`

The later expansion is broader semantic reuse:

`new requirement -> find reusable known pages/components/capabilities/states/flows -> generate only missing parts`

#### Current State

- `[x]` Page-level knowledge cache exists for page enrichment.
- `[x]` Stable locator evidence can be queried from Neo4j and used before prompt assembly.
- `[x]` DB impact comparison reports LLM calls, token estimates, DB hits, cache hits, and compile readiness.
- `[ ]` There is no dedicated artifact registry for POM contracts.
- `[ ]` POM contract generation does not yet check a stable artifact fingerprint before calling the LLM.
- `[ ]` Neo4j does not yet model `Run -> PRODUCED/REUSED -> Artifact` for generated POM contracts.
- `[ ]` Flow reuse is not yet modeled as a first-class graph capability.

#### Complexity Assessment

Overall complexity: **High**, but suitable for staged implementation.

The highest-risk parts are:

- deterministic fingerprinting of POM contract inputs;
- safe invalidation of stale artifacts;
- inserting reuse before the LLM call without bypassing quality gates;
- preserving universal behavior across projects instead of optimizing only for Login/Dashboard;
- producing trustworthy token-saved metrics.

#### Phase E0: Terminology and Boundaries

Define and keep these concepts separate:

- `Artifact`: generated or persisted output, such as POM contract, API spec, prompt package, quality summary, or test data plan.
- `Knowledge`: structured application facts, such as page, route, locator, component, capability, state, assertion, or flow.
- `ArtifactRegistry`: metadata store for artifacts, fingerprints, quality status, file paths, and reuse counters.
- `QA Knowledge Graph`: graph of pages, flows, components, states, capabilities, and their relationships.
- `ReusePolicy`: deterministic decision layer deciding `REUSE_STABLE`, `CALL_LLM`, or `FORCE_REFRESH`.
- `ReusePlanner`: later semantic planner that decides which known paths/components/artifacts can support a new requirement.

Rule: `POM contract JSON reuse` belongs to the Artifact Reuse Layer. `Login/Dashboard path reuse` belongs to the QA Knowledge Graph.

#### Phase E1: Neo4j Artifact Registry MVP

Add a minimal but future-ready graph schema.

Required node types:

- `Artifact`
- `Page`
- `Run`
- `QualityGate`

Future node types should be compatible with the schema, but not required for the first implementation:

- `Requirement`
- `TestCase`
- `Route`
- `Component`
- `Capability`
- `Flow`
- `State`
- `Locator`
- `Assertion`
- `ApiEndpoint`

Required `Artifact` metadata:

- `artifactId`
- `artifactType`
- `targetType`
- `targetId`
- `fingerprint`
- `schemaVersion`
- `promptTemplateVersion`
- `model`
- `temperature`
- `status`
- `qualityScore`
- `writerSucceeded`
- `compileSucceeded`
- `filePath`
- `createdAt`
- `lastUsedAt`
- `reuseCount`

Required relationships:

- `(:Artifact)-[:GENERATED_FOR]->(:Page)`
- `(:Run)-[:PRODUCED]->(:Artifact)`
- `(:Run)-[:REUSED]->(:Artifact)`
- `(:Artifact)-[:VALIDATED_BY]->(:QualityGate)`

Required constraints:

- unique `Artifact.artifactId`
- unique `Page.pageId`
- unique `Run.runId`
- unique `QualityGate.gateId`
- later: composite unique key for `artifactType + targetId + fingerprint`

#### Phase E2: Artifact Fingerprint Builder

Add deterministic fingerprinting before LLM generation.

For POM contracts, fingerprint should include:

- `targetPageId`
- `route`
- `pageCapabilityContractHash`
- `approvedLocatorsHash`
- `requiredActionsHash`
- `requiredAssertionsHash`
- `expectedResultBundleHash`
- `promptTemplateVersion`
- `pomContractSchemaVersion`
- `modelName`
- `temperature`
- `generationMode`
- `retrievalMode`

Fingerprint must not include:

- `runId`
- `createdAt`
- temporary file paths
- LLM request id
- debug-only diagnostics
- token counts

Implementation classes:

- `ArtifactFingerprint`
- `ArtifactFingerprintBuilder<T>`
- `PomContractFingerprintBuilder`

Canonicalization is mandatory:

- stable-sort locators, actions, assertions, and expected values;
- normalize whitespace;
- remove timestamps and run-specific fields;
- hash canonical JSON/string with SHA-256.

#### Phase E3: File-Backed Stable Artifact Store

Neo4j should store metadata and relationships. The full artifact content should stay in files.

Recommended structure:

```text
target/ai-run-history/
  stable/
    pom-contracts/
      LoginPage.<fingerprint>.pom-contract.json
    prompt-context/
      LoginPage.<fingerprint>.context.json
    flow-contracts/
      AUTH_LOGIN_TO_DASHBOARD.<fingerprint>.flow.json
```

Neo4j stores `Artifact.filePath`, not the whole JSON body.

#### Phase E4: Reuse Policy Before LLM

Insert the reuse decision before `OpenAiResponseGenerationClient.generate(...)`.

New flow:

```text
PomContractGenerationInput
 -> PomContractFingerprintBuilder
 -> ArtifactRegistry.findStableArtifact(...)
 -> ArtifactReusePolicy.decide(...)
 -> if REUSE_STABLE: load artifact JSON
 -> if CALL_LLM: call LLM
 -> schema validation
 -> PomContractQualityGate
 -> DeterministicPomJavaWriter
 -> compile/review/smoke
 -> save/update ArtifactRegistry
```

Initial decisions:

- `FORCE_REFRESH` when config explicitly disables reuse for the run.
- `REUSE_STABLE` when stable artifact exists for `artifactType + targetId + fingerprint`.
- `CALL_LLM` when no stable artifact exists.
- `REGENERATE_SCHEMA_CHANGED` when schema version differs.
- `REGENERATE_PREVIOUS_INVALID` when previous artifact failed validation.

#### Phase E5: Artifact Lifecycle

Do not mark an artifact as stable immediately after LLM output.

Required lifecycle:

```text
GENERATED
 -> SCHEMA_VALIDATED
 -> QUALITY_VALIDATED
 -> WRITER_VALIDATED
 -> COMPILE_VALIDATED
 -> SMOKE_VALIDATED
 -> STABLE
```

MVP can mark `STABLE` after `WRITER_VALIDATED + COMPILE_VALIDATED`, but only if smoke validation is not configured for that run. When smoke is enabled, smoke failure must prevent stable reuse.

#### Phase E6: Run Metrics and DB Impact Reporting

Extend run-level metrics with artifact reuse:

```json
{
  "artifactReuse": {
    "enabled": true,
    "llmCallsExecuted": 2,
    "llmCallsSkipped": 2,
    "artifactCacheHits": 2,
    "artifactCacheMisses": 1,
    "tokensSavedEstimate": 3826,
    "reuseByArtifactType": {
      "POM_CONTRACT": {
        "hits": 2,
        "misses": 0
      }
    }
  }
}
```

DB impact comparison should distinguish:

- page enrichment cache hit;
- stable locator reuse;
- POM contract artifact reuse;
- flow reuse;
- LLM calls executed;
- LLM calls skipped;
- actual OpenAI token usage when available;
- tokenizer-estimated saved tokens when exact usage is unavailable.

#### Phase E7: FlowContract MVP

After POM contract reuse works, add minimal flow reuse.

Start with:

- `AUTH_LOGIN_TO_DASHBOARD`
- `LOGOUT_TO_LOGIN`

Minimum graph:

- `(:Flow)-[:STARTS_AT]->(:Page)`
- `(:Flow)-[:ENDS_AT]->(:Page)`
- `(:Flow)-[:REQUIRES_STATE]->(:State)`
- `(:Flow)-[:PRODUCES_STATE]->(:State)`
- `(:Flow)-[:USES_ARTIFACT]->(:Artifact)`

Initial states:

- `unauthenticated`
- `authenticated`
- `userMenuOpen`

#### Phase E8: Reuse Planner MVP

Add a simple rule-based planner before semantic retrieval:

```text
NormalizedRequirement
 -> ReusePlanner
 -> known precondition flow decisions
 -> missing page/component/capability decisions
```

Example output:

```json
{
  "requirementId": "REQ-DASHBOARD-REPORTS-001",
  "targetPage": "DashboardPage",
  "preconditions": [
    {
      "type": "FLOW",
      "id": "AUTH_LOGIN_TO_DASHBOARD",
      "decision": "REUSE_STABLE"
    }
  ],
  "missingKnowledge": [
    {
      "type": "COMPONENT",
      "id": "Dashboard.ReportsPanel",
      "decision": "DISCOVER"
    }
  ]
}
```

#### Phase E9: Qdrant Semantic Layer

Add semantic matching only after Neo4j exact artifact reuse is reliable.

Qdrant should provide candidates, not final reuse decisions.

Good embedding targets:

- requirement text;
- capability description;
- flow description;
- component purpose;
- assertion intent.

Flow:

```text
New requirement
 -> Qdrant candidate capabilities/flows
 -> Neo4j exact graph expansion
 -> ReusePlanner decision
```

#### Phase E10: Invalidation Policy

Reuse must be blocked when:

- fingerprint changed;
- schema version changed;
- prompt template version changed;
- writer version changed;
- quality gate failed;
- compile failed;
- smoke validation failed;
- page fingerprint changed;
- required actions/assertions changed;
- manual force refresh is enabled.

Artifact statuses:

- `STABLE`
- `STALE`
- `INVALIDATED`
- `NEEDS_REVIEW`

#### Phase E11: Config Flags

Add safe rollout flags:

```properties
ai.artifact-reuse.enabled=true
ai.artifact-reuse.pom-contract.enabled=true
ai.artifact-reuse.flow-contract.enabled=false
ai.artifact-reuse.test-data.enabled=false
ai.artifact-reuse.policy=strict
ai.artifact-reuse.force-refresh=false
ai.artifact-reuse.explain-decisions=true
ai.semantic-reuse.enabled=false
```

#### Exit Criteria

- repeated identical POM contract input skips the LLM;
- reused artifact still passes schema, quality, deterministic writer, compile, and smoke gates;
- run summary shows LLM calls executed vs skipped;
- DB impact report shows token savings from artifact reuse separately from page enrichment cache reuse;
- Neo4j contains `Run -> REUSED/PRODUCED -> Artifact` relationships;
- stale or failed artifacts are never reused silently.

#### Primary Risks and Controls

- Fingerprint instability: use canonicalization tests and golden fixtures.
- Stale artifact reuse: require lifecycle status and invalidation checks.
- Over-trusting DB cache: every reused POM contract still goes through local validation and writer/compile gates.
- Graph schema drift: add constraints and schema version fields.
- Token metrics inflation: count skipped calls from stored previous prompt/response token metadata, not guessed row totals.
- Product overfitting: keep target identifiers generic and route/capability based; avoid Login/Dashboard-only logic outside fixtures.

### Recommended Execution Order

1. [x] Finish Track A through stable compile, test-compile, and smoke execution.
2. [x] Execute Track D steps 1-7 to remove demo-target thinking from the UI architecture.
3. [ ] Implement Track E phases E1-E6 for POM contract artifact reuse before expanding semantic reuse.
4. [ ] Add traceability and evidence basics from Track A and Track C.
5. [ ] Re-enable OpenAI according to Track B and Track D with strict boundaries.
6. [ ] Expand AI usage only after deterministic UI generation and artifact reuse validation are stable.
7. [ ] Return to API, healing, approval, and broader execution tracks after the UI slice is product-stable.

### Closure Plan for Recommended Execution Order Item 2

Item 2 is closed. Track D steps 1-7 now have verified implementation evidence across the golden OrangeHRM flow and a second The Internet authentication fixture.

Current evidence:

- compile and test-compile are stable through `mvn test`;
- generated UI smoke passes for two page object files;
- live browser smoke passes through the capability/profile-driven flow `open source page -> satisfy authentication preconditions -> validate dashboard route -> open user menu -> validate logout -> logout back to login`;
- project profile, confirmed page registry, discovery, semantic model, prompt evidence, POM contract, deterministic writer, and smoke validation are wired for the current golden flow.
- scenario page resolution, business flow route selection, and prompt route formatting now use confirmed profile/discovery/cache routes instead of direct catalog/details/cart fallback routes;
- prompt route context now formats confirmed routes by `PageCapability`, not by legacy product route names such as `catalog`, `products`, `details`, or `cart`;
- POM capability contract formatting now resolves route and capability from `PromptUiEvidence` and `ConfirmedPageRegistry` before falling back to mapped knowledge.
- live browser smoke now runs through a profile/capability-driven runner with reusable phases: open source page, satisfy preconditions, validate target page, execute optional action, and validate postcondition;
- a second authentication fixture exists for `the-internet.herokuapp.com` through `profiles/the-internet.project-profile.yaml` and `requirements/the-internet-valid-login-requirement.md`.
- regression coverage now fails if unrelated POM prompts leak `ListingPage`, `DetailsPage`, `CartPage`, catalog/product fallback selectors, or unconfirmed profile routes;
- onboarding acceptance coverage now exercises `project profile -> requirements -> discovery -> UI plan -> POM contract -> deterministic Java -> compile artifact -> generated-source smoke` for the The Internet authentication fixture.
- the latest clean `with-db` run uses `stable-page-cache` with `neo4jHit=true`, `qdrantHit=true`, `stableCacheUsed=true`, `pageEnrichmentCacheHits=2`, and zero page-enrichment OpenAI calls;
- the latest clean `with-db` run emits explicit validation artifacts: `persisted-generated-sources.json`, `generated-code-compile-result.json`, `generated-code-review-result.json`, `generated-ui-smoke-result.json`, `live-ui-smoke-result.json`, and `pom-source-traceability.json`;
- prompt leakage checks confirm that `ListingPage`, `DetailsPage`, `CartPage`, catalog/product fallback labels, `/profile`, `openTargetContainer`, `addEntityToContainer`, and `removeEntityFromContainer` are absent from current POM prompts/contracts;
- generated `LoginPage` and `DashboardPage` compile successfully and the review gate reports zero findings.

Non-blocking follow-up items:

- traceability is now present, but should be refined into `ownedRequirementIds`, `prerequisiteRequirementIds`, and `flowParticipantRequirementIds`;
- LoginPage prompt ownership can still contain a non-blocking `logout()` suggestion from flow participation; the POM contract and generated Java correctly exclude it;
- generated test flow remains prompt-only/limited, so the platform proves POM generation more strongly than generated test execution;
- onboarding coverage should be expanded beyond authentication/logout once the next roadmap item starts.

Plan to close item 2:

1. [x] Replace remaining route/page fallback vocabulary with capability-only mapping where possible.
2. [x] Make `ScenarioPageResolver`, `BusinessFlowResolver`, and prompt route formatting consume only confirmed profile/discovery/cache routes.
3. [x] Refactor templates and prompt builders to consume `ConfirmedPageRegistry`, `PromptUiEvidence`, and capability contracts without project-specific page assumptions.
4. [x] Convert `LiveLoginDashboardSmokeService` into a profile/capability-driven smoke runner with reusable phases: open source page, satisfy preconditions, validate target page, execute optional action, validate postcondition.
5. [x] Add a second non-OrangeHRM fixture, preferably `the-internet.herokuapp.com` authentication, to prove the flow is not tuned to one product.
6. [x] Add regression tests that fail if `ListingPage`, `DetailsPage`, `CartPage`, catalog/product fallback selectors, or unconfirmed profile routes appear in POM prompts for unrelated projects.
7. [x] Add a final onboarding acceptance test: `project profile -> requirements -> discovery -> UI plan -> POM contract -> deterministic Java -> compile -> smoke`.

Exit criteria for item 2:

- [x] Track D steps 5, 6, and 7 are marked `[x]`;
- [x] at least two different project profiles pass the same onboarding flow;
- [x] no unconfirmed route/page names are emitted into current POM prompt generation for unrelated projects;
- [x] smoke runner is capability/profile based, not hardcoded to Login/Dashboard class names;
- [x] generated artifacts include traceability from requirement id to page contract and generated source.

---

## Definition of Success

The project will be considered successful when it can:

- read requirements from configurable external sources;
- normalize them into a traceable canonical model;
- generate UI and API automation through templates and rules;
- validate generated code before acceptance;
- analyze failed executions from collected evidence;
- propose safe changes with human approval instead of uncontrolled self-modification.
