# AI POM Fix Plan

> Status: historical stabilization plan. The examples in this document use older
> ecommerce-style page names such as `ListingPage`, `DetailsPage`, and
> `CartPage` because they describe the original failure mode. The current
> implementation uses capability/route/evidence-based page selection through
> `ConfirmedPageRegistry`, `MappedUiKnowledgeCurated`, and `PromptUiEvidence`.

## Objective
Stabilize the AI-driven UI pipeline so that:
- page object generation receives correct page-scoped context
- generated page objects cover required scenario operations and assertions
- generated tests use only valid public page-object contracts
- retrieval and discovery contribute business-relevant UI knowledge instead of navigation noise

## Status Snapshot

### Already improved in codebase
- canonical page identity layer exists:
  - `CanonicalPageType`
  - `PageIdentity`
  - `PageNamingResolver`
  - `AliasRegistry`
- cross-layer page matching is no longer raw-name-only:
  - `PageReferenceMatcher` is already wired into retrieval, slicing, planner, and writer
- route/alias/canonical-type fallback matching is already partially implemented

### Still failing in current AI runs
- `page-object-spec` can still receive `Page-scoped UI scenarios: none`
- retrieval still injects `AllPage` / `AnothertestPage` noise into `ListingPage` prompts
- canonical test cases still collapse too much behavior into `AllPage`
- generated page objects still include fallback business logic like:
  - click first available
  - open details if add-to-cart is missing
- generated tests can still call framework APIs that were never explicitly contracted in prompt context
- concrete current compile failure:
  - generated tests call `ScenarioData.optional(key, defaultValue)`
  - real framework supports only `ScenarioData.optional(String key)`

## Current Failure Summary
- `page-object-spec` prompt still receives `Page-scoped UI scenarios: none`
- canonical page naming is inconsistent between planner, discovery, mapping, and AI context
- discovery knowledge is still too DOM-oriented and not business-operation-oriented
- page object specs are incomplete for cart/add/remove/details flows
- test specs compensate for missing POM methods by inventing flows or classes
- validator catches real contract gaps, but still produces some noise on comment/member scanning
- prompt contracts still omit some real framework API details, which lets the LLM invent unsupported helper signatures

## Wave 1: Context Integrity

### Step 1. Fix page-scoped scenario slicing
Goal:
Ensure each page object prompt receives real scenarios for the requested page.

Why:
Without page-scoped scenarios, the LLM generates only a partial page API.

Targets:
- `src/main/java/ua/demo/agentlab/ai/ui/generation/AiPageObjectSpecGenerator.java`
- `src/main/java/ua/demo/agentlab/ai/context/TargetAwareContextSlicer.java`
- `src/main/java/ua/demo/agentlab/ai/context/AiContextScopeResolver.java`

Tasks:
- verify how `pageName` is matched during scoped slicing
- make scenario filtering use canonical page identity, not only raw page names
- fail early if requested page exists in UI plan but scoped scenario list is empty
- write an artifact with:
  - requested page
  - matched scenarios count
  - matched scenario ids

Expected result:
- `ListingPage-prompt.txt` contains actual page-scoped UI scenarios instead of `none`

### Step 2. Add page-scope validation gate
Goal:
Stop the workflow before AI page object generation if scoped context is structurally wrong.

Why:
Right now the workflow continues with an invalid prompt and the failure appears much later.

Targets:
- new validation component before `AiPageObjectSpecAgent`
- `src/main/java/ua/demo/agentlab/app/DemoRunner.java`

Tasks:
- add a pre-page-object validation agent
- verify for each page in UI plan:
  - page exists in scoped context
  - scenario count > 0
  - operation intents are present
  - locator hints or mapped page evidence exist
- fail with a focused message when any page is under-scoped

Expected result:
- no more silent runs where POM prompt is obviously incomplete

## Wave 2: Canonical Model Alignment

### Step 3. Unify canonical page naming
Status:
Partially completed.

Goal:
Use one page identity across planner, discovery, mapping, retrieval, and AI generation.

Why:
Current mismatch like `ListingPage` vs `DetailsPage` for `/collections` breaks scenario-to-page binding.

