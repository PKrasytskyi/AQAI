package ua.demo.agentlab.ai.rag.model;

import java.util.List;

import ua.demo.agentlab.ai.rag.retrieval.RetrievalTrace;

public record RagGenerationResult(
        String prompt,
        List<RetrievedChunk> matches,
        String generatedText,
        RetrievalTrace retrievalTrace
) {
}
