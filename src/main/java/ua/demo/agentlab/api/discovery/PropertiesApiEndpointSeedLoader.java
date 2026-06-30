package ua.demo.agentlab.api.discovery;

import ua.demo.agentlab.api.model.ApiEndpointBundle;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesApiEndpointSeedLoader {

    private final Properties properties = new Properties();
    private final ApiEndpointSeedParser parser;

    public PropertiesApiEndpointSeedLoader() {
        this("framework.properties", new ApiEndpointSeedParser());
    }

    public PropertiesApiEndpointSeedLoader(String resourceName) {
        this(resourceName, new ApiEndpointSeedParser());
    }

    PropertiesApiEndpointSeedLoader(String resourceName, ApiEndpointSeedParser parser) {
        this.parser = parser == null ? new ApiEndpointSeedParser() : parser;
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find API endpoint seed resource: " + resourceName);
            }
            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load API endpoint seed resource: " + resourceName, exception);
        }
    }

    public ApiEndpointBundle load() {
        String seed = readText("project.api.endpoint-seed", "");
        return parser.parse("project.api.endpoint-seed", seed);
    }

    private String readText(String key, String fallback) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        return fallback;
    }
}