Targets:
- `src/main/java/ua/demo/agentlab/ui/discovery/enrichment/UiDiscoveryEnricher.java`
- `src/main/java/ua/demo/agentlab/ai/context/*`
- `src/main/java/ua/demo/agentlab/ui/generator/*`

Tasks:
- define canonical page resolution rules:
  - `ListingPage`
  - `DetailsPage`
  - `CartPage`
  - `SearchPage`
  - `AuthPage`
- normalize discovered pages to canonical names before AI context assembly
- ensure UI scenarios and mapped pages reference the same canonical names

Expected result:
- planner and discovery refer to the same logical page objects

### Step 4. Add route-based fallback matching
Status:
Largely completed, but still needs verification against prompt artifacts and planner output.

Goal:
Bind pages by route when names are inconsistent or weak.

Why:
Route is often more stable than discovered page names.

Targets:
- `AiContextScopeResolver`
- `TargetAwareContextSlicer`

Tasks:
- match page contexts by:
  - canonical page name
  - canonical route
  - normalized URL pattern
- add scoring and debug output for page match resolution

Expected result:
- page scoping survives imperfect discovery names

## Wave 3: Discovery to Business Knowledge

### Step 5. Enrich mapper with business UI semantics
Goal:
Move from raw DOM inventory to operation-aware page knowledge.

Why:
LLM currently sees too many generic links and too little business-relevant evidence.

Targets:
- `src/main/java/ua/demo/agentlab/ui/discovery/mapping/RuleBasedPageMapper.java`
- mapped page model classes

Tasks:
- classify elements into semantic roles such as:
  - `ITEM_CARD`
  - `ITEM_TITLE`
  - `ITEM_PRICE`
  - `DETAILS_LINK`
  - `ADD_TO_CART_BUTTON`
  - `REMOVE_BUTTON`
  - `CART_LINK`
  - `CART_ITEM_ROW`
  - `EMPTY_STATE`
  - `PAGE_HEADING`
- preserve candidate locators with confidence and source
- attach supported business operations to each semantic element

Expected result:
- AI can reason from `add/remove/cart/item` evidence instead of `link7`, `aboutUs`, `home`

### Step 6. Add operation-to-evidence binding
Goal:
For every scenario operation, provide concrete page evidence candidates.

Why:
Requirement text alone is not enough to generate correct page methods.

Targets:
- flow-scoped knowledge layer
- AI context packaging layer

Tasks:
- build evidence bundles for operations:
  - `OPEN_DETAILS`
  - `ADD_ENTITY_TO_CONTAINER`
  - `OPEN_TARGET_CONTAINER`
  - `REMOVE_ENTITY_FROM_CONTAINER`
  - `INSPECT_ENTITY_SUMMARY`
- build assertion evidence bundles for:
  - `ENTITY_PRESENT_IN_CONTAINER`
  - `CONTAINER_EMPTY`
  - `DETAILS_VISIBLE`
  - `COLLECTION_VISIBLE`
- include:
  - candidate elements
  - preferred locators
  - confidence
  - source page

Expected result:
- page object prompt contains actionable UI evidence for each required capability

## Wave 4: Page Object Contract Hardening

### Step 7. Introduce page capability contract
Goal:
Generate POM from a compact deterministic contract instead of a noisy long-form prompt only.

Why:
The current prompt mixes requirements, discovery, retrieval, and test plan into one unstable input.

Targets:
- new DTO in AI context layer
- `AiPageObjectPromptBuilder`

Tasks:
- create a page capability contract containing:
  - `pageName`
  - `route`
  - `requiredOperations`
  - `requiredAssertions`
  - `operationEvidence`
  - `assertionEvidence`
  - `preferredLocators`
  - `prerequisiteFlows`
- include this contract explicitly in page object prompt

Expected result:
- LLM generates methods because the contract requires them, not because it guesses them

### Step 7.1 Ban fallback business logic in generated POM
Goal:
Prevent the LLM from hiding missing evidence behind permissive fallback behavior.

Why:
Current generated methods still do things like:
- click first available item
- open details when add-to-cart button is missing
- approximate console/browser error checks via page source text

