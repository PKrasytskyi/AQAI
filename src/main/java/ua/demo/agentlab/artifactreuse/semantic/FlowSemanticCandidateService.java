package ua.demo.agentlab.artifactreuse.semantic;

import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient;
import ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore;
import ua.demo.agentlab.ai.rag.store.VectorStore;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractLookupResult;
import ua.demo.agentlab.artifactreuse.flow.FlowContractRegistry;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.artifactreuse.planner.FlowSemanticCandidate;
import ua.demo.agentlab.artifactreuse.planner.FlowSemanticCandidateBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.KnowledgeVectorRuntimeConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Qdrant ranks candidates; Neo4j remains the exact source of truth for reuse. */
public class FlowSemanticCandidateService {

    private final ArtifactReuseRuntimeConfig reuseConfig;
    private final KnowledgeVectorRuntimeConfig vectorConfig;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final FlowContractRegistry registry;

    public FlowSemanticCandidateService(
            ArtifactReuseRuntimeConfig reuseConfig,
            KnowledgeVectorRuntimeConfig vectorConfig,
            FlowContractRegistry registry
    ) {
        this(reuseConfig, vectorConfig, new OpenAiEmbeddingClient(new FlowSemanticIndexer.VectorConfigAdapter(vectorConfig)),
                new QdrantVectorStore(new FlowSemanticIndexer.VectorConfigAdapter(vectorConfig)), registry);
    }

    FlowSemanticCandidateService(
            ArtifactReuseRuntimeConfig reuseConfig,
            KnowledgeVectorRuntimeConfig vectorConfig,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            FlowContractRegistry registry
    ) {
        if (reuseConfig == null || vectorConfig == null || embeddingService == null || vectorStore == null || registry == null) {
            throw new IllegalArgumentException("semantic candidate service dependencies cannot be null");
        }
        this.reuseConfig = reuseConfig;
        this.vectorConfig = vectorConfig;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.registry = registry;
    }

    public FlowSemanticCandidateBundle findCandidates(CanonicalTestCaseBundle tests, KnowledgeRunMetadata metadata) {
        if (!reuseConfig.enabled() || !reuseConfig.semanticReuseEnabled()) return FlowSemanticCandidateBundle.unavailable("semantic reuse is disabled");
        if (!vectorConfig.enabled()) return FlowSemanticCandidateBundle.unavailable("Qdrant vector knowledge is disabled");
        if (vectorConfig.openAiApiKey() == null || vectorConfig.openAiApiKey().isBlank()) return FlowSemanticCandidateBundle.unavailable("embedding API key is not configured");
        if (tests == null || metadata == null) return FlowSemanticCandidateBundle.unavailable("canonical tests or namespace metadata is missing");
        try {
            Map<String, RetrievedChunk> matches = new LinkedHashMap<>();
            for (CanonicalTestCase testCase : tests.testCases()) {
                for (RetrievedChunk match : vectorStore.search(embeddingService.embed(query(testCase)), 5, filter(metadata))) {
                    String flowId = match.metadata().attributes().getOrDefault("flowId", "");
                    if (!flowId.isBlank()) matches.putIfAbsent(flowId, match);
                }
            }
            FlowContractLookupResult lookup = registry.findConfirmedByIdsWithResult(List.copyOf(matches.keySet()), metadata);
            List<FlowContract> verified = lookup.contracts();
            Map<String, FlowContract> verifiedById = new LinkedHashMap<>();
            verified.forEach(flow -> verifiedById.put(flow.flowId(), flow));
            List<FlowSemanticCandidate> candidates = new ArrayList<>();
            matches.forEach((flowId, match) -> {
                FlowContract flow = verifiedById.get(flowId);
                candidates.add(new FlowSemanticCandidate(flowId, flow == null ? flowType(match, flowId) : flow.type(),
                        flow == null ? attribute(match, "flowSourceRoute") : flow.source().route(),
                        flow == null ? attribute(match, "flowTargetRoute") : flow.target().route(),
                        match.score(), flow != null, flow,
                        flow == null ? unverifiedSource(lookup) : "qdrant+neo4j"));
            });
            return new FlowSemanticCandidateBundle(candidates, !matches.isEmpty(), !verified.isEmpty(), "",
                    lookup.message().isBlank() ? List.of() : List.of(lookup.message()));
        } catch (Exception exception) {
            return FlowSemanticCandidateBundle.unavailable(exception.getMessage());
        }
    }

    private Map<String, String> filter(KnowledgeRunMetadata metadata) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("knowledgeType", "flow-contract");
        result.put("flowStatus", "CONFIRMED");
        result.put("appId", metadata.appId());
        result.put("baseUrlHash", metadata.baseUrlHash());
        result.put("schemaVersion", metadata.schemaVersion());
        return result;
    }

    private String query(CanonicalTestCase testCase) {
        return String.join(" ", testCase.title(), String.join(" ", testCase.actions()),
                String.join(" ", testCase.assertions()), testCase.sourceRoute(), testCase.route());
    }

    private FlowContractType flowType(RetrievedChunk match, String flowId) {
        String value = attribute(match, "flowType");
        if (!value.isBlank()) {
            try {
                return FlowContractType.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                // Historical chunks may predate typed flow metadata.
            }
        }
        return new FlowSemanticCandidate(flowId, 0.0d, false, null, "").flowType();
    }

    private String attribute(RetrievedChunk match, String name) {
        if (match == null || match.metadata() == null || match.metadata().attributes() == null) return "";
        return match.metadata().attributes().getOrDefault(name, "");
    }

    private String unverifiedSource(FlowContractLookupResult lookup) {
        return lookup.success() ? "qdrant-unverified" : "qdrant-neo4j-verification-failed";
    }
}
