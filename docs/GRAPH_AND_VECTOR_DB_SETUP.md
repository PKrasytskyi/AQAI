# Graph and Vector DB Setup

## Goal

This project now has:

- page-mapping artifacts ready for graph/vector persistence
- Qdrant integration already used by the RAG layer
- a Neo4j-ready schema for mapped UI knowledge

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
- future dependency and planner queries

### Qdrant

Use Qdrant for:

- semantic retrieval
- page summaries
- element summaries
- action summaries
- transition summaries
- RAG context search

## 1. Start Both Databases

From the project root:

```powershell
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

## 2. Default Credentials

Current compose defaults:

- Neo4j username: `neo4j`
- Neo4j password: `agentlab123`

Change this before shared/team use.

## 3. Verify Neo4j

Open:

- [Neo4j Browser](http://localhost:7474)

Login with:

- username: `neo4j`
- password: `agentlab123`

Run a quick check:

```cypher
RETURN 1 AS ok;
```

## 4. Apply Graph Schema

### Option A: from inside the container

```powershell
docker exec -it agentlab-neo4j cypher-shell -u neo4j -p agentlab123 -f /var/lib/neo4j/import/page-knowledge-schema.cypher
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

Current project config already points RAG to local Qdrant:

- `src/main/resources/framework.properties`

Current values:

```properties
rag.qdrant.url=http://localhost:6333
rag.qdrant.collection=agentlab-project-style
```

No Neo4j runtime config is consumed by the Java code yet.
The schema and container are prepared now so the next step can be a `GraphPageKnowledgeWriter` that writes `MappedUiKnowledge` into Neo4j.

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

Best next step:

1. add `GraphPageKnowledgeWriter`
2. add `Neo4jRuntimeConfig`
3. write `MappedUiKnowledge.graphNodes/graphEdges` into Neo4j
4. write `MappedUiKnowledge.vectorDocuments` into Qdrant

At that point your `PageMapper` layer becomes persistent knowledge instead of only local JSON artifacts.
