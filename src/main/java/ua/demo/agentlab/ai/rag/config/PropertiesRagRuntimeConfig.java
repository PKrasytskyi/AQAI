package ua.demo.agentlab.ai.rag.config;

import java.nio.file.Path;
import java.util.Objects;
import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesRagRuntimeConfig implements RagRuntimeConfig {

    private final RuntimeProperties properties;

    public PropertiesRagRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesRagRuntimeConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
    }

    @Override
    public boolean enabled() {
        return properties.readKnowledgeDbBoolean("rag.enabled", "false");
    }

    @Override
    public String qdrantUrl() {
        return readValue("rag.qdrant.url", "http://localhost:6333");
    }

    @Override
    public String qdrantApiKey() {
        return readOptional("rag.qdrant.api-key", "RAG_QDRANT_API_KEY");
    }

    @Override
    public String collectionName() {
        return readValue("rag.qdrant.collection", "agentlab-project-style");
    }

    @Override
    public Path chunksJsonlPath() {
        return Path.of(readValue("rag.index.chunks-jsonl", "target/rag/chunks.jsonl")).toAbsolutePath().normalize();
    }

    @Override
    public int chunkMaxChars() {
        return Integer.parseInt(readValue("rag.chunk.max-chars", "1800"));
    }

    @Override
    public int chunkOverlapChars() {
        return Integer.parseInt(readValue("rag.chunk.overlap-chars", "250"));
    }

    @Override
    public int retrievalLimit() {
        return Integer.parseInt(readValue("rag.retrieval.limit", "8"));
    }

    @Override
    public String embeddingModel() {
        return readValue("rag.openai.embedding-model", "text-embedding-3-small");
    }

    @Override
    public String generationModel() {
        return readValue("rag.openai.generation-model", readValue("openai.model", "gpt-5-mini"));
    }

    @Override
    public String openAiApiKey() {
        return firstNonBlank(
                readOptional("rag.openai.api-key", "RAG_OPENAI_API_KEY"),
                readOptional("openai.api-key", "OPENAI_API_KEY")
        );
    }

    @Override
    public String openAiBaseUrl() {
        return readValue("rag.openai.base-url", readValue("openai.base-url", "https://api.openai.com/v1"));
    }

    @Override
    public int maxOutputTokens() {
        return Integer.parseInt(readValue("rag.openai.max-output-tokens", "1800"));
    }

    private String readValue(String key, String defaultValue) {
        String value = properties.readOptional(key, key.toUpperCase().replace('.', '_').replace('-', '_'));
        return value == null || value.isBlank() ? Objects.requireNonNull(defaultValue) : value.trim();
    }

    private String readOptional(String propertyKey, String environmentKey) {
        return properties.readOptional(propertyKey, environmentKey);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
