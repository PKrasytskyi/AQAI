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

1. [x] Stabilize support-layer ownership between runtime code and dedicated unit tests under `src/test/ua.demo.agentlab/unity`.
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
20. [ ] Execute a targeted generated UI smoke suite successfully.
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
5. [~] Refactor UI planning so that scenario-to-page mapping can come from discovery + profile, not only from hardcoded keyword rules.
6. [ ] Refactor templates so they consume generic page/flow metadata instead of assuming one demo target.
7. [ ] Stabilize one end-to-end onboarding flow: `project profile -> requirements -> discovery -> UI plan -> generated code -> compile -> smoke run`.
8. [ ] Restore OpenAI only after step 7 is stable and connect it first to planning, ambiguity detection, and controlled discovery assistance.
9. [ ] Keep AI out of framework plumbing generation when deterministic templates already cover the need.
10. [ ] Add human approval gates for ambiguous locator selection, inferred assertions, and healing proposals.

### Recommended Execution Order

1. [ ] Finish Track A through stable compile, test-compile, and smoke execution.
2. [ ] Execute Track D steps 1-7 to remove demo-target thinking from the UI architecture.
3. [ ] Add traceability and evidence basics from Track A and Track C.
4. [ ] Re-enable OpenAI according to Track B and Track D with strict boundaries.
5. [ ] Expand AI usage only after deterministic UI generation is stable.
6. [ ] Return to API, healing, approval, and broader execution tracks after the UI slice is product-stable.

---

## Definition of Success

The project will be considered successful when it can:

- read requirements from configurable external sources;
- normalize them into a traceable canonical model;
- generate UI and API automation through templates and rules;
- validate generated code before acceptance;
- analyze failed executions from collected evidence;
- propose safe changes with human approval instead of uncontrolled self-modification.
