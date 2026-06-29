package ua.demo.agentlab.ai.rag.graph;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.retrieval.QueryIntent;

import java.util.List;

public interface GraphQueryService {

    GraphQueryResult expand(QueryIntent intent, List<RetrievedChunk> semanticSeeds, int maxArtifacts);
}
