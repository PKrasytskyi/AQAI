package ua.demo.agentlab.ai.openai;

public record OpenAiRuntimeSettings(
        String apiKey,
        String model,
        String baseUrl,
        boolean assistiveOnly
) {
}
