# RAG Intent Retrieval

## Purpose

The RAG layer should answer:

`What context is actually needed for this task?`

It should not return only generic semantic matches.
It should assemble a minimal, useful context pack for generation.

## Example

Request:

`Generate negative login tests`

Intent-aware retrieval should prefer artifacts like:

- `LoginPage.java`
- `LoginTest.java`
- `BaseTest.java`
- `TestDataFactory.java`
- `LocatorPolicy.md`

## Implemented Contracts

- `ArtifactType`
- `ChunkMetadata`
- `IndexedArtifact`
- `QueryIntent`
- `QueryIntentResolver`
- `ContextRetrievalRequest`
- `ContextRetrievalResult`
- `ContextAssembler`
- `ProjectContextRetriever`

## How It Works

1. `CodebaseIndexer` classifies each indexed file into an artifact type
2. Metadata is attached to every `CodeChunk`
3. Metadata is persisted to Qdrant payload
4. `QueryIntentResolver` extracts:
   - domain terms
   - qualifiers like `negative`, `smoke`, `regression`
   - preferred artifact types
5. `ProjectContextRetriever` performs retrieval with an intent-enriched query
6. `ContextAssembler` deduplicates by file and ranks results by:
   - semantic similarity
   - artifact type relevance
   - domain-term overlap
   - qualifier overlap

## Why This Matters

Without this layer, RAG tends to return noisy chunks.

With this layer, generation receives a cleaner context pack and is more likely to:

- reuse the right Page Object
- align with existing tests
- reuse base classes and test data
- respect policy and locator rules
- spend fewer tokens on irrelevant code
