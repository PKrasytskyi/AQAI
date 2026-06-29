package ua.demo.agentlab.ai.openai;

public final class OpenAiClientFactory {

    private OpenAiClientFactory() {
    }

    public static OpenAiRuntimeSettings requireRuntimeSettings(OpenAiRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtimeConfig cannot be null");
        }
        if (!runtimeConfig.enabled()) {
            throw new IllegalStateException(
                    "OpenAI assistive mode is disabled. Set 'openai.enabled=true' before running with '--ai'."
            );
        }
        String apiKey = runtimeConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not configured. Remove '--ai' or set the API key before running AI mode."
            );
        }
        String model = runtimeConfig.model();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("OpenAI model is not configured. Set 'openai.model'.");
        }

        return new OpenAiRuntimeSettings(
                apiKey.trim(),
                model.trim(),
                runtimeConfig.baseUrl(),
                runtimeConfig.assistiveOnly()
        );
    }
}
