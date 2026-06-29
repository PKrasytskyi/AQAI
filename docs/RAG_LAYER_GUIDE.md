# RAG Layer Guide

## Purpose

The RAG layer is responsible for:

1. Indexing project source files in Java, Markdown, and Gherkin feature format
2. Splitting files into reusable retrieval chunks
3. Writing `chunks.jsonl` as an inspectable index artifact
4. Storing vectors and payloads in Qdrant
5. Retrieving relevant chunks for a user request
6. Building a project-aware prompt and generating a test in project style

## Implemented Components

- `ua.demo.agentlab.ai.rag.model.CodeChunk`
  Canonical chunk model for indexed code and documentation.

- `ua.demo.agentlab.ai.rag.index.CodebaseIndexer`
  Collects Java, Markdown, and Gherkin files and turns them into `CodeChunk` objects.

- `ua.demo.agentlab.ai.rag.index.ChunkJsonlWriter`
  Persists indexed chunks to `chunks.jsonl` for inspection and debugging.

- `docker-compose.qdrant.yml`
  Local Qdrant runtime for vector storage.

- `ua.demo.agentlab.ai.rag.embedding.EmbeddingService`
  Embedding abstraction for chunk and query vectorization.

- `ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector`
  Scans the workspace and collects `.java`, `.md`, and `.feature` files.

- `ua.demo.agentlab.ai.rag.chunking.ProjectDocumentChunker`
  Splits files into overlapping chunks that are suitable for embeddings and retrieval.

- `ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient`
  Calls OpenAI Embeddings API to produce vectors for code and documentation chunks.

- `ua.demo.agentlab.ai.rag.store.VectorStore`
  Vector storage contract for search and persistence.

- `ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore`
  Creates the Qdrant collection, upserts chunk vectors, and performs similarity search.

- `ua.demo.agentlab.ai.rag.command.IndexProjectCommand`
  End-to-end indexing pipeline: index -> write `chunks.jsonl` -> embed -> persist to Qdrant.

- `ua.demo.agentlab.ai.rag.retrieval.Retriever`
  Embeds a user request and retrieves relevant chunks from Qdrant.

- `ua.demo.agentlab.ai.rag.prompt.PromptBuilder`
  Prompt building contract for project-aware code generation.

- `ua.demo.agentlab.ai.rag.prompt.ProjectStylePromptBuilder`
  Builds a prompt using project context plus retrieved code/style examples.

- `ua.demo.agentlab.ai.rag.agent.GenerateTestAgent`
  Retrieval + prompt building + OpenAI generation.

- `ua.demo.agentlab.app.RagConsoleRunner`
  Console entry point for indexing, search, and generation.

## Configuration

RAG is configured in [framework.properties](/C:/Users/demra/IdeaProjects/agent_base_testing_framework/src/main/resources/framework.properties).

Key properties:

- `rag.enabled`
- `rag.qdrant.url`
- `rag.qdrant.collection`
- `rag.index.chunks-jsonl`
- `rag.chunk.max-chars`
- `rag.chunk.overlap-chars`
- `rag.retrieval.limit`
- `rag.openai.embedding-model`
- `rag.openai.generation-model`
- `rag.openai.base-url`
- `rag.openai.max-output-tokens`

Secrets should come from environment variables:

- `OPENAI_API_KEY` or `RAG_OPENAI_API_KEY`
- `RAG_QDRANT_API_KEY` when Qdrant is protected

## Run Flow

### 1. Index the project

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="index ."
```

This will also write `chunks.jsonl` to the configured location.

### 2. Search relevant chunks

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="search . \"Generate a TestNG UI test for collections page navigation\""
```

### 3. Generate test code in project style

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="generate . \"Generate a TestNG UI test for collections page navigation\""
```

### 4. One-shot index and generate

```powershell
mvn -Dexec.mainClass=ua.demo.agentlab.app.RagConsoleRunner exec:java -Dexec.args="index-and-generate . \"Generate a TestNG UI test for collections page navigation\""
```

## Current Boundaries

- The RAG layer generates code text, but does not yet persist generated tests into the project automatically.
- The generation step is style-aware, because it uses scanned project context and retrieved source chunks.
- Qdrant must be started separately before indexing or retrieval.
- OpenAI credentials must be configured before embeddings or generation can run.

## External API References

- [OpenAI Embeddings API](https://developers.openai.com/api/reference/resources/embeddings)
- [OpenAI Responses API](https://platform.openai.com/docs/api-reference/responses)
- [Qdrant Create Collection](https://api.qdrant.tech/api-reference/collections/create-collection)
- [Qdrant Upsert Points](https://api.qdrant.tech/api-reference/points/upsert-points)
- [Qdrant Search Points](https://api.qdrant.tech/api-reference/search/points)
