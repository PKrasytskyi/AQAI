# API Layer Documentation

This document describes the current API generation layer for the AgentLab platform.

The API layer is an MVP-quality deterministic pipeline for producing RestAssured/TestNG API automation artifacts from requirement context and endpoint evidence. It is intentionally conservative: it writes runnable source only after typed contracts pass the API quality gate.

## Goal

The API layer turns endpoint evidence into generated Java API automation code:

```text
requirements + endpoint evidence
  -> ApiEndpointBundle
  -> CanonicalApiTestCaseBundle
  -> ApiGenerationSpec
  -> ApiQualityGate
  -> RestAssured/TestNG generated source
  -> compile validation
```

For resources with a complete CRUD surface, it can also generate one atomic full CRUD scenario:

```text
POST collection
  -> GET by created id
  -> PUT by created id
  -> PATCH by created id
  -> DELETE by created id
```

## Runtime Mode

Run API demo mode with:

```powershell
$env:API_AUTH_TOKEN="your-token"
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--api requirements/valid-author.md"
```

`--api-demo` is an alias for `--api`.

The API demo workflow is defined by:

- `ua.demo.agentlab.app.workflow.WorkflowModeResolver`
- `ua.demo.agentlab.app.workflow.ApiDemoWorkflowFactory`
- `ua.demo.agentlab.app.workflow.ApiModuleFactory`

The active API demo agent chain is:

1. `api-generation-agent`
2. `api-generated-source-persistence-agent`
3. `generated-code-compile-agent`

## Configuration

Main API settings live in `src/main/resources/framework.properties`.

Relevant keys:

```properties
project.api.base-url=https://gorest.co.in
api.base-url=https://gorest.co.in
api.auth.token=${API_AUTH_TOKEN}
project.api.endpoint-seed=GET /public/v2/users listUsers; ...
```

Endpoint seed format:

```text
METHOD /path optionalOperationName; METHOD /path/{id} optionalOperationName
```

Example:

```properties
project.api.endpoint-seed=GET /public/v2/users listUsers; POST /public/v2/users createUser; GET /public/v2/users/{id} getUser
```

Secrets must be supplied through environment variables or JVM properties. Do not put real tokens into checked-in property files.

Supported override order for endpoint seeds:

1. JVM property: `-Dproject.api.endpoint-seed="..."`
2. Environment variable: `PROJECT_API_ENDPOINT_SEED`
3. `framework.properties`

The API token is resolved from `${API_AUTH_TOKEN}` through the shared config layer.

## Discovery Inputs

### Manual Endpoint Seed

`PropertiesApiEndpointSeedLoader` reads `project.api.endpoint-seed` and passes the text to `ApiEndpointSeedParser`.

`ApiEndpointSeedParser` creates `ApiEndpointModel` records with:

- HTTP method
- path
- optional operation name
- default expected status code
- manual evidence
- confidence `0.82`

Default statuses:

- `POST` -> `201`
- `DELETE` -> `204`
- everything else -> `200`

### Network Scan Evidence

`NetworkEndpointAdapter` can adapt API-like browser network calls from Selenium discovery into API endpoint evidence.

It currently accepts calls that are same-origin and look API-like by resource type or path:

- XHR/fetch
- path contains `/api/`
- path contains `/public/`
- path ends with `.json`

Network evidence confidence is `0.78`.

### OpenAPI Evidence

`OpenApiEndpointAdapter` converts repository intelligence OpenAPI models into `ApiEndpointModel` records.

OpenAPI evidence confidence is `0.95`.

The adapter exists, but the current `--api` demo path primarily uses endpoint seeds plus optional network evidence. OpenAPI should be wired as a first-class API demo input in the next stabilization pass.

## Core Contracts

### ApiEndpointBundle

Container for confirmed or candidate API endpoints.

Produced by endpoint evidence loaders/adapters and merged by `ApiEndpointBundleMerger`.

### ApiEndpointModel

Typed endpoint evidence:

- `endpointId`
- `method`
- `path`
- `operationName`
- `businessCapability`
- parameters
- request body model
- response models
- auth requirements
- evidence list
- confidence

An endpoint is considered confirmed when:

- confidence is at least `0.70`, or
- at least one evidence record is confirmed.

### CanonicalApiTestCaseBundle

Produced by `RuleBasedCanonicalApiTestCaseGenerator`.

It converts endpoint evidence into canonical API test cases with typed assertion contracts.

### ApiAssertionContract

Typed assertion contract used by generated tests and the quality gate.

The current minimum useful contract includes:

- endpoint id
- expected status code
- response quality assertions
- schema/body/json-path/header assertions where available

The quality gate blocks successful API tests that only assert status code and do not assert body/schema quality.

### ApiGenerationSpec

Generated API source plan:

- `ApiClientSpec`
- `ApiDtoSpec`
- `ApiTestSpec`
- `ApiCrudScenarioSpec`

This is the main writer input.

### ApiCrudScenarioSpec

Represents one controlled full CRUD flow for a resource.

It is generated only when one client has all required methods:

- create: `POST` collection with request body
- read: `GET` by id
- update: `PUT` by id with request body
- patch: `PATCH` by id with request body
- delete: `DELETE` by id

## Generation Services

### ApiGenerationAgent

Main API pipeline agent.

Responsibilities:

- load endpoint seed evidence
- adapt network discovery evidence
- merge endpoint bundles
- generate canonical API test cases
- generate client/DTO/test/CRUD specs
- run API quality gate
- write source previews if quality allows
- publish API generation result artifacts

### ApiTemplateSpecGenerator

Builds Java-oriented API specs from endpoint models.

It groups endpoints by base resource path and generates:

