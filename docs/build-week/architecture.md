# Build Week Architecture

AQAI uses GPT-5.6 only for constrained semantic reasoning. It does not allow the model to write Selenium or Java method bodies.

```text
Requirements
  -> Canonical scenarios
  -> Browser discovery
  -> Candidate locator, element, and action evidence
  -> Scoring and requirement relevance
  -> Live and postcondition verification
  -> ConfirmedUiCatalog
  -> GPT-5.6 JSON planning OR stable knowledge reuse
  -> Deterministic Page Object and TestNG Java
  -> Compile, review, live smoke, execution, runtime feedback
```

## Evidence Boundary

Only confirmed, browser-verified, same-origin evidence can reach `ConfirmedUiCatalog` and a POM prompt. Raw discovery output, fallback locators, stale retrieved knowledge, and unverified candidates are retained only for diagnostics or review.

## AI And Deterministic Responsibilities

| Concern | Owner |
|---|---|
| Page semantic enrichment | GPT-5.6 or validated reuse |
| POM contract planning | GPT-5.6 or validated reuse |
| Requirement normalization and scenario planning | deterministic platform stages |
| Locator scoring, promotion, and evidence validation | deterministic platform stages |
| Java Page Object and TestNG generation | deterministic writers |
| Compile, review, smoke, execution, and feedback | deterministic validation stages |

This separation allows a warm run to reuse trusted semantic artifacts while retaining the executable validation path.
