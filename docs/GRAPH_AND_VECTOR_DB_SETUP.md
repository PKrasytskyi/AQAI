# Graph and Vector DB Setup

## Goal

This project now has:

- page-mapping artifacts ready for graph/vector persistence;
- Qdrant integration for UI knowledge vector documents;
- Neo4j integration for graph page knowledge and stable page-enrichment cache lookup;
- namespace metadata for run-aware retrieval and stable page cache reuse.

This document explains how to start both databases locally with Docker and how to initialize the Neo4j schema.

## Files Added

- `docker-compose.knowledge.yml`
- `infra/neo4j/page-knowledge-schema.cypher`

The existing `docker-compose.qdrant.yml` remains valid for Qdrant-only runs.

## What Each DB Is For

### Neo4j

Use Neo4j for:

- page structure
- element/action/locator relationships
- transitions
- assertion hint relationships
- page-enrichment cache records keyed by app/base URL/page fingerprint
- dependency and planner queries

### Qdrant

Use Qdrant for:

- semantic retrieval
- page summaries
- element summaries
- action summaries
- transition summaries
- page-enrichment summaries
- RAG context search

## 1. Start Both Databases

From the project root:

```powershell
$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD="local-neo4j-password"
docker compose -f docker-compose.knowledge.yml up -d
```

Check status:

```powershell
docker compose -f docker-compose.knowledge.yml ps
```

Expected ports:

- Neo4j Browser: `http://localhost:7474`
- Neo4j Bolt: `bolt://localhost:7687`
- Qdrant HTTP: `http://localhost:6333`
- Qdrant gRPC: `localhost:6334`

## 2. Local Credentials

Current compose uses the `KNOWLEDGE_GRAPH_NEO4J_PASSWORD` environment variable. If it is not set, Docker Compose falls back to the local demo password `local-neo4j-password`.

- Neo4j username: `neo4j`
- Neo4j password: `$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD`

Set a real value before shared/team use. Do not commit private passwords to `framework.properties` or documentation.

## 3. Verify Neo4j

Open:

- [Neo4j Browser](http://localhost:7474)

Login with:

- username: `neo4j`
- password: value of `$env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD`

Run a quick check:

```cypher
RETURN 1 AS ok;
```

## 4. Apply Graph Schema

### Option A: from inside the container

```powershell
docker exec -it agentlab-neo4j cypher-shell -u neo4j -p $env:KNOWLEDGE_GRAPH_NEO4J_PASSWORD -f /var/lib/neo4j/import/page-knowledge-schema.cypher
```

### Option B: paste manually in Neo4j Browser

Open the file:

- `infra/neo4j/page-knowledge-schema.cypher`

Paste and run its contents in the browser.

## 5. Verify Schema

Run:

```cypher
SHOW CONSTRAINTS;
SHOW INDEXES;
```

You should see constraints/indexes for:

- `Page`
- `Section`
- `Element`
- `Form`
- `Field`
- `Action`
- `Locator`
- `AssertionHint`
- `Transition`

## 6. Verify Qdrant

Open:

- [Qdrant Collections API](http://localhost:6333/collections)

If Qdrant is up, it returns JSON.

## 7. Project Configuration

Current project config points both repository RAG and UI knowledge retrieval to local services:

- `src/main/resources/framework.properties`

Current values:

```properties
rag.qdrant.url=http://localhost:6333
rag.qdrant.collection=agentlab-project-style
knowledge.graph.enabled=true
knowledge.graph.neo4j.url=http://localhost:7474
knowledge.vector.enabled=true
knowledge.vector.qdrant.url=http://localhost:6333
knowledge.vector.qdrant.collection=agentlab-ui-knowledge
```

Neo4j requires `KNOWLEDGE_GRAPH_NEO4J_PASSWORD` in the same shell that runs the workflow. Qdrant does not require an API key for the default local Docker setup.

## 8. Useful Docker Commands

Stop both DBs:

```powershell
docker compose -f docker-compose.knowledge.yml down
```

Stop and remove volumes:

```powershell
docker compose -f docker-compose.knowledge.yml down -v
```

View logs:

```powershell
docker compose -f docker-compose.knowledge.yml logs neo4j
docker compose -f docker-compose.knowledge.yml logs qdrant
```

Restart one service:

```powershell
docker compose -f docker-compose.knowledge.yml restart neo4j
docker compose -f docker-compose.knowledge.yml restart qdrant
```

## 9. Recommended First Neo4j Smoke Test

After applying schema, manually create one page and one element:

```cypher
MERGE (p:Page {id: 'page:collections'})
SET p.name = 'CollectionsPage',
    p.pageType = 'listing',
    p.urlPattern = '/collections';

MERGE (e:Element {id: 'page:collections:element:product-card'})
SET e.semanticName = 'productCard',
    e.elementType = 'link',
    e.pageId = 'page:collections';

MERGE (p)-[:PAGE_HAS_ELEMENT]->(e);
```

Query it:

```cypher
MATCH (p:Page)-[:PAGE_HAS_ELEMENT]->(e:Element)
RETURN p.name, e.semanticName;
```

## 10. What To Do Next

Best next steps:

1. strengthen DashboardPage fingerprint stability so authenticated pages can reuse stable page cache records;
2. add a DB health artifact that records Neo4j/Qdrant writes, reads, cache hits, and misses per run;
3. penalize run quality when expected DB cache hits are missing or when only vector docs are written without graph confirmation;
4. add cleanup/versioning commands for local development data.
