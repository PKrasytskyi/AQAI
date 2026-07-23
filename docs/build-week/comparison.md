# Cold Discovery vs Knowledge Reuse

Both captured runs execute the same `orangehrm-login-user-menu-logout-v1` fixture, produce two Page Objects and four TestNG tests, and finish with compile, review, live smoke, and generated test execution passing.

| Metric | Without DB: cold discovery | With knowledge layer: reuse |
|---|---:|---:|
| GPT-5.6 runtime calls | 4 | 0 |
| Page-enrichment calls | 2 | 0 |
| Page-enrichment cache hits | 0 | 2 |
| POM planning calls | 2 | 0 |
| Stable POM contracts reused | 0 | 2 |
| Neo4j | miss | hit |
| Qdrant | miss | hit |
| Confirmed catalog pages | 2 | 2 |
| Generated Page Objects | 2 | 2 |
| Generated TestNG tests | 4 | 4 |
| Tests | 4/4 passed | 4/4 passed |
| Compile / live smoke | passed / passed | passed / passed |

**Key result:** same requirements, same validation path, and the same 4/4 passing generated tests, with four fewer runtime model calls after verified knowledge is available.

The comparison is deliberately not a claim that the warm run avoids browser verification. AQAI avoids repeat semantic-model calls by reusing validated knowledge; deterministic generation and validation remain part of the run.

Source summaries: [cold discovery](without-db-summary.md) and [knowledge reuse](with-db-summary.md).
