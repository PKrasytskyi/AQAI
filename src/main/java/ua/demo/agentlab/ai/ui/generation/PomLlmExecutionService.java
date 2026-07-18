package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;

/** Isolates the only POM LLM side effect and captures exact usage metadata. */
public final class PomLlmExecutionService {
    private final OpenAiResponseGenerationClient client;

    public PomLlmExecutionService(OpenAiResponseGenerationClient client) {
        this.client = client;
    }

    public PomLlmExecutionResult execute(String prompt) {
        String response = client.generate(prompt);
        var usage = client.lastUsage();
        return new PomLlmExecutionResult(response, prompt == null ? 0 : prompt.length(),
                response == null ? 0 : response.length(), usage.inputTokens(), usage.outputTokens(), usage.totalTokens());
    }

    public record PomLlmExecutionResult(String response, int promptChars, int responseChars,
                                        int inputTokens, int outputTokens, int totalTokens) {}
}
