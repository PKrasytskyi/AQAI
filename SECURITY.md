# Security Policy

## Supported Scope

This project is an experimental AI-assisted QA automation platform. Security reports are welcome for issues in repository code, configuration handling, generated-source safety, prompt artifact handling, dependency usage, and local service setup.

## Reporting a Vulnerability

Please do not open a public issue for sensitive vulnerabilities.

Report security concerns privately through GitHub's private vulnerability reporting feature if it is enabled for the repository. If private reporting is not available, contact the repository owner through a private channel and include:

- affected component or file;
- reproduction steps;
- expected impact;
- whether secrets, generated artifacts, or local services are involved;
- suggested mitigation, if known.

## Secret Handling

Never commit real credentials. The repository should use placeholders or environment variables for:

```text
OPENAI_API_KEY
RAG_OPENAI_API_KEY
KNOWLEDGE_GRAPH_NEO4J_PASSWORD
API_AUTH_TOKEN
TEST_VALID_USERNAME
TEST_VALID_PASSWORD
```

Local override files such as `.env`, `framework-local.properties`, and `test-data-local.properties` are ignored by Git and should remain local.

## Generated Artifacts

Runtime artifacts can contain discovered URLs, page text, prompt context, screenshots, or local execution traces. They belong under `target/` and must not be committed unless they are intentionally sanitized fixtures.

## Dependency and Service Notes

- Neo4j and Qdrant are optional local services.
- AI/RAG/database integrations are disabled by default in committed configuration.
- Generated Java code should pass quality gates before being moved into source folders.

## Response Expectations

Security reports will be reviewed as project capacity allows. Accepted fixes should include tests or documentation where practical.
