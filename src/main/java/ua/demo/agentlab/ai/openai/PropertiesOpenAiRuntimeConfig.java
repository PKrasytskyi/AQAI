package ua.demo.agentlab.ai.openai;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class PropertiesOpenAiRuntimeConfig implements OpenAiRuntimeConfig {

    private final Properties properties = new Properties();

    public PropertiesOpenAiRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesOpenAiRuntimeConfig(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find OpenAI config resource: " + resourceName);
            }
            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load OpenAI config resource: " + resourceName, exception);
        }
    }

    @Override
    public boolean enabled() {
        return Boolean.parseBoolean(readValue("openai.enabled", "false"));
    }

    @Override
    public boolean strict() {
        return Boolean.parseBoolean(readValue("openai.strict", "false"));
    }

    @Override
    public String apiKey() {
        return readOptional("openai.api-key", "OPENAI_API_KEY");
    }

    @Override
    public String model() {
        return readValue("openai.model", "gpt-5-mini");
    }

    @Override
    public String baseUrl() {
        return readValue("openai.base-url", "https://api.openai.com/v1");
    }

    @Override
    public boolean assistiveOnly() {
        return Boolean.parseBoolean(readValue("openai.assistive-only", "true"));
    }

    @Override
    public int maxOutputTokens() {
        return Integer.parseInt(readValue("openai.max-output-tokens", "4000"));
    }

    private String readValue(String key, String defaultValue) {
        String value = readOptional(key, key.toUpperCase().replace('.', '_').replace('-', '_'));
        return value == null || value.isBlank() ? Objects.requireNonNull(defaultValue) : value.trim();
    }

    private String readOptional(String propertyKey, String envKey) {
        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return null;
    }
}
