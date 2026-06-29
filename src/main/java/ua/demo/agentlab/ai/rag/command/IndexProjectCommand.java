package ua.demo.agentlab.ai.rag.command;

import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.index.ChunkJsonlWriter;
import ua.demo.agentlab.ai.rag.index.CodebaseIndexer;
import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.model.RagIndexingReport;
import ua.demo.agentlab.ai.rag.store.VectorStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IndexProjectCommand {

    private final CodebaseIndexer codebaseIndexer;
    private final ChunkJsonlWriter chunkJsonlWriter;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final String collectionName;
    private final Path chunksJsonlPath;

    public IndexProjectCommand(
            CodebaseIndexer codebaseIndexer,
            ChunkJsonlWriter chunkJsonlWriter,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            String collectionName,
            Path chunksJsonlPath
    ) {
        if (codebaseIndexer == null || chunkJsonlWriter == null || embeddingService == null || vectorStore == null) {
            throw new IllegalArgumentException("IndexProjectCommand dependencies cannot be null");
        }
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("collectionName cannot be blank");
        }
        if (chunksJsonlPath == null) {
            throw new IllegalArgumentException("chunksJsonlPath cannot be null");
        }
        this.codebaseIndexer = codebaseIndexer;
        this.chunkJsonlWriter = chunkJsonlWriter;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.collectionName = collectionName;
        this.chunksJsonlPath = chunksJsonlPath;
    }

    public RagIndexingReport execute(Path workspaceRoot) {
        List<CodeChunk> chunks = codebaseIndexer.index(workspaceRoot);
        chunkJsonlWriter.write(chunksJsonlPath, chunks);

        if (chunks.isEmpty()) {
            return new RagIndexingReport(0, 0, 0, collectionName, chunksJsonlPath.toAbsolutePath().normalize());
        }

        List<List<Double>> vectors = new ArrayList<>(chunks.size());
        for (CodeChunk chunk : chunks) {
            vectors.add(embeddingService.embed(chunk.text()));
        }

        int vectorSize = vectors.get(0).size();
        vectorStore.ensureCollection(vectorSize);
        vectorStore.upsert(chunks, vectors);

        return new RagIndexingReport(
                countDistinctDocuments(chunks),
                chunks.size(),
                vectorSize,
                collectionName,
                chunksJsonlPath.toAbsolutePath().normalize()
        );
    }

    private int countDistinctDocuments(List<CodeChunk> chunks) {
        return (int) chunks.stream().map(CodeChunk::relativePath).distinct().count();
    }
}