Targets:
- `src/main/java/ua/demo/agentlab/ai/ui/prompt/AiPageObjectPromptBuilder.java`
- new page-object semantic validator after parsing

Tasks:
- explicitly forbid:
  - `click first available`
  - `if not found then open details`
  - `fallback:` comments
  - page-source keyword checks as substitute for console-log validation
- require missing capability to fail validation instead of being auto-compensated
- flag generated methods that mix:
  - primary page action
  - navigation fallback
  - hidden orchestration

Expected result:
- generated page methods stay deterministic and contract-driven
- missing capability becomes visible early instead of being masked

### Step 7.2 Add page-object semantic validation
Goal:
Catch technically valid but semantically wrong POM methods before writing files.

Why:
The current pipeline can accept a compilable page method that is still wrong for the business flow.

Targets:
- new validator after `AiPageObjectSpecParser`
- `DemoRunner`

Tasks:
- reject methods that:
  - navigate to another flow as fallback
  - use first-match behavior without explicit requirement
  - represent browser console validation through `getPageSource()`
  - expose too much hidden orchestration inside one action
- classify violations separately from syntax/parser failures

Expected result:
- fewer “valid Java but wrong automation design” page objects

### Step 8. Add page-object completeness validation
Goal:
Prevent test generation when page object spec does not cover the required page contract.

Why:
Right now missing methods are discovered only after tests are already generated.

Targets:
- new completeness validator after `AiPageObjectSpecAgent`
- `DemoRunner`

Tasks:
- compare generated page object spec against page capability contract
- validate presence of required methods for each operation/assertion
- examples:
  - add-to-cart flow requires methods like:
    - `addItemToContainer`
    - `openCart` or `openDestinationContainer`
    - `isItemPresentInContainer`
  - remove flow requires methods like:
    - `removeItemFromContainer`
    - `isDestinationContainerEmpty`
- fail before `AiUiTestSpecAgent` when spec is incomplete

Expected result:
- no more runs where tests depend on methods the page object never declared

## Wave 5: Test Generation Contract Hardening

### Step 9. Tighten AI UI test generation contract
Goal:
Force tests to consume only the available public page API.

Why:
The model still invents extra classes or compensates for missing POM methods.

Targets:
- `src/main/java/ua/demo/agentlab/ai/ui/prompt/AiUiTestPromptBuilder.java`
- `src/main/java/ua/demo/agentlab/ai/ui/generation/AiUiTestSpecGenerator.java`

Tasks:
- pass only page-complete specs to test generation
- validate that every referenced page method exists in supplied spec before accepting parsed output
- expose the exact allowed helper API for framework objects, not just their names
- reject test specs containing:
  - invented classes not supplied in context
  - comments with `assume`, `simulate`, `fallback`
  - constructor usage inconsistent with project runtime

Expected result:
- test generation becomes a consumer of validated page contracts, not a second source of invention

### Step 9.1 Add framework helper API contract to test prompt
Goal:
Stop the LLM from inventing unsupported framework helper overloads.

Why:
Current compile failures show the prompt names `ScenarioData`, but does not define its real API.

Targets:
- `src/main/java/ua/demo/agentlab/ai/ui/prompt/AiUiTestPromptBuilder.java`
- post-parse validation for test specs

Tasks:
- explicitly publish allowed `ScenarioData` API:
  - `required(String key)`
  - `optional(String key)`
  - `has(String key)`
  - `requiredInt(String key)`
- forbid invented overloads such as:
  - `optional(String, String)`
  - `required(String, String)`
- add post-parse validation that rejects unsupported helper calls before file writing

Expected result:
- no more test compile failures caused by invented framework helper signatures

### Step 10. Add cross-page prerequisite model
Goal:
Handle flows that genuinely span listing, details, and cart pages.

Why:
Some failures happen because the system compresses a multi-page flow into one page object.

Targets:
- UI scenario model
- page capability contract
- AI prompt builders

Tasks:
- model:
  - starting page
  - transition steps
  - execution page
  - destination page
- allow scenario decomposition like:
  - `ListingPage.openDetailsFor(...)`
  - `DetailsPage.addItemToContainer(...)`
  - `CartPage.removeItemFromContainer(...)`

