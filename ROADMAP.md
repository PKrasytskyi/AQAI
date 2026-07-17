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

The immediate delivery focus is intentionally narrow and evidence-driven:

1. stabilize a capability-based SPA UI vertical slice: authentication -> authenticated area -> module navigation -> record list -> filter;
2. make `PromptUiEvidence` depend only on page-owned, live-verified or DB-stable evidence;
3. complete the Neo4j lifecycle loop: candidate -> live verification -> smoke feedback -> confirmed/degraded -> reuse;
4. prove deterministic POM output through writer, persistence, compile, review, and smoke gates;
5. keep LLM usage constrained to typed enrichment and `pom-contract-v1` planning, never Java generation.

API expansion, failure healing, approval workflows, and external-source adapters remain planned product work. They must not weaken the current UI/SPA evidence boundary.

---

## Current Baseline

The active architecture is no longer a sequential demo workflow. It currently contains:

- dependency-based orchestration through typed `WorkflowArtifact` contracts, `PipelineAgent`, `PipelineArtifactStore`, and a DAG-resolving `AgentOrchestrator`;
- project-profile driven onboarding with YAML profile resolution, environment/JVM overrides, and file/URL requirement inputs;
- normalized and capability-first requirements, typed assertion contracts, canonical test cases, and structured SPA behavior contracts;
- Selenium discovery, PageModel mapping, locator quality scoring, semantic actions, SPA inventory, component interaction graphs, and targeted live verification;
- PromptUiEvidence filtering, runtime skill-based LLM prompts, JSON Schema validation, deterministic POM Java generation, source maps, compile/review/smoke gates;
- Neo4j/Qdrant knowledge retrieval plus artifact reuse, lifecycle promotion, namespace/version controls, and DB impact metrics;
- an API generation foundation for endpoint evidence, DTO/client/test specs, quality gating, and RestAssured/TestNG writing.

`WorkflowState` is now primarily a compatibility run envelope/read model. It is not fully removed, so typed artifact ownership remains an active architecture-maintenance task.

## 2026-07 Definition of Success Assessment

This review is based on the current source tree, the checked-in unit suite, and the latest SPA artifact analysis. `[x]` means implemented and covered by the platform test suite; `[~]` means functional in a constrained vertical slice but not yet product-complete; `[ ]` means not yet implemented as an executable product capability.

| Definition of Success capability | Status | Evidence and boundary |
| --- | --- | --- |
| Read requirements from configurable external sources | `[~]` | File and URL readers work through `RequirementSource`; CSV, Jira, and TestRail adapters are not implemented. |
| Normalize into a traceable canonical model | `[x]` | `NormalizedRequirement`, structured capability sections, assertion contracts, canonical test cases, and POM source maps exist. Traceability still needs a single cross-layer query model. |
| Generate UI and API automation through templates and rules | `[~]` | UI POM contract -> deterministic Java is implemented. API has specs, quality gate, and RestAssured writer, but a full CRUD demo run is not yet the release bar. Generated UI tests remain prompt-only. |
| Validate generated code before acceptance | `[~]` | POM writer -> persistence -> compile -> review -> smoke is wired and unit-tested. The new Recruitment/Vacancies SPA vertical slice still needs a clean live proof after the current evidence-scope fixes. |
| Analyze failed executions from collected evidence | `[~]` | Discovery/runtime artifacts, screenshots, HTML, logs, source maps, and needs-review artifacts exist. There is no complete failure classification -> remediation workflow yet. |
| Propose safe changes with human approval | `[ ]` | Policy flags and needs-review artifacts exist; executable healing proposals, approval records, and controlled patch application do not. |

**Conclusion:** the repository is a strong AI-assisted UI POM platform and an advanced SPA discovery prototype. It does **not** yet meet the full product Definition of Success. The next release gate is a repeatable SPA vertical slice, not broader feature expansion.

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

