package ua.demo.agentlab.ai.rag.config;

import java.nio.file.Path;

public interface RagRuntimeConfig {

    boolean enabled();

    String qdrantUrl();

    String qdrantApiKey();

    String collectionName();

    Path chunksJsonlPath();

    int chunkMaxChars();

    int chunkOverlapChars();

    int retrievalLimit();

    String embeddingModel();

    String generationModel();

    String openAiApiKey();

    String openAiBaseUrl();

    int maxOutputTokens();
}
