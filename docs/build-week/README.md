# Build Week Evidence

This directory preserves the strongest verified Build Week demo results outside the ignored `target/` directory. It is a curated record of two successful runs of the same versioned OrangeHRM authentication, user-menu, and logout fixture.

| Run | Mode | Runtime model calls | Result |
|---|---|---:|---|
| [Cold discovery](without-db-summary.md) | `COLD-DISCOVERY` without DB | 4 | 4/4 generated tests passed |
| [Knowledge reuse](with-db-summary.md) | `KNOWLEDGE-REUSE` with DB | 0 | 4/4 generated tests passed |

The [comparison](comparison.md) shows the measurable reuse result. The [architecture note](architecture.md) explains why the platform does not allow unverified evidence to enter a POM prompt or generated Java.

## Evidence Scope

- Demo: `orangehrm-login-user-menu-logout-v1`
- Runtime model: `gpt-5.6-luna`
- Scenario count: 4
- Generated Page Objects: 2
- Generated TestNG tests: 4

These are captured run summaries, not mocked benchmark values. They intentionally exclude credentials, raw DOM snapshots, and transient browser artifacts.

## Reproduce

Use the versioned bundle in [demo/orangehrm-login-logout](../../demo/orangehrm-login-logout). Run once with `KNOWLEDGE_DB_STATUS=false` for the cold baseline. For the reuse measurement, start Neo4j and Qdrant, run once to seed validated knowledge, then run the same demo again with `KNOWLEDGE_DB_STATUS=true`.

The live generated summary is written to `target/ai-run/quality/build-week-demo-summary.md`.