1. [~] Prevent `WorkflowState` from becoming a god object by introducing clearer typed ownership boundaries. `PipelineArtifactStore` and typed artifacts are primary for new stages; compatibility projections remain.
2. [~] Introduce a dedicated traceability model instead of relying only on string artifacts. POM source maps and generated traceability artifacts exist, but requirement ownership is not yet queryable end-to-end.
3. [x] Introduce project-level profile/config abstractions for reusable multi-project setup.
4. [x] Introduce template versioning or template set identification for controlled evolution through runtime skill/schema/template versions.
5. [~] Add contract tests for template-driven generation. POM schemas, quality-gate tests, golden prompt tests, and onboarding tests exist; UI test-spec coverage remains incomplete.
6. [~] Add regression checks for generated UI artifacts after template changes. Prompt leakage and POM contract regressions are covered; generated test regression coverage is pending.
7. [x] Keep deterministic generation logic separate from AI-assisted logic.
8. [~] Do not expand the API/healing branches until one Selenium UI vertical slice is fully stable end to end. API foundations exist; the full CRUD demo remains deferred until SPA evidence closure.

### Track D. Universal Onboarding Implementation Plan

This track is the step-by-step implementation plan for the current main objective: finish a reusable UI platform first, then reconnect AI around it.

1. [x] Decouple UI planning from `ParaBankPageCatalog` by introducing a generic `UiPageCatalog` strategy for project-specific page maps.
2. [x] Add a generic project profile model that captures base URL, auth type, selector policy, framework policy, output settings, and test data source.
3. [x] Add a UI discovery layer that can inspect a target site and propose pages, links, forms, and candidate locators.
4. [x] Introduce a canonical page/flow model that sits between raw discovery and generated page objects.
5. [x] Refactor UI planning so that scenario-to-page mapping can come from discovery + profile, not only from hardcoded keyword rules.
6. [x] Refactor templates so they consume generic page/flow metadata instead of assuming one demo target.
7. [~] Stabilize one end-to-end onboarding flow: `project profile -> requirements -> discovery -> UI plan -> generated code -> compile -> smoke run`. Authentication is proven; the protected SPA Recruitment/Vacancies flow is the active completion target.
8. [x] Re-enable OpenAI only as controlled JSON-contract enrichment/planning with schema, prompt, quality, and deterministic-writer boundaries.
9. [x] Keep AI out of framework plumbing generation when deterministic templates already cover the need.
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
- `[x]` Dedicated artifact registry terminology, contracts, and Neo4j writer MVP exist for validated generated artifacts.
- `[x]` POM contract generation checks a compact, deterministic stable-artifact fingerprint before calling the LLM.
- `[x]` Neo4j artifact registry schema models `Run -> PRODUCED/REUSED -> Artifact`, `Artifact -> Page`, and `Artifact -> QualityGate`.
- `[x]` Flow contracts and semantic candidates are modeled as first-class graph capabilities; reuse remains conservative and requires Neo4j exact confirmation.

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

Status: `[x]` Implemented as `ua.demo.agentlab.artifactreuse.model`. The boundary is explicit:

- artifact reuse owns generated outputs, fingerprints, quality status, writer/compile/smoke validation, file paths, and reuse counters;
- UI knowledge graph owns page, route, locator, component, state, capability, assertion, and flow facts;
- POM contract reuse is an artifact-layer decision; flow/path reuse remains a knowledge-graph decision.

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

Status: `[x]` MVP implemented as `Neo4jArtifactRegistry`.

Implemented classes:

- `ArtifactRegistry`
- `ArtifactRegistryWriteRequest`
- `ArtifactRegistryWriteResult`
- `Neo4jArtifactRegistry`
- `Neo4jArtifactRegistrySchema`

The MVP writes:

- Neo4j uniqueness constraints for `Artifact`, `Page`, `Run`, and `QualityGate`;
- `(:Artifact)-[:GENERATED_FOR]->(:Page)`;
- `(:Run)-[:PRODUCED]->(:Artifact)` or `(:Run)-[:REUSED]->(:Artifact)`;
- `(:Artifact)-[:VALIDATED_BY]->(:QualityGate)`.

Runtime integration before the POM LLM call is implemented for POM contracts in E2-E4. Lifecycle promotion after writer/compile/smoke remains E5.

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

Status: `[x]` Implemented.