- client classes, for example `UserClient`
- request DTOs, for example `CreateUserRequest`
- update DTOs, for example `UpdateUserRequest`
- patch DTOs, for example `PatchUserRequest`
- response DTOs, for example `UserResponse`
- read-only API test specs
- full CRUD scenario specs when a complete CRUD surface exists

Generated package targets:

```text
ua.demo.agentlab.api.generated.clients
ua.demo.agentlab.api.generated.models.request
ua.demo.agentlab.api.generated.models.update
ua.demo.agentlab.api.generated.models.response
ua.demo.agentlab.api.generated.tests
```

### ApiRestAssuredTestNgWriter

Turns `ApiGenerationSpec` into Java source files.

It writes:

- API clients using `ApiManager`
- DTO classes
- conservative GET tests
- full CRUD tests when `ApiCrudScenarioSpec` is present

Generated source paths:

```text
src/main/java/ua/demo/agentlab/api/generated/...
src/test/java/ua/demo/agentlab/api/generated/...
```

These generated paths are ignored by Git.

### ApiGeneratedSourcePersistenceAgent

Persists generated API source only when:

- `ApiGenerationResult` exists
- quality report has no blockers
- source preview list is not empty

It uses `LocalGeneratedFileWriter`.

## Runtime Support Classes

### ApiManager

Located at `ua.demo.agentlab.core.api.ApiManager`.

It centralizes RestAssured request setup:

- base URI
- JSON content type
- JSON accept header
- authorized request with bearer token
- unauthorized request

Generated API clients should use `ApiManager`, not raw RestAssured setup directly.

### ApiAssertions

Located at `ua.demo.agentlab.core.api.assertions.ApiAssertions`.

Current helpers:

- `assertStatusCode`
- `assertFieldEquals`
- `assertFieldExists`

The assertion helper set is deliberately small. It should grow through typed assertion needs, not ad hoc generated assertions.

## Quality Gate

`ApiQualityGate` validates endpoint evidence, canonical test cases, generation specs, CRUD specs, and generated source previews.

Blocking examples:

- no endpoint evidence
- no canonical API test cases
- test case references missing endpoint
- endpoint is not confirmed
- test case method/path differs from endpoint model
- assertion contract is missing
- success test has no body/schema assertion
- client method references missing request DTO
- generated isolated test uses path params without scenario data
- generated isolated mutation test has no data/auth policy
- CRUD scenario is incomplete
- CRUD client method has wrong HTTP verb
- generated API tests are written outside `src/test/java`
- generated clients/DTOs are written outside `src/main/java`
- generated source is blank or has invalid imports

Important distinction:

- isolated generated tests are conservative and currently limited to safe GET collection scenarios;
- mutation/path-param tests are allowed only inside the controlled full CRUD scenario, because that flow creates its own entity and owns cleanup.

## Current GoRest Demo Shape

The checked-in `framework.properties` currently points API settings to GoRest:

```properties
project.api.base-url=https://gorest.co.in
api.base-url=https://gorest.co.in
```

The endpoint seed includes:

- users
- posts
- comments
- todos
- nested user posts
- nested post comments
- nested user todos

The full CRUD demo is available for the users resource because the seed contains:

```text
GET /public/v2/users
GET /public/v2/users/{id}
POST /public/v2/users
PUT /public/v2/users/{id}
PATCH /public/v2/users/{id}
DELETE /public/v2/users/{id}
```

Expected generated CRUD test:

```text
UserCrudApiTest.shouldCreateReadUpdatePatchAndDeleteUser
```

## Generated Artifact Policy

Generated API source is intentionally ignored by Git:

```gitignore
src/main/java/ua/demo/agentlab/api/generated/
src/test/java/ua/demo/agentlab/api/generated/
```

This keeps the repository clean while still allowing local demo runs to materialize compile-checkable code.

The release/demo note is tracked:

```text
docs/RELEASE_DEMO_NOTES.md
```

## Verification Commands

Unit suite:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test
```

Compile generated API tests without running live API calls:

```powershell
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" test-compile
```

API demo generation:

```powershell
$env:API_AUTH_TOKEN="your-token"
mvn --batch-mode "-Duser.home=." "-Dmaven.repo.local=.m2repo" exec:java "-Dexec.args=--api requirements/valid-author.md"
```

## Current Limitations

The API layer is usable for a demo run, but not production-complete yet.

Known limitations:

- OpenAPI evidence adapter exists, but API demo mode does not yet treat OpenAPI as a first-class input source.
- DTO fields are still generated from a rule-based catalog, not from real OpenAPI schemas or response samples.
- Isolated mutation tests are intentionally blocked until data factory and cleanup policies are stronger.
- Auth support is bearer-token oriented.
- Negative tests and error-contract tests are not generated yet.
- JSON schema assertions are represented in contracts but not yet deeply generated from OpenAPI schemas.
- Request body generation is deterministic sample data, not requirement-aware scenario data.
- Generated tests are excluded from normal Surefire execution to avoid accidental live API calls during unit test runs.

## Recommended Next Steps

1. Wire OpenAPI input into the `--api` workflow as a first-class source.
2. Add an API evidence artifact writer for endpoint bundle, canonical cases, generation spec, and quality report.
3. Add JSON Schema contracts for API LLM outputs if AI-assisted API enrichment is introduced later.
4. Expand `ApiDtoFieldCatalog` with schema/sample-driven field inference.
5. Introduce `ApiScenarioDataFactory` for safe unique test data.
6. Add cleanup policy for resources that do not support DELETE or return non-standard delete statuses.
7. Add negative test generation for `401`, `403`, `404`, and validation errors.
8. Add API quality summary similar to UI run-level quality summary.

