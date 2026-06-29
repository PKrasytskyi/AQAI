package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.intelligence.model.VectorSummary;
import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.ChunkMetadata;
import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.store.VectorStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QdrantVectorSummaryIndexer implements VectorSummaryIndexer {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public QdrantVectorSummaryIndexer(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @Override
    public int index(Path workspaceRoot, List<VectorSummary> vectorSummaries) {
        if (vectorSummaries == null || vectorSummaries.isEmpty()) {
            return 0;
        }
        List<CodeChunk> chunks = new ArrayList<>();
        List<List<Double>> vectors = new ArrayList<>();
        for (VectorSummary summary : vectorSummaries) {
            List<Double> vector = embeddingService.embed(summary.summaryText());
            chunks.add(new CodeChunk(
                    "summary-" + summary.id(),
                    resolveAbsolutePath(workspaceRoot, summary.sourcePath()),
                    summary.sourcePath() + "#summary:" + summary.id(),
                    "markdown",
                    0,
                    0,
                    summary.summaryText().length(),
                    summary.summaryText(),
                    new ChunkMetadata(
                            ArtifactType.DOCUMENTATION,
                            summary.summaryType(),
                            "",
                            buildTags(summary)
                    )
            ));
            vectors.add(vector);
        }
        vectorStore.ensureCollection(vectors.get(0).size());
        vectorStore.upsert(chunks, vectors);
        return chunks.size();
    }

    private Path resolveAbsolutePath(Path workspaceRoot, String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return workspaceRoot.resolve("target/repository-intelligence/vector-summaries.md").toAbsolutePath().normalize();
        }
        return workspaceRoot.resolve(sourcePath).toAbsolutePath().normalize();
    }

    private List<String> buildTags(VectorSummary summary) {
        List<String> tags = new ArrayList<>(summary.keywords());
        tags.add(summary.summaryType().toLowerCase(Locale.ROOT));
        return tags.stream().distinct().toList();
    }
}