Expected result:
- no artificial overloading of `ListingPage` with behaviors that belong elsewhere

## Wave 6: Retrieval and Noise Reduction

### Step 11. Restrict retrieval to flow-relevant domains
Goal:
Reduce navigation/header/footer noise in AI context.

Why:
Current retrieval still overemphasizes `search`, `about`, `login`, `blog`, `home`.

Targets:
- `src/main/java/ua/demo/agentlab/ai/flow/FlowScopedKnowledgeService.java`
- `src/main/java/ua/demo/agentlab/ai/context/UiKnowledgeRetrievalService.java`

Tasks:
- rank evidence higher when it matches:
  - target route
  - target operation
  - item/cart/details semantics
- down-rank global navigation and non-business content
- add explicit exclusion weights for:
  - auth
  - blog
  - policy/footer
  - generic global navigation

Expected result:
- prompt context focuses on collections/products/cart instead of layout chrome

### Step 12. Add retrieval trace artifact
Goal:
Make every AI run debuggable.

Why:
Right now it is possible to see the final prompt, but not always easy to explain why the wrong nodes won.

Targets:
- AI debug artifact layer

Tasks:
- write a structured retrieval trace with:
  - query terms
  - accepted nodes
  - rejected nodes
  - ranking reasons
  - domain penalties
  - final evidence sent to prompt

Expected result:
- each bad run can be traced back to a concrete retrieval decision

### Step 12.1 Add prompt composition trace
Goal:
Explain not only retrieval ranking, but also why specific prompt sections were included.

Why:
Current failures are often caused by prompt composition drift:
- empty page-scoped scenarios
- noisy canonical cases
- irrelevant mapped pages
- stale baseline POM

Targets:
- AI debug artifact layer
- prompt builders

Tasks:
- write a structured artifact with:
  - requested page/scenario
  - scoped scenarios count
  - canonical cases included
  - mapped pages included
  - retrieval items included
  - baseline spec included
  - explicit exclusions

Expected result:
- every bad prompt can be debugged section-by-section, not only via final text dump

## Wave 7: Validator Cleanup

### Step 13. Fix validator false positives on comments
Goal:
Stop reporting valid public method usage as forbidden direct access.

Why:
Current validator likely scans comments and produces misleading errors.

Targets:
- `src/main/java/ua/demo/agentlab/validation/SimpleGeneratedUiContractValidator.java`

Tasks:
- ignore comments and string literals when scanning for member access
- keep detection for true forbidden access like:
  - `page.elements`
  - `page.driver`
  - locator field access
- preserve inherited `BasePage` public methods as allowed

Expected result:
- validator reports only real contract issues

### Step 14. Split validator output into error categories
Goal:
Make failures easier to triage.

Why:
Not all contract failures mean the same thing.

Targets:
- `SimpleGeneratedUiContractValidator`
- console/reporting output

Tasks:
- classify violations as:
  - `MissingPageMethod`
  - `ForbiddenPageInternalAccess`
  - `UnknownPageClass`
  - `PromptContractViolation`
  - `FrameworkHelperContractViolation`
  - `PageObjectSemanticViolation`
- show grouped output in workflow failure report

Expected result:
- faster debugging after each AI run

## Recommended Execution Order
1. Step 1
2. Step 2
3. Step 3
4. Step 4
5. Step 9.1
6. Step 7.1
7. Step 7.2
8. Step 8
9. Step 13
10. Step 5
11. Step 6
12. Step 7
13. Step 9
14. Step 10
15. Step 11
16. Step 12
17. Step 12.1
18. Step 14

## Definition of Done
- `page-object-spec` prompt always contains real page-scoped scenarios
- discovered pages and planned pages share canonical identity
- generated page object specs cover required operations for current UI scenarios
- generated page object specs do not hide missing behavior behind fallback flows
- generated test specs do not invent unsupported page methods or classes
- generated test specs do not invent unsupported framework helper signatures
- validator reports only real contract failures
- retrieval artifacts show business-relevant evidence for the target flow
- the workflow can produce a coherent multi-page UI contract for listing/details/cart flows
