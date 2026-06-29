package ua.demo.agentlab.ai.openai;

public interface OpenAiRuntimeConfig {

    boolean enabled();

    boolean strict();

    String apiKey();

    String model();

    String baseUrl();

    boolean assistiveOnly();

    int maxOutputTokens();
}
