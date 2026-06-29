package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.graph.GraphQueryResult;
import ua.demo.agentlab.ai.rag.graph.GraphQueryService;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HybridContextRetriever {

    private final ProjectContextRetriever semanticRetriever;
    private final GraphQueryService graphQueryService;
    private final MetadataContextFilter metadataContextFilter;
    private final ContextReranker contextReranker;

    public HybridContextRetriever(
            ProjectContextRetriever semanticRetriever,
            GraphQueryService graphQueryService,
            MetadataContextFilter metadataContextFilter,
            ContextReranker contextReranker
    ) {
        this.semanticRetriever = Objects.requireNonNull(semanticRetriever, "semanticRetriever cannot be null");
        this.graphQueryService = Objects.requireNonNull(graphQueryService, "graphQueryService cannot be null");
        this.metadataContextFilter = Objects.requireNonNull(metadataContextFilter, "metadataContextFilter cannot be null");
        this.contextReranker = Objects.requireNonNull(contextReranker, "contextReranker cannot be null");
    }

    public ContextRetrievalResult retrieve(ContextRetrievalRequest request) {
        ContextRetrievalResult semanticResult = semanticRetriever.retrieve(request);
        RetrievalPolicy retrievalPolicy = semanticResult.retrievalPolicy();
        int desiredArtifacts = retrievalPolicy.desiredArtifacts();
        GraphQueryResult graphQueryResult = graphQueryService.expand(
                semanticResult.intent(),
                semanticResult.contextChunks(),
                desiredArtifacts * retrievalPolicy.graphExpansionMultiplier()
        );
        List<RetrievedChunk> combined = new ArrayList<>(semanticResult.contextChunks());
        combined.addAll(graphQueryResult.relatedChunks());
        MetadataFilterResult filterResult = metadataContextFilter.filter(semanticResult.intent(), combined);
        RerankResult rerankResult = contextReranker.rerank(
                semanticResult.intent(),
                filterResult.filteredChunks(),
                desiredArtifacts
        );
        List<RetrievedChunk> rerankedContext = rerankResult.rerankedChunks();
        RetrievalTrace retrievalTrace = new RetrievalTrace(
                request.userRequest(),
                semanticResult.intent().taskClassification().taskType(),
                semanticResult.intent().taskClassification().confidenceScore(),
                semanticResult.ragQuery().semanticQuery(),
                desiredArtifacts,
                semanticResult.rawMatchCount(),
                semanticResult.contextChunks().size(),
                graphQueryResult.expandedArtifactCount(),
                filterResult.filteredChunks().size(),
                rerankedContext.size(),
                graphQueryResult.source()
        );
        return new ContextRetrievalResult(
                semanticResult.intent(),
                semanticResult.ragQuery(),
                retrievalPolicy,
                retrievalTrace,
                rerankResult.explanations(),
                rerankedContext,
                semanticResult.rawMatchCount(),
                graphQueryResult.expandedArtifactCount(),
                filterResult.filteredChunks().size(),
                graphQueryResult.source()
        );
    }
}
