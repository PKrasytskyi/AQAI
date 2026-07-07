# Project Detailed Guide

## 1. Purpose

This project is an orchestration framework for requirement-driven test generation.

The target state is not "generate one test file from one prompt".

The target state is:

1. analyze input requirements;
2. understand the target UI;
3. build a canonical internal representation of intended flows;
4. derive independent test cases;
5. derive shared page-level contracts;
6. generate maintainable automation assets that match framework rules.

The current primary stack is:

- Java 17
- Maven
- Selenium
- TestNG

## 2. Active Architecture

The active UI path is now centered around canonical test cases, confirmed page evidence, `pom-contract-v1`, and deterministic Page Object Java generation.

### 2.1 High-level flow

```mermaid
flowchart LR
    A["Requirement source"] --> B["RequirementReaderAgent"]
    B --> C["RequirementNormalizationAgent"]
    C --> D["UiDiscoveryAgent"]
    D --> E["UiPageMappingAgent"]
    E --> F["FlowScopedKnowledgeAgent"]
    F --> G["RequirementToTestCaseAgent"]
    G --> H["Canonical test cases"]
    H --> I["UiTestPlanAgent"]
    I --> J["Assertion and expected-result contracts"]
    J --> K["PromptUiEvidence"]
    K --> L["POM contract prompt"]
    L --> M["pom-contract-v1"]
    M --> N["PomContractQualityGate"]
    N --> O["DeterministicPomJavaWriter"]
    O --> P["Generated Page Objects"]
```

### 2.2 Main principle

The system should generate:

- one shared Page Object per page;
- one or more atomic tests per requirement;
- no test that depends on another test;
- no test that bypasses the page-object API.

At the current checkpoint, LoginPage Page Object generation is stable enough to serve as the golden UI vertical slice. UI test generation is still kept out of the LLM path until test contracts and deterministic test writing are brought to the same quality level.

## 3. Why `TestPlan` is no longer the primary path

The repository previously leaned on:

`requirements -> test plan -> ui plan -> code`

That created two problems:

1. the planner layer became a noisy intermediate abstraction for UI generation;
2. the system could fall back to old heuristics that were not truly target-aware.

The active pipeline now focuses on:

`requirements -> canonical test cases -> UI scenarios -> shared page contracts -> code`

Legacy test-plan classes are still available for future use cases, but they have been isolated into:

`ua.demo.agentlab.futurefeat.testplan`

This keeps the repository flexible without letting old planner assumptions leak back into the current UI flow.

## 4. Core Data Flow

### 4.1 Requirements

Packages:

- `ua.demo.agentlab.requirements.model`
- `ua.demo.agentlab.requirements.source`
- `ua.demo.agentlab.requirements.agent`
- `ua.demo.agentlab.requirements.normalization`
- `ua.demo.agentlab.requirements.normalization.model`
- `ua.demo.agentlab.requirements.normalization.agent`

Important classes:

- `RequirementInput`
- `RequirementDocument`
- `RequirementReaderAgent`
- `NormalizedRequirement`
- `NormalizedRequirementBundle`
- `RequirementNormalizationAgent`

Responsibilities:

- load raw requirement content;
- preserve source references;
- normalize statements into structured items;
- mark UI and API relevance.

### 4.2 Discovery and mapping

Packages:

- `ua.demo.agentlab.ui.discovery`
- `ua.demo.agentlab.ui.discovery.agent`
- `ua.demo.agentlab.ui.discovery.selenium`
- `ua.demo.agentlab.ui.discovery.mapping`
- `ua.demo.agentlab.ui.discovery.persistence`

Responsibilities:

- open the target UI safely;
- capture page snapshots;
- extract interactive elements and forms;
- infer transitions and routes;
- persist discovery artifacts;
- persist knowledge nodes and vector documents.

This is where the platform begins to move from requirement-only reasoning toward actual UI-aware generation.

### 4.3 Flow scoping

Packages:

- `ua.demo.agentlab.ai.flow`
- `ua.demo.agentlab.ai.context`

Responsibilities:

- resolve business-flow signals from requirements and discovered UI;
- identify relevant routes, pages, and interactions;
- reduce noise before generation;
- prepare scoped context for downstream deterministic or AI stages.

### 4.4 Canonical test cases

Packages:

