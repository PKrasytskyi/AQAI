package ua.demo.agentlab.ui.discovery.persistence.knowledge.config;

import java.util.Objects;
import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesKnowledgeVectorRuntimeConfig implements KnowledgeVectorRuntimeConfig {

    private final RuntimeProperties properties;

    public PropertiesKnowledgeVectorRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesKnowledgeVectorRuntimeConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
    }

    @Override
    public boolean enabled() {
        return properties.readKnowledgeDbBoolean("knowledge.vector.enabled", "false");
    }

    @Override
    public String qdrantUrl() {
        return readValue("knowledge.vector.qdrant.url", readValue("rag.qdrant.url", "http://localhost:6333"));
    }

    @Override
    public String qdrantApiKey() {
        return firstNonBlank(
                properties.readOptional("knowledge.vector.qdrant.api-key", "KNOWLEDGE_VECTOR_QDRANT_API_KEY"),
                properties.readOptional("rag.qdrant.api-key", "RAG_QDRANT_API_KEY")
        );
    }

    @Override
    public String collectionName() {
        return readValue("knowledge.vector.qdrant.collection", "agentlab-ui-knowledge");
    }

    @Override
    public String embeddingModel() {
        return readValue("knowledge.vector.openai.embedding-model",
                readValue("rag.openai.embedding-model", "text-embedding-3-small"));
    }

    @Override
    public String openAiApiKey() {
        return firstNonBlank(
                properties.readOptional("knowledge.vector.openai.api-key", "KNOWLEDGE_VECTOR_OPENAI_API_KEY"),
                properties.readOptional("rag.openai.api-key", "RAG_OPENAI_API_KEY"),
                properties.readOptional("openai.api-key", "OPENAI_API_KEY")
        );
    }

    @Override
    public String openAiBaseUrl() {
        return readValue("knowledge.vector.openai.base-url",
                readValue("rag.openai.base-url", readValue("openai.base-url", "https://api.openai.com/v1")));
    }

    private String readValue(String key, String defaultValue) {
        String value = properties.readOptional(key, key.toUpperCase().replace('.', '_').replace('-', '_'));
        return value == null || value.isBlank() ? Objects.requireNonNull(defaultValue) : value.trim();
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
