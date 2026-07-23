# With Knowledge Layer: Reuse

Captured successful run: `b713d042-c3ee-41b6-b0c0-3c3065687936`

| Field | Result |
|---|---|
| Demo | `orangehrm-login-user-menu-logout-v1` |
| Run mode | `KNOWLEDGE-REUSE` |
| Runtime model | `gpt-5.6-luna` |
| AI reasoning status | reused validated knowledge |
| Page-enrichment calls / cache hits | 0 / 2 |
| POM planning calls / stable POM reuse | 0 / 2 |
| Avoided runtime model calls | 4 |
| Neo4j / Qdrant | hit / hit |
| Confirmed catalog | 2 pages, verified locator evidence |
| POM contracts / Page Objects | 2 / 2 |
| Generated TestNG tests | 4 |
| Compile / review / live smoke | passed / 0 findings / passed |
| Executed tests | 4 passed, 0 failed |
| Evidence maturity score | 79/100 |
| Coverage gaps / blockers | 2 / 0 |

The reuse run does not skip validation. It reuses only previously validated enrichment and POM contracts, then deterministically generates and validates the same executable output path.