- `ua.demo.agentlab.testcase.agent`
- `ua.demo.agentlab.testcase.generator`
- `ua.demo.agentlab.testcase.planning`
- `ua.demo.agentlab.testcase.model`

Important classes:

- `CanonicalTestCase`
- `CanonicalTestCaseBundle`
- `RequirementToTestCaseGenerator`
- `ScenarioPipelineRequirementToTestCaseGenerator`
- `RequirementToTestCaseAgent`

Responsibilities:

- convert normalized requirements into independent test cases;
- keep requirement references;
- store operation intents;
- store assertion intents;
- store prerequisite flow;
- keep target pages explicit.

### 4.5 UI scenarios

Packages:

- `ua.demo.agentlab.ui`
- `ua.demo.agentlab.ui.generator`
- `ua.demo.agentlab.ui.agent`

Important classes:

- `UiTestPlan`
- `UiTestScenario`
- `CanonicalTestCaseUiPlanGenerator`
- `UiTestPlanAgent`

Responsibilities:

- translate canonical test cases into executable UI scenario contracts;
- preserve source page, target page, route, actions, assertions, operation intents, and locator hints.

## 5. Page Evidence and Prompt Contract Layer

The current architecture no longer relies on product-specific page roles such as catalog/details/cart. Page object prompts are built from confirmed capability and route evidence.

Important contracts:

- `MappedUiKnowledgeRaw`
- `MappedUiKnowledgeCurated`
- `PromptUiEvidence`
- `ConfirmedPageRegistry`
- `AssertionContract`

### 5.1 Why it exists

Without a prompt-ready evidence layer, raw discovery facts can leak into LLM prompts and produce:

- stale locators;
- cross-route page evidence;
- unsupported page methods;
- page names inherited from another application.

The prompt contract layer fixes this by narrowing each prompt to the target page, target route, requirement ids, required actions, assertion contracts, allowed locators, forbidden evidence, and trace metadata.

### 5.2 What it does

For each confirmed page scope, the platform:

- resolves route/capability/page ownership;
- filters actions and assertions by requirement scope;
- keeps only promoted locator evidence;
- excludes external-origin and low-confidence locator candidates;
- provides deterministic expected values when requirements contain assertion requirements;
- writes prompt traces and quality reports.

### 5.3 Result

If a project contains:

- `/login` with authentication capability;
- `/secure` with authenticated-area capability;
- `/pim/viewEmployeeList` with record-list capability;

the platform should derive names such as:

- `LoginPage`
- `SecureAreaPage`
- `EmployeeListPage`

These names are consequences of evidence, not hardcoded product fallbacks.

## 6. Selenium Generation Layer

Packages:

- `ua.demo.agentlab.ui.selenium.writer`
- `ua.demo.agentlab.templates.ui`
- `ua.demo.agentlab.templates.assertions`

Important classes:

- `TemplateDrivenSeleniumWriter`
- `SeleniumTemplatePageObjectWriter`
- `SeleniumTemplateUiTestWriter`
- `SeleniumPageObjectTemplate`
- `SeleniumTestNgTemplate`
- `UiActionTemplateLibrary`
- `AssertionTemplateLibrary`

### 6.1 Generation philosophy

The deterministic writer now follows this model:

1. aggregate page contracts;
2. generate shared page objects from those contracts;
3. generate tests that only consume public page APIs;
4. keep Selenium details inside page objects.

### 6.2 Test responsibilities

Generated tests may:

- open pages;
- call public page methods;
- assemble the scenario flow;
- execute assertions.

Generated tests must not:

- use `driver.findElement(...)`;
- create new locators;
- access private page fields;
- iterate through raw Selenium elements;
- create hidden business logic.

### 6.3 Page object responsibilities

Generated page objects may:

- hold private locators;
- implement page interactions;
- implement page-level state checks;
- expose reusable public methods for tests.

## 7. Validation Layer

Packages:

- `ua.demo.agentlab.validation`
- `ua.demo.agentlab.validation.agent`

Important classes:

- `SimpleGeneratedUiContractValidator`
- `GeneratedUiContractValidationAgent`
- `MavenGeneratedCodeValidator`
- `GeneratedCodeCompileAgent`

### 7.1 Contract validation

The UI contract validator checks that generated tests:

- call only public methods that exist on generated page objects;
- use inherited BasePage methods only when allowed;
- do not call forbidden low-level Selenium constructs.

Recent strengthening added rejections for patterns such as:

