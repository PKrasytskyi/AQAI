package ua.demo.agentlab.ai.rag.store;

import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    void ensureCollection(int vectorSize);

    void upsert(List<CodeChunk> chunks, List<List<Double>> vectors);

    List<RetrievedChunk> search(List<Double> queryVector, int limit);

    default List<RetrievedChunk> search(List<Double> queryVector, int limit, Map<String, String> mustPayloadMatch) {
        return search(queryVector, limit);
    }
}
