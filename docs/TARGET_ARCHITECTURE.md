# Target Architecture

## Goal
Move the framework toward a stable architecture where:

- `core` contains reusable framework behavior
- `templates` contain reusable code skeletons
- AI/orchestration maps requirements into templates
- generated code stays thin and project-specific

## Core Contracts

### `ua.demo.agentlab.core.ui`
- `BasePage`
- `BaseTest`

### `ua.demo.agentlab.core.ui.actions`
- `ElementActions`
- `DropdownActions`
- `FrameActions`
- `AlertActions`
- `WindowActions`

### `ua.demo.agentlab.core.ui.wait`
- `WaitActions`
- `WaitUtils`

### `ua.demo.agentlab.core.ui.driver`
- `DriverFactory`
- `DefaultDriverFactory`

### `ua.demo.agentlab.core.ui.assertions`
- `UiAssertions`

### `ua.demo.agentlab.core.ui.locators`
- `SeleniumLocatorMapper`

### `ua.demo.agentlab.core.config`
- `UiRuntimeConfig`
- `PropertiesUiRuntimeConfig`

### `ua.demo.agentlab.core.data`
- `TestDataProvider`
- `PropertiesTestDataProvider`
- `ScenarioData`
- `UserCredentials`

### `ua.demo.agentlab.core.api.assertions`
- `ApiAssertions`

## Template Contracts

### `ua.demo.agentlab.templates.ui`
- `SeleniumPageObjectTemplate`
- `SeleniumTestNgTemplate`
- `UiActionTemplateLibrary`
- `UiScenarioTemplateContext`
- `UiOperationType`
- `SeleniumTemplateBundle`
- `SeleniumTemplateRegistry`

### `ua.demo.agentlab.templates.assertions`
- `AssertionTemplateLibrary`

## Rules For Generated Code

- generated pages must extend `ua.demo.agentlab.core.ui.BasePage`
- generated tests must extend `ua.demo.agentlab.core.ui.BaseTest`
- generated tests should use `UiAssertions` instead of open-coded assertion patterns
- generated pages should contain page-specific flows, not shared framework behavior
- shared Selenium interaction logic must stay in `core.ui.actions` and `core.ui.wait`

## Discovery/Profile Direction

- keep project adaptation in profile/discovery/policy layers
- avoid hard-wiring business/product assumptions into templates
- map requirements to canonical intents first, then to templates

## Compatibility Strategy

The legacy compatibility layers have been removed. The new `core` and `templates.ui` packages are now the only supported source-of-truth targets for generation and refactoring work.
