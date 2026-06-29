package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;
import java.util.Objects;

public class ProjectContextRetriever {

    private final Retriever retriever;
    private final QueryIntentResolver queryIntentResolver;
    private final RagQueryBuilder ragQueryBuilder;
    private final RetrievalPolicyResolver retrievalPolicyResolver;
    private final ContextAssembler contextAssembler;

    public ProjectContextRetriever(
            Retriever retriever,
            QueryIntentResolver queryIntentResolver,
            RagQueryBuilder ragQueryBuilder,
            RetrievalPolicyResolver retrievalPolicyResolver,
            ContextAssembler contextAssembler
    ) {
        this.retriever = Objects.requireNonNull(retriever, "retriever cannot be null");
        this.queryIntentResolver = Objects.requireNonNull(queryIntentResolver, "queryIntentResolver cannot be null");
        this.ragQueryBuilder = Objects.requireNonNull(ragQueryBuilder, "ragQueryBuilder cannot be null");
        this.retrievalPolicyResolver = Objects.requireNonNull(retrievalPolicyResolver, "retrievalPolicyResolver cannot be null");
        this.contextAssembler = Objects.requireNonNull(contextAssembler, "contextAssembler cannot be null");
    }

    public ContextRetrievalResult retrieve(ContextRetrievalRequest request) {
        if (request == null || request.userRequest() == null || request.userRequest().isBlank()) {
            throw new IllegalArgumentException("request.userRequest cannot be blank");
        }

        QueryIntent intent = queryIntentResolver.resolve(request.userRequest());
        RagQuery ragQuery = ragQueryBuilder.build(intent);
        RetrievalPolicy retrievalPolicy = retrievalPolicyResolver.resolve(intent.taskClassification(), request.maxArtifacts());
        int desiredArtifacts = retrievalPolicy.desiredArtifacts();
        int retrievalLimit = Math.max(desiredArtifacts * retrievalPolicy.semanticRetrievalMultiplier(), desiredArtifacts);
        List<RetrievedChunk> rawMatches = retriever.retrieve(ragQuery.semanticQuery(), retrievalLimit);
        List<RetrievedChunk> contextChunks = contextAssembler.assemble(intent, rawMatches, desiredArtifacts);
        RetrievalTrace retrievalTrace = new RetrievalTrace(
                request.userRequest(),
                intent.taskClassification().taskType(),
                intent.taskClassification().confidenceScore(),
                ragQuery.semanticQuery(),
                desiredArtifacts,
                rawMatches.size(),
                contextChunks.size(),
                0,
                contextChunks.size(),
                contextChunks.size(),
                "SEMANTIC_ONLY"
        );
        return new ContextRetrievalResult(
                intent,
                ragQuery,
                retrievalPolicy,
                retrievalTrace,
                java.util.List.of(),
                contextChunks,
                rawMatches.size(),
                0,
                contextChunks.size(),
                "SEMANTIC_ONLY"
        );
    }
}
