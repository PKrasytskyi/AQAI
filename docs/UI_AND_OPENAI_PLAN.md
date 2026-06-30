# UI Completion and OpenAI Re-Enable Plan

## Goal

Finish the UI layer first, make the generated Selenium/TestNG output stable, and only then reconnect OpenAI in a controlled way.

---

## Phase 1. Finish the UI layer

### Step 1. Stabilize support package boundaries

Goal:

- make sure generated test code imports only stable support classes;
- avoid mixed ownership between runtime support in `src/main/java` and dedicated unit tests under `src/test/ua.demo.agentlab/unity`.

Actions:

- decide which support classes belong to runtime support and which belong only to test support;
- keep `BasePage`, Selenium actions, and driver support in one stable place;
- keep compile fixtures or integration-only helpers out of runtime support unless generated code needs them directly.

Done criteria:

- generated page objects and tests import consistent support packages;
- no generated file depends on an unstable or legacy support type.

### Step 2. Regenerate generated pages and tests from the new templates

Goal:

- align generated artifacts with the latest template changes.

Actions:

- rerun the orchestration pipeline;
- replace old generated files that still reflect outdated template contracts;
- verify that typed data objects are used where expected.

Done criteria:

- generated page objects compile against current support classes;
- generated tests compile against current `BaseTest` helpers.

### Step 3. Close scenario-template gaps

Goal:

- make sure each canonical flow type has complete action and assertion coverage.

Actions:

- audit every canonical flow type;
- check whether page prerequisites are correct;
- verify helper page injection logic for cross-page scenarios;
- verify that every scenario uses the right data source.

Done criteria:

- no missing branch in `UiActionTemplateLibrary`;
- no brittle or placeholder assertion left in `AssertionTemplateLibrary`.

### Step 4. Strengthen page object generation rules

Goal:

- make generated POM output more reusable and less heuristic.

Actions:

- add stronger composite methods for common workflows;
- reduce raw low-level actions in generated tests;
- improve naming for locators and semantic methods;
- add guardrails for unsupported locator strategies.

Done criteria:

- generated tests mostly call semantic page methods instead of low-level actions;
- generated page objects are readable and reusable.

### Step 5. Verify generated-code compile and test flow

Goal:

- ensure the workflow produces compilable UI automation artifacts end to end.

Actions:

- run `mvn compile`;
- run `mvn test-compile`;
- run targeted generated UI tests;
- fix remaining compilation or contract mismatches.

Done criteria:

- pipeline output reaches at least successful `test-compile`;
- at least one stable generated UI flow can be executed end to end.

### Step 6. Add UI-layer traceability

Goal:

- connect generated files back to source scenarios and normalized requirements.

Actions:

- persist source references into generated classes or metadata artifacts;
- add trace artifacts to workflow state;
- expose trace summary in reporting.

Done criteria:

- each generated UI test can be traced back to requirement source and scenario id.

---

## Phase 2. Prepare the project for OpenAI

### Step 7. Restore clean OpenAI integration boundary

Goal:

- re-enable OpenAI without coupling it to the rest of the system.

Actions:

- restore the OpenAI SDK dependency;
- keep OpenAI behind generator interfaces only;
- keep `DemoRunner` wiring explicit and optional for AI mode.

Done criteria:

- non-AI workflow still works without OpenAI;
- AI workflow is optional and isolated.

### Step 8. Add environment and startup validation for AI mode

Goal:

- fail clearly before runtime if AI mode is misconfigured.

Actions:

- validate `OPENAI_API_KEY`;
- validate model selection source;
- validate timeout and usage strategy;
- print a clear message when AI mode is requested but not available.

Done criteria:

- AI mode fails fast and explainably;
- no hidden runtime crash inside the generator constructor.

### Step 9. Reconnect OpenAI only to the right layers

Goal:

- use AI where it adds value, not where templates already solve the problem deterministically.

Recommended use:

- functional test plan enrichment;
- requirement ambiguity detection;
- locator suggestion fallback;
- failure analysis support;
- controlled healing proposals.

Avoid using AI for:

- raw page object boilerplate already covered by templates;
- trivial test class wrappers;
- fixed framework plumbing.

Done criteria:

- AI is complementary to templates, not a replacement for deterministic code generation.

### Step 10. Add AI prompt contracts around templates and policy

Goal:

- make OpenAI aware of framework policy, selector policy, and available templates.

Actions:

- pass normalized requirements instead of raw text;
- pass active generation policy;
- pass available template metadata from `TemplateRegistry`;
- pass project context hints from `ProjectContextScanner`.

Done criteria:

- AI output is constrained by policy and template availability;
- token usage is reduced because AI is not asked to invent full framework structure.

### Step 11. Add review and approval gates for AI-influenced output

Goal:

- keep AI output under control before it reaches committed code.

Actions:

- mark AI-assisted artifacts explicitly;
- require compile success;
- require review findings to be visible;
- later add human approval for healing or structural changes.

Done criteria:

- AI output never bypasses compile and review gates.

---

## Recommended execution order

If we keep momentum and reduce risk, the best order is:

1. stabilize support package ownership;
2. regenerate UI artifacts;
3. fix scenario coverage gaps;
4. improve page-object generation;
5. reach stable `test-compile` and targeted test execution;
6. add traceability;
7. restore OpenAI SDK boundary;
8. add startup validation;
9. reconnect OpenAI to planning and controlled assistive tasks;
10. add approval and review protections.

---

## Practical next milestone

The next strong milestone for the project should be:

`Generated Selenium UI flow compiles, runs, and uses only template-driven support classes and typed test data.`

Only after that milestone is complete should OpenAI be moved back from placeholder mode into active use.
