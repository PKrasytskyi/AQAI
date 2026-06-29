package ua.demo.agentlab.ai.openai;

import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;

import java.nio.file.Path;

public class OpenAiRuntimeConfigRagAdapter implements RagRuntimeConfig {

    private final OpenAiRuntimeConfig config;

    public OpenAiRuntimeConfigRagAdapter(OpenAiRuntimeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
    }

    @Override
    public boolean enabled() {
        return config.enabled();
    }

    @Override
    public String qdrantUrl() {
        return "http://localhost:6333";
    }

    @Override
    public String qdrantApiKey() {
        return null;
    }

    @Override
    public String collectionName() {
        return "unused-openai-test-plan";
    }

    @Override
    public Path chunksJsonlPath() {
        return Path.of("target/openai/test-plan/chunks.jsonl").toAbsolutePath().normalize();
    }

    @Override
    public int chunkMaxChars() {
        return 1800;
    }

    @Override
    public int chunkOverlapChars() {
        return 0;
    }

    @Override
    public int retrievalLimit() {
        return 8;
    }

    @Override
    public String embeddingModel() {
        return "text-embedding-3-small";
    }

    @Override
    public String generationModel() {
        return config.model();
    }

    @Override
    public String openAiApiKey() {
        return config.apiKey();
    }

    @Override
    public String openAiBaseUrl() {
        return config.baseUrl();
    }

    @Override
    public int maxOutputTokens() {
        return config.maxOutputTokens();
    }
}