- `driver.`
- `findElement(`
- `findElements(`
- `By.`
- `WebElement `
- `.elements`
- `.locators`
- `.waits`

### 7.2 Compile gate

The compile agent confirms that generated code remains syntactically valid in the current repository.

### 7.3 Review gate

The review agent provides a rule-based quality pass after generation.

## 8. AI Layer

Packages:

- `ua.demo.agentlab.ai.context`
- `ua.demo.agentlab.ai.flow`
- `ua.demo.agentlab.ai.ui.generation`
- `ua.demo.agentlab.ai.ui.prompt`
- `ua.demo.agentlab.ai.ui.parser`
- `ua.demo.agentlab.ai.ui.writer`

Important classes:

- `AiContextAssembler`
- `TargetAwareContextSlicer`
- `AiPageObjectSpecGenerator`
- `AiUiTestSpecGenerator`
- `AiPageObjectPromptBuilder`
- `AiUiTestPromptBuilder`

### 8.1 Current role of AI

AI is assistive, not authoritative by itself.

It receives:

- scoped normalized requirements;
- scoped flows;
- scoped mapped pages;
- retrieval context;
- deterministic UI scenario baseline;
- deterministic shared page-object baseline.

For Page Objects, the LLM is a contract planner only. It returns `pom-contract-v1` JSON with locators, structured action steps, structured assertion checks, coverage gaps, and rejected suggestions. Java bodies are generated by `DeterministicPomJavaWriter`.

### 8.2 Why deterministic baseline matters

Without a baseline, the LLM tends to:

- invent page methods;
- under-generate methods required by tests;
- drift into scenario-specific page APIs.

The current implementation now feeds deterministic shared page-object baselines into AI page-object generation, so AI starts from a reusable page contract instead of a blank canvas.

### 8.3 Current limitation

AI quality still depends on:

- scoped retrieval quality;
- prompt size;
- precision of page/action discovery;
- strictness of output parsing.

The LoginPage POM flow is now stable enough for the golden demo slice. DashboardPage also participates in that slice when authenticated discovery succeeds: route evidence, user-menu trigger, and logout link can become confirmed POM evidence. Dashboard heading validation remains a coverage gap unless discovery confirms a stable heading locator. UI test generation remains prompt-only/disabled until its contract-first writer is ready.

## 9. Main Packages Worth Knowing

### 9.1 Orchestration

`src/main/java/ua/demo/agentlab/orchestration`

- `WorkflowAgent`
- `WorkflowState`
- `AgentOrchestrator`

### 9.2 App bootstrap

`src/main/java/ua/demo/agentlab/app`

- `DemoRunner`

### 9.3 UI page-object contract layer

`src/main/java/ua/demo/agentlab/ui/pageobject`

- `SharedPageObjectContractAggregator`
- `SharedPageObjectSpec`

### 9.4 Future feature isolation

`src/main/java/ua/demo/agentlab/futurefeat/testplan`

This package now contains the old test-plan-first branch and related legacy planning code.

## 10. Running the Project

### Compile

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" compile
```

### Deterministic run

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java
```

### AI run

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--ai requirements/valid-login-requirement.md"
```

## 11. Known Environment Issue

In the current Windows environment, Maven/JDK may log:

`AccessDeniedException` on `.m2repo\org\slf4j\slf4j-api\2.0.17\slf4j-api-2.0.17.jar`

If the command still ends with `BUILD SUCCESS`, treat that as a local lock issue rather than a source-level compilation failure.

## 12. What the repository can do now

Already implemented:

- requirement ingestion
- requirement normalization
- Selenium discovery
- page mapping
- knowledge persistence hooks
- flow scoping
- canonical test-case generation
- UI scenario generation
- shared page-object contract aggregation
- deterministic Selenium page/test generation
- AI page/test spec generation
- contract validation
- compile gate
- review gate

Still maturing:

- stronger target-aware retrieval with less noise
- richer universal interaction model
- API generation path
- production-grade AI recovery and rerun behavior
- broader framework portability across very different products

## 13. Recommended next evaluation step

Before the next real test run, validate these things in order:

1. the requirement file is small and focused;
2. discovery artifacts show the correct pages and actions;
3. canonical test cases are atomic;
4. shared page-object specs match the expected page boundaries;
5. generated tests call only shared public page methods;
6. compile and UI contract validation stay green before execution.
