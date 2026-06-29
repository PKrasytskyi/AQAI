package ua.demo.agentlab.ui.discovery.persistence.knowledge.config;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class PropertiesKnowledgeVectorRuntimeConfig implements KnowledgeVectorRuntimeConfig {

    private final Properties properties = new Properties();

    public PropertiesKnowledgeVectorRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesKnowledgeVectorRuntimeConfig(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find knowledge vector config resource: " + resourceName);
            }
            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load knowledge vector config resource: " + resourceName, exception);
        }
    }

    @Override
    public boolean enabled() {
        return Boolean.parseBoolean(readValue("knowledge.vector.enabled", "false"));
    }

    @Override
    public String qdrantUrl() {
        return readValue("knowledge.vector.qdrant.url", readValue("rag.qdrant.url", "http://localhost:6333"));
    }

    @Override
    public String qdrantApiKey() {
        return firstNonBlank(
                readSystemProperty("knowledge.vector.qdrant.api-key"),
                readEnvironment("KNOWLEDGE_VECTOR_QDRANT_API_KEY"),
                properties.getProperty("knowledge.vector.qdrant.api-key"),
                readSystemProperty("rag.qdrant.api-key"),
                readEnvironment("RAG_QDRANT_API_KEY"),
                properties.getProperty("rag.qdrant.api-key")
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
                readSystemProperty("knowledge.vector.openai.api-key"),
                readEnvironment("KNOWLEDGE_VECTOR_OPENAI_API_KEY"),
                properties.getProperty("knowledge.vector.openai.api-key"),
                readSystemProperty("rag.openai.api-key"),
                readEnvironment("RAG_OPENAI_API_KEY"),
                properties.getProperty("rag.openai.api-key"),
                readSystemProperty("openai.api-key"),
                readEnvironment("OPENAI_API_KEY"),
                properties.getProperty("openai.api-key")
        );
    }

    @Override
    public String openAiBaseUrl() {
        return readValue("knowledge.vector.openai.base-url",
                readValue("rag.openai.base-url", readValue("openai.base-url", "https://api.openai.com/v1")));
    }

    private String readValue(String key, String defaultValue) {
        String value = firstNonBlank(
                readSystemProperty(key),
                readEnvironment(key),
                properties.getProperty(key)
        );
        return value == null || value.isBlank() ? Objects.requireNonNull(defaultValue) : value.trim();
    }

    private String readSystemProperty(String key) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String readEnvironment(String keyOrEnvName) {
        String normalized = keyOrEnvName.contains(".")
                ? keyOrEnvName.toUpperCase().replace('.', '_').replace('-', '_')
                : keyOrEnvName;
        String value = System.getenv(normalized);
        return value == null || value.isBlank() ? null : value.trim();
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
