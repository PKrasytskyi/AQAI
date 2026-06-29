package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.ChunkMetadata;
import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient;
import ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore;
import ua.demo.agentlab.ai.rag.store.VectorStore;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.KnowledgeVectorRuntimeConfig;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QdrantPageKnowledgeWriter implements PageKnowledgeWriter {

    private final KnowledgeVectorRuntimeConfig config;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final Path workspaceRoot;

    public QdrantPageKnowledgeWriter(KnowledgeVectorRuntimeConfig config) {
        this(config, Path.of("").toAbsolutePath().normalize());
    }

    public QdrantPageKnowledgeWriter(KnowledgeVectorRuntimeConfig config, Path workspaceRoot) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        this.config = config;
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        RagRuntimeConfig ragRuntimeConfig = new KnowledgeVectorRagRuntimeConfigAdapter(config);
        this.embeddingService = new OpenAiEmbeddingClient(ragRuntimeConfig);
        this.vectorStore = new QdrantVectorStore(ragRuntimeConfig);
    }

    @Override
    public PageKnowledgeWriteResult write(MappedUiKnowledge knowledge) {
        if (!config.enabled()) {
            return new PageKnowledgeWriteResult("qdrant", false, 0, 0, 0, "Qdrant persistence is disabled");
        }
        if (knowledge == null) {
            throw new IllegalArgumentException("knowledge cannot be null");
        }
        if (knowledge.vectorDocuments().isEmpty()) {
            return new PageKnowledgeWriteResult("qdrant", true, 0, 0, 0, "No vector documents to persist");
        }

        List<CodeChunk> chunks = new ArrayList<>();
        List<List<Double>> vectors = new ArrayList<>();
        for (int index = 0; index < knowledge.vectorDocuments().size(); index++) {
            PageKnowledgeVectorDocument document = knowledge.vectorDocuments().get(index);
            List<Double> vector = embeddingService.embed(document.text());
            chunks.add(toChunk(document, index));
            vectors.add(vector);
        }

        vectorStore.ensureCollection(vectors.get(0).size());
        vectorStore.upsert(chunks, vectors);

        return new PageKnowledgeWriteResult(
                "qdrant",
                true,
                0,
                0,
                knowledge.vectorDocuments().size(),
                "Mapped UI knowledge vector documents persisted to Qdrant"
        );
    }

    private CodeChunk toChunk(PageKnowledgeVectorDocument document, int index) {
        String safeFileName = toSafeFileName(document.documentId());
        return new CodeChunk(
                toPointId(document),
                resolveSyntheticPath(document),
                "target/ui-discovery/vector-documents/" + safeFileName + ".md",
                "markdown",
                index,
                0,
                document.text().length(),
                document.text(),
                new ChunkMetadata(
                        ArtifactType.DOCUMENTATION,
                        document.documentType(),
                        "ua.demo.agentlab.ui.discovery.mapping",
                        buildTags(document),
                        document.metadata()
                )
        );
    }

    private Path resolveSyntheticPath(PageKnowledgeVectorDocument document) {
        String safeFileName = toSafeFileName(document.documentId());
        return workspaceRoot
                .resolve("target")
                .resolve("ui-discovery")
                .resolve("vector-documents")
                .resolve(safeFileName + ".md")
                .toAbsolutePath()
                .normalize();
    }

    private String toSafeFileName(String value) {
        String normalized = value == null ? "" : value.trim();
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("(^[.\\-]+|[.\\-]+$)", "");
        return normalized.isBlank() ? "document" : normalized;
    }

    private String toPointId(PageKnowledgeVectorDocument document) {
        String runId = document.metadata().getOrDefault("runId", "");
        String normalized = (runId + "::" + document.documentId()).trim();
        if (normalized.isBlank()) {
            normalized = "document";
        }
        return UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<String> buildTags(PageKnowledgeVectorDocument document) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add("ui-knowledge");
        tags.add(document.documentType());
        if (!document.sourcePageId().isBlank()) {
            tags.add(document.sourcePageId());
        }
        if (!document.sourceEntityId().isBlank()) {
            tags.add(document.sourceEntityId());
        }
        tags.addAll(document.keywords());
        return List.copyOf(tags);
    }

    private static final class KnowledgeVectorRagRuntimeConfigAdapter implements RagRuntimeConfig {

        private final KnowledgeVectorRuntimeConfig config;

        private KnowledgeVectorRagRuntimeConfigAdapter(KnowledgeVectorRuntimeConfig config) {
            this.config = config;
        }

        @Override
        public boolean enabled() {
            return config.enabled();
        }

        @Override
        public String qdrantUrl() {
            return config.qdrantUrl();
        }

        @Override
        public String qdrantApiKey() {
            return config.qdrantApiKey();
        }

        @Override
        public String collectionName() {
            return config.collectionName();
        }

        @Override
        public Path chunksJsonlPath() {
            return Path.of("target/ui-discovery/vector-documents/chunks.jsonl").toAbsolutePath().normalize();
        }

        @Override
        public int chunkMaxChars() {
            return 1800;
        }

        @Override
        public int chunkOverlapChars() {
            return 0;
        }

        @Override
        public int retrievalLimit() {
            return 8;
        }

        @Override
        public String embeddingModel() {
            return config.embeddingModel();
        }

        @Override
        public String generationModel() {
            return "gpt-5-mini";
        }

        @Override
        public String openAiApiKey() {
            return config.openAiApiKey();
        }

        @Override
        public String openAiBaseUrl() {
            return config.openAiBaseUrl();
        }

        @Override
        public int maxOutputTokens() {
            return 1800;
        }
    }
}
