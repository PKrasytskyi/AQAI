package ua.demo.agentlab.ai.openai;

import java.util.Objects;
import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesOpenAiRuntimeConfig implements OpenAiRuntimeConfig {

    private final RuntimeProperties properties;

    public PropertiesOpenAiRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesOpenAiRuntimeConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
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
        return firstNonBlank(
                readOptional("openai.api-key", "OPENAI_API_KEY"),
                readOptional("rag.openai.api-key", "RAG_OPENAI_API_KEY"),
                readOptional("knowledge.vector.openai.api-key", "KNOWLEDGE_VECTOR_OPENAI_API_KEY")
        );
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

    @Override
    public boolean pageObjectLlmEnabled() {
        return Boolean.parseBoolean(readValue("ai.page-object.llm.enabled", "false"));
    }

    @Override
    public boolean pageEnrichmentLlmEnabled() {
        return Boolean.parseBoolean(readValue("ai.page-enrichment.llm.enabled", "true"));
    }

    @Override
    public boolean uiTestLlmEnabled() {
        return Boolean.parseBoolean(readValue("ai.ui-test.llm.enabled", "false"));
    }

    private String readValue(String key, String defaultValue) {
        String value = readOptional(key, key.toUpperCase().replace('.', '_').replace('-', '_'));
        return value == null || value.isBlank() ? Objects.requireNonNull(defaultValue) : value.trim();
    }

    private String readOptional(String propertyKey, String envKey) {
        return properties.readOptional(propertyKey, envKey);
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
