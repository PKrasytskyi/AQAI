package ua.demo.agentlab.ui.discovery.persistence.knowledge.config;

public interface KnowledgeVectorRuntimeConfig {

    boolean enabled();

    String qdrantUrl();

    String qdrantApiKey();

    String collectionName();

    String embeddingModel();

    String openAiApiKey();

    String openAiBaseUrl();
}