Implemented classes:

- `ArtifactFingerprint`
- `ArtifactFingerprintBuilder<T>`
- `CanonicalArtifactHasher`
- `PomContractFingerprintInput`
- `PomContractFingerprintBuilder`

The POM fingerprint uses page identity, route, capability, requirement IDs, confirmed prompt locators, required actions, required assertions, prompt template version, schema version, model, temperature, generation mode, and retrieval mode. It explicitly excludes run IDs, timestamps, temporary paths, token counts, and debug diagnostics.

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

Status: `[x]` Implemented for POM contracts.

Implemented classes:

- `FileBackedStableArtifactStore`
- `StableArtifactLookup`
- `StableArtifactWriteResult`

Current output:

```text
target/ai-run-history/stable/pom-contracts/<PageName>.<fingerprint>.pom-contract.json
```

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

Status: `[x]` Implemented for POM contracts before `OpenAiResponseGenerationClient.generate(...)`.

Implemented classes:

- `ArtifactLookupRequest`
- `ArtifactLookupResult`
- `ArtifactReusePolicy`
- `ArtifactReuseDecision`
- `ArtifactReuseDecisionType`
- `ArtifactReusePolicyInput`

Current behavior:

- prompt artifacts are still written for audit;
- prompt quality gate still runs before reuse;
- fingerprint is calculated from curated prompt evidence and stable config;
- Neo4j registry is queried for a matching POM artifact;
- full contract JSON is loaded from the file-backed store;
- if registry + file hit and policy returns `REUSE_STABLE`, the LLM call is skipped;
- if no reusable artifact exists, OpenAI is called and the parsed contract is written to the file-backed store and registered in Neo4j as `SCHEMA_VALIDATED`;
- run artifacts expose hit/miss/decision/token-saved estimate fields.

Only artifacts with `STABLE` status participate in reuse lookup. A `SCHEMA_VALIDATED` JSON file may exist on disk but cannot be reused before lifecycle promotion.

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

Status: `[x]` Implemented for POM contracts.

- `artifact-lifecycle-promotion-agent` runs after deterministic writer, persistence, compile, review, and generated smoke validation;
- it writes `validation/artifact-lifecycle-result.json` with one entry per POM contract;
- a produced contract is promoted to `STABLE` only when writer, compile, review, generated smoke, and optional live smoke all pass; a skipped live smoke is accepted only when live smoke is disabled by configuration;
- compile, review, or smoke failures leave the artifact non-reusable; the policy and Neo4j lookup now accept only `STABLE`;
- reuse runs preserve the existing stable registry status instead of writing it back as `SCHEMA_VALIDATED`.

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

Status: `[x]` Implemented for POM contract reuse.

- `artifact-reuse-metrics-agent` runs after lifecycle promotion and writes `metrics/artifact-reuse-summary.json`;
- `run-summary.json` and `run-summary.md` receive a compact `artifactReuse` section with explicit POM LLM calls executed/skipped, cache hits/misses, token-saved estimate, lifecycle counts, stable locator reuse, and the current `flowReuse` value;
- `DbImpactComparisonReporter` now prefers these explicit metrics over prompt-file count, because a prompt file exists even when a stable artifact skips the POM LLM call;
- comparison output separately reports page-knowledge cache reuse, stable locator reuse, POM artifact reuse, POM LLM calls skipped, lifecycle status, and token-saved estimate;
- actual OpenAI usage remains authoritative when available; reused POM calls use the local tokenizer estimate recorded at the skip decision.

#### Phase E7: FlowContract MVP

After POM contract reuse works, add a capability-based flow contract layer. Flow contracts are structured reusable knowledge; E7 persists and validates them, while E8 decides whether a new requirement may reuse one.

Initial authentication flows:

- `AUTH_LOGIN_TO_DASHBOARD`
- `LOGOUT_TO_LOGIN`

Expanded generic flow taxonomy:

