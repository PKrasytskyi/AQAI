package ua.demo.agentlab.ai.rag.embedding;

import java.util.List;

public interface EmbeddingService {

    List<Double> embed(String input);
}
