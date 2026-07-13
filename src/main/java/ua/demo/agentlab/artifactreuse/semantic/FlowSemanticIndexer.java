package ua.demo.agentlab.artifactreuse.semantic;

import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.ChunkMetadata;
import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient;
import ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore;
import ua.demo.agentlab.ai.rag.store.VectorStore;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.KnowledgeVectorRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persists only confirmed flow summaries. Raw flow evidence never enters prompt context from this layer. */
public class FlowSemanticIndexer {

    private final ArtifactReuseRuntimeConfig reuseConfig;
    private final KnowledgeVectorRuntimeConfig vectorConfig;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final FlowSemanticDocumentBuilder documentBuilder;

    public FlowSemanticIndexer(ArtifactReuseRuntimeConfig reuseConfig, KnowledgeVectorRuntimeConfig vectorConfig) {
        this(reuseConfig, vectorConfig, new OpenAiEmbeddingClient(new VectorConfigAdapter(vectorConfig)),
                new QdrantVectorStore(new VectorConfigAdapter(vectorConfig)), new FlowSemanticDocumentBuilder());
    }

    FlowSemanticIndexer(
            ArtifactReuseRuntimeConfig reuseConfig,
            KnowledgeVectorRuntimeConfig vectorConfig,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            FlowSemanticDocumentBuilder documentBuilder
    ) {
        if (reuseConfig == null || vectorConfig == null || embeddingService == null || vectorStore == null || documentBuilder == null) {
            throw new IllegalArgumentException("semantic indexer dependencies cannot be null");
        }
        this.reuseConfig = reuseConfig;
        this.vectorConfig = vectorConfig;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.documentBuilder = documentBuilder;
    }

    public FlowSemanticIndexResult index(FlowContractBundle bundle) {
        if (!reuseConfig.enabled() || !reuseConfig.semanticReuseEnabled()) {
            return FlowSemanticIndexResult.skipped("semantic reuse is disabled");
        }
        if (!vectorConfig.enabled()) {
            return FlowSemanticIndexResult.skipped("Qdrant vector knowledge is disabled");
        }
        if (vectorConfig.openAiApiKey() == null || vectorConfig.openAiApiKey().isBlank()) {
            return FlowSemanticIndexResult.skipped("embedding API key is not configured");
        }
        if (bundle == null || bundle.runMetadata() == null) {
            return FlowSemanticIndexResult.skipped("flow bundle namespace metadata is missing");
        }
        List<FlowContract> confirmed = bundle.contracts().stream().filter(FlowContract::confirmed).toList();
        if (confirmed.isEmpty()) {
            return new FlowSemanticIndexResult(true, true, 0, "qdrant", "no confirmed flow contracts to index");
        }
        try {
            List<CodeChunk> chunks = new ArrayList<>();
            List<List<Double>> vectors = new ArrayList<>();
            for (int index = 0; index < confirmed.size(); index++) {
                FlowContract flow = confirmed.get(index);
                String text = documentBuilder.build(flow);
                chunks.add(chunk(flow, text, index, bundle));
                vectors.add(embeddingService.embed(text));
            }
            vectorStore.ensureCollection(vectors.get(0).size());
            vectorStore.upsert(chunks, vectors);
            return new FlowSemanticIndexResult(true, true, confirmed.size(), "qdrant", "confirmed flow summaries indexed");
        } catch (Exception exception) {
            return new FlowSemanticIndexResult(true, false, 0, "qdrant", safe(exception.getMessage()));
        }
    }

    private CodeChunk chunk(FlowContract flow, String text, int index, FlowContractBundle bundle) {
        var metadata = bundle.runMetadata();
        Map<String, String> attributes = new LinkedHashMap<>(metadata.asMap());
        attributes.put("knowledgeType", "flow-contract");
        attributes.put("flowId", flow.flowId());
        attributes.put("flowType", flow.type().name());
        attributes.put("flowSourceRoute", flow.source().route());
        attributes.put("flowTargetRoute", flow.target().route());
        attributes.put("flowStatus", flow.status().name());
        attributes.put("semanticSource", "artifact-reuse");
        return new CodeChunk(
                UUID.nameUUIDFromBytes((metadata.appId() + "|" + metadata.baseUrlHash() + "|" + flow.flowId())
                        .getBytes(StandardCharsets.UTF_8)).toString(),
                Path.of("target", "artifact-reuse", "semantic", flow.flowId() + ".md").toAbsolutePath(),
                "target/artifact-reuse/semantic/" + flow.flowId() + ".md",
                "markdown", index, 0, text.length(), text,
                new ChunkMetadata(ArtifactType.DOCUMENTATION, "flow-contract", "ua.demo.agentlab.artifactreuse.flow",
                        List.of("artifact-reuse", "flow-contract", flow.type().name()), attributes)
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Qdrant semantic indexing failed" : value.trim();
    }

    static final class VectorConfigAdapter implements RagRuntimeConfig {
        private final KnowledgeVectorRuntimeConfig config;

        VectorConfigAdapter(KnowledgeVectorRuntimeConfig config) { this.config = config; }
        @Override public boolean enabled() { return config.enabled(); }
        @Override public String qdrantUrl() { return config.qdrantUrl(); }
        @Override public String qdrantApiKey() { return config.qdrantApiKey(); }
        @Override public String collectionName() { return config.collectionName(); }
        @Override public Path chunksJsonlPath() { return Path.of("target/artifact-reuse/semantic/chunks.jsonl"); }
        @Override public int chunkMaxChars() { return 1800; }
        @Override public int chunkOverlapChars() { return 0; }
        @Override public int retrievalLimit() { return 5; }
        @Override public String embeddingModel() { return config.embeddingModel(); }
        @Override public String generationModel() { return ""; }
        @Override public String openAiApiKey() { return config.openAiApiKey(); }
        @Override public String openAiBaseUrl() { return config.openAiBaseUrl(); }
        @Override public int maxOutputTokens() { return 0; }
    }
}
