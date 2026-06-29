# Template Layer Changes

## What was added

- `TemplateRegistry` and `DefaultTemplateRegistry` for framework-aware template resolution.
- `ProjectContext`, `ProjectContextScanner`, and `DefaultProjectContextScanner` for scanning the local project structure before template selection.
- `UiActionTemplateLibrary` for reusable Selenium action blocks.
- `AssertionTemplateLibrary` for reusable Selenium assertion blocks.
- `TestDataProvider` layer with universal models:
  - `UserCredentials`
  - `ScenarioData`
  - `UiRuntimeConfig`

## What was updated

- `DemoRunner` now scans project context and resolves templates through the new registry instead of wiring Selenium packages directly.
- `TemplateDrivenSeleniumWriter` now accepts `TemplateDescriptor`, which makes future registry-driven writer selection easier.
- `SeleniumPageObjectTemplate` now generates reusable page methods that consume generic scenario data instead of product-specific field DTOs.
- `BaseTest` now uses `TestDataProvider` and exposes helper accessors for credentials and scenario datasets.
- `framework.properties` and test-data resources now act as the central source for runtime and UI test data configuration.
- `openai-java` was removed from the active build for now, and `OpenAiTestPlanGenerator` was reduced to an explicit placeholder so the UI/template work is not blocked by a broken local SDK cache.

## Why this helps

- Template selection is no longer tied to one hardcoded constructor path.
- Selenium generation logic is more modular and easier to extend scenario by scenario.
- Test data is centralized and can later be moved from config files to secrets or external data sources without rewriting templates.
- The current UI generation flow is closer to the long-term orchestration goal: scan context -> resolve templates -> generate page objects/tests from reusable libraries.
