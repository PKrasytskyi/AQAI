# Without DB: Cold Discovery

Captured successful run: `1203fb18-18e4-426f-abe0-23b6aa0b288d`

| Field | Result |
|---|---|
| Demo | `orangehrm-login-user-menu-logout-v1` |
| Run mode | `COLD-DISCOVERY` |
| Knowledge layer | disabled |
| Runtime model | `gpt-5.6-luna` |
| AI reasoning status | executed |
| Page-enrichment calls | 2 |
| POM planning calls | 2 |
| Neo4j / Qdrant | miss / miss |
| Confirmed catalog | 2 pages, verified locator evidence |
| POM contracts / Page Objects | 2 / 2 |
| Generated TestNG tests | 4 |
| Compile / review / live smoke | passed / 0 findings / passed |
| Executed tests | 4 passed, 0 failed |
| Evidence maturity score | 79/100 |
| Coverage gaps / blockers | 2 / 0 |

This run establishes the evidence through browser discovery, live verification, constrained GPT-5.6 semantic planning, deterministic Java generation, and validation.

The score is intentionally conservative: two assertions remain explicit coverage gaps rather than AI-invented UI checks. They do not block compilation, live smoke, or the four generated test executions.
