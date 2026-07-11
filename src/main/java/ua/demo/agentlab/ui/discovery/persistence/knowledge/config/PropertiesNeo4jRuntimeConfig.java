package ua.demo.agentlab.ui.discovery.persistence.knowledge.config;

import java.util.Objects;
import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesNeo4jRuntimeConfig implements Neo4jRuntimeConfig {

    private final RuntimeProperties properties;

    public PropertiesNeo4jRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesNeo4jRuntimeConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
    }

    @Override
    public boolean enabled() {
        return properties.readKnowledgeDbBoolean("knowledge.graph.enabled", "false");
    }

    @Override
    public String httpUrl() {
        return readValue("knowledge.graph.neo4j.url", "http://localhost:7474");
    }

    @Override
    public String database() {
        return readValue("knowledge.graph.neo4j.database", "neo4j");
    }

    @Override
    public String username() {
        return readValue("knowledge.graph.neo4j.username", "neo4j");
    }

    @Override
    public String password() {
        return readOptional("knowledge.graph.neo4j.password", "KNOWLEDGE_GRAPH_NEO4J_PASSWORD");
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
