# Runtime Skills

Runtime skills are versioned prompt/schema contracts used by the platform when an LLM task is required.
They are intentionally separated from Java orchestration code so prompt rules, examples, and JSON schemas can be reviewed and evolved without changing mapper or writer classes.

Current packages:

- `pom-json-generation` - creates `pom-contract-v1` JSON for the deterministic POM Java writer.
- `page-enrichment` - enriches page-owned mapper evidence with concise semantic metadata.
- `test-json-generation` - creates structured UI test spec JSON, not Java code.
- `self-healing-locator` - reviews replacement locator candidates after runtime failures.
- `error-analysis` - classifies run/test failures from runtime evidence.
- `bug-report-generation` - drafts bug reports from validated failure evidence.

Each skill contains:

- `skill.yaml`
- `prompt.md`
- `input-schema.json`
- `output-schema.json`
- `rules.md`
- `examples/valid-input.json`
- `examples/valid-output.json`
- `examples/bad-output.json`

These files are the target source of truth for future prompt builders. Current Java prompt builders still keep compatibility logic while the runtime skill loader is introduced.

