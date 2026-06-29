package ua.demo.agentlab.ui.discovery.persistence.knowledge.config;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class PropertiesNeo4jRuntimeConfig implements Neo4jRuntimeConfig {

    private final Properties properties = new Properties();

    public PropertiesNeo4jRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesNeo4jRuntimeConfig(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find Neo4j config resource: " + resourceName);
            }
            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load Neo4j config resource: " + resourceName, exception);
        }
    }

    @Override
    public boolean enabled() {
        return Boolean.parseBoolean(readValue("knowledge.graph.enabled", "false"));
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
        String value = firstNonBlank(
                readSystemProperty(key),
                readEnvironment(key),
                properties.getProperty(key)
        );
        return value == null || value.isBlank() ? Objects.requireNonNull(defaultValue) : value.trim();
    }

    private String readOptional(String propertyKey, String environmentKey) {
        return firstNonBlank(
                readSystemProperty(propertyKey),
                readEnvironment(environmentKey),
                properties.getProperty(propertyKey)
        );
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
