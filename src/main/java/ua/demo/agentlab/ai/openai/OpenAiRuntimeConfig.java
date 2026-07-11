package ua.demo.agentlab.ai.openai;

public interface OpenAiRuntimeConfig {

    boolean enabled();

    boolean strict();

    String apiKey();

    String model();

    String baseUrl();

    boolean assistiveOnly();

    int maxOutputTokens();

    default boolean pageObjectLlmEnabled() {
        return false;
    }

    default boolean pageEnrichmentLlmEnabled() {
        return false;
    }

    default boolean uiTestLlmEnabled() {
        return false;
    }
}
