package ua.demo.agentlab.ai.rag.graph;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.retrieval.QueryIntent;

import java.util.List;
import java.util.Objects;

public class FallbackGraphQueryService implements GraphQueryService {

    private final GraphContextExpander graphContextExpander;

    public FallbackGraphQueryService(GraphContextExpander graphContextExpander) {
        this.graphContextExpander = Objects.requireNonNull(graphContextExpander, "graphContextExpander cannot be null");
    }

    @Override
    public GraphQueryResult expand(QueryIntent intent, List<RetrievedChunk> semanticSeeds, int maxArtifacts) {
        List<RetrievedChunk> expanded = graphContextExpander.expand(semanticSeeds, maxArtifacts);
        int matchedSeeds = semanticSeeds == null ? 0 : semanticSeeds.size();
        int additional = Math.max(0, expanded.size() - matchedSeeds);
        return new GraphQueryResult(expanded, matchedSeeds, additional, "LEGACY_CODE_GRAPH");
    }
}