- `NAVIGATION` and `COLLECTION_INSPECTION`;
- `FORM_ENTRY`, `FORM_SUBMISSION`, `FORM_COMPLETION`, `SELECT_OPTION`, and `TOGGLE_CONTROL`;
- `SEARCH`, `FILTER`, `SORT`, and `PAGINATION`;
- `RECORD_OPEN`, `RECORD_CREATE`, `RECORD_EDIT`, and `RECORD_DELETE`;
- `OPEN_MODAL`, `OPEN_MENU`, and `CONFIRM_ACTION`;
- `UPLOAD_FILE`, `DOWNLOAD_FILE`, and `CONTAINER_MUTATION`.

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

Additional generic states:

- `formReady`, `formDirty`, `formSubmitted`;
- `navigationReady`, `navigationComplete`;
- `recordListVisible`, `recordDetailsVisible`;
- `modalOpen`, `filterApplied`, `sortApplied`, `pageChanged`;
- `fileSelected`, `downloadRequested`, `containerUpdated`.

Status: `[x]` Implemented as a deterministic, evidence-gated contract layer.

- `FlowContractBuilder` derives contracts from canonical test-case operations, requirement actions, canonical discovery flows, mapped pages/forms/actions, and mapped transitions;
- `FlowContractBuilderAgent` allocates `KnowledgeRunMetadata` before flow persistence and semantic indexing, so every flow bundle has the same application namespace as page knowledge;
- `CONFIRMED` requires page evidence plus route-transition, form, or relevant action evidence depending on the contract type; weaker evidence is stored as `NEEDS_REVIEW` and is not reusable;
- contracts are written to `flow-contracts/flow-contracts.json` and persisted through `Neo4jFlowContractRegistry` when `artifact.reuse.flow-contract.enabled=true` and the knowledge DB is enabled;
- the graph contains `STARTS_AT`, `ENDS_AT`, `REQUIRES_STATE`, `PRODUCES_STATE`, `HAS_STEP`, and optional `USES_ARTIFACT` relationships;
- E7 itself does not reuse a flow. E8's planner is the only layer allowed to emit a `REUSE_STABLE` decision, so flow persistence cannot silently change a test or prompt.

#### Phase E8: Reuse Planner MVP

Add a simple rule-based planner before semantic retrieval:

```text
NormalizedRequirement
 -> ReusePlanner
 -> known precondition flow decisions
 -> missing page/component/capability decisions
```

Status: `[x]` Implemented as an explainable, non-mutating decision stage.

- `ReusePlanner` emits one `RequirementReusePlan` per canonical test case with only `REUSE_STABLE`, `DISCOVER`, `NEEDS_REVIEW`, or `DISABLED` decisions;
- current-run confirmed flows are evidence only and are never misreported as reused;
- only a semantic candidate that has passed Neo4j exact confirmation can become `REUSE_STABLE`;
- missing page/route, component, or flow evidence becomes explicit discovery work rather than prompt leakage.

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

Status: `[x]` Implemented as optional candidate ranking, not a source of truth.

- `FlowSemanticIndexer` stores compact summaries for `CONFIRMED` flow contracts only, tagged with application and namespace metadata;
- `FlowSemanticCandidateService` filters Qdrant by `appId`, `baseUrlHash`, `schemaVersion`, and `flowStatus`, then validates returned `flowId`s through Neo4j;
- missing embedding credentials or disabled Qdrant produce an explicit unavailable reason in `reuse-plan.json` and do not change deterministic planning.

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

Status: `[x]` Implemented as a conservative policy plus deterministic fingerprint controls.

- `ArtifactInvalidationPolicy` evaluates fingerprint/schema/template/writer/page/action/assertion compatibility and quality, compile, smoke, and force-refresh controls;
- POM fingerprints now include `artifact.reuse.writer-version`; page, locator, action, assertion, schema, template, model, and retrieval changes are canonical fingerprint inputs;
- only lifecycle-promoted `STABLE` artifacts can be reused. A failed quality, compile, or smoke gate remains non-reusable.
- a local POM contract may be used as a registry-outage fallback only after its `.stable` validation marker is written by the lifecycle promotion stage; a raw generated JSON file is never enough.
- Flow Contracts now include `contractFingerprint`, `lastSuccessfulSmoke`, `runtimePassRate`, and `flakyRate`. A cross-run flow reuse additionally requires a successful smoke timestamp, pass rate `>= 0.90`, and flaky rate `<= 0.10`; changed flow behaviour resets these counters in Neo4j.

