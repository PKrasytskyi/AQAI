package ua.demo.agentlab.ai.rag.service;

import ua.demo.agentlab.ai.rag.chunking.ProjectDocumentChunker;
import ua.demo.agentlab.ai.rag.command.IndexProjectCommand;
import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.index.ChunkJsonlWriter;
import ua.demo.agentlab.ai.rag.index.CodebaseIndexer;
import ua.demo.agentlab.ai.rag.model.RagIndexingReport;
import ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector;
import ua.demo.agentlab.ai.rag.store.VectorStore;

import java.nio.file.Path;

public class RagIndexingService {

    private final IndexProjectCommand indexProjectCommand;

    public RagIndexingService(
            WorkspaceDocumentCollector collector,
            ProjectDocumentChunker chunker,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            String collectionName,
            Path chunksJsonlPath
    ) {
        if (collector == null || chunker == null || embeddingService == null || vectorStore == null) {
            throw new IllegalArgumentException("RAG indexing dependencies cannot be null");
        }
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("collectionName cannot be blank");
        }
        this.indexProjectCommand = new IndexProjectCommand(
                new CodebaseIndexer(collector, chunker),
                new ChunkJsonlWriter(),
                embeddingService,
                vectorStore,
                collectionName,
                chunksJsonlPath
        );
    }

    public RagIndexingReport indexWorkspace(Path workspaceRoot) {
        return indexProjectCommand.execute(workspaceRoot);
    }
}