#### Phase E11: Config Flags

Add safe rollout flags:

```properties
artifact.reuse.enabled=true
artifact.reuse.pom-contract.enabled=true
artifact.reuse.flow-contract.enabled=false
artifact.reuse.test-data.enabled=false
artifact.reuse.policy=strict
artifact.reuse.force-refresh=false
artifact.reuse.explain-decisions=true
semantic.reuse.enabled=false
```

Status: `[x]` Implemented in `framework.properties`, the OrangeHRM YAML profile, and `RuntimeProperties` profile resolution. `KNOWLEDGE_DB_STATUS=false` still disables DB-backed POM, flow, test-data, and semantic reuse together.

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

The former sequence is superseded by the following release-oriented plan.

1. [~] **SPA evidence closure (P0).** Complete a clean `valid-author.md` run where discovery confirms Dashboard -> Recruitment -> Vacancies, targeted live verification selects component-scoped evidence, and POM prompts contain only live-verified or DB-stable locators. No empty POM may be marked successful.
2. [~] **DB locator lifecycle proof (P0).** Run the same capability flow at least twice with Neo4j enabled and verify `CANDIDATE -> CONFIRMED` promotion only after live verification plus smoke feedback. Then prove the next run reuses the stable locator/artifact without an unnecessary POM LLM call.
3. [ ] **SPA interaction acceptance suite (P0).** Add executable acceptance coverage for `MODULE_NAVIGATION`, `FILTER` readiness, `FILTER` with scenario data, `USER_MENU -> LOGOUT`, and a negative path that produces actionable `needs-review` evidence.
4. [ ] **POM/test contract closure (P1).** Keep LLM output JSON-only; complete deterministic UI test-spec writing and run generated test code through compile/review/smoke. Make requirement -> contract -> field/method -> generated source traceability queryable from one artifact.
5. [ ] **Failure-analysis and review loop (P1).** Consolidate runtime screenshot/DOM/log/source-map evidence into typed failure classification and human-review records. Do not implement automatic code mutation in this phase.
6. [ ] **API CRUD demo closure (P1).** Demonstrate endpoint discovery/seed -> canonical API case -> typed assertions -> RestAssured/TestNG source -> quality gate -> compile -> controlled CRUD execution.
7. [ ] **Product onboarding expansion (P2).** Add CSV first, then Jira/TestRail adapters behind `RequirementSource`; add profile validation and fixture-based acceptance tests per source.
8. [ ] **Controlled healing and approval (P2).** Add proposal, approval, diff, targeted rerun, and audit contracts only after failure evidence is stable.

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

The project is successful only when all of the following measurable release conditions are met:

1. **Onboarding:** file, URL, CSV, and at least one work-management adapter produce the same `NormalizedRequirementBundle` contract with source traceability.
2. **UI:** two independent project profiles complete `requirements -> discovery -> semantic evidence -> POM contract -> deterministic Java -> compile -> review -> live smoke`; one profile must include a protected SPA flow beyond login.
3. **Evidence:** POM prompts contain only page-owned, confirmed current-run or DB-stable evidence. Candidate/fallback/excluded evidence never becomes an allowed locator or generated method.
4. **Knowledge reuse:** stable page/locator/artifact reuse is namespace-safe, lifecycle-gated, observable in run metrics, and rejected after degraded runtime/compile/smoke feedback.
5. **API:** one full CRUD API profile completes endpoint evidence -> typed API contract -> RestAssured/TestNG source -> quality gate -> compile -> controlled execution.
6. **Failure handling:** a failed generated flow produces a typed evidence bundle, classification, human-review item, and targeted rerun proposal; no fix is silently applied.
7. **Governance:** every generated source and reusable artifact is traceable to requirements, profile, schema/template versions, quality gates, and approval state where applicable.

Until all seven conditions are met, the platform should be described as an **AI-assisted QA automation platform in active product stabilization**, not as a fully autonomous test-generation product.
