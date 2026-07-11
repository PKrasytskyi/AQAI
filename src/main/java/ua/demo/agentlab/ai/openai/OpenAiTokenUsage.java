package ua.demo.agentlab.ai.openai;

public record OpenAiTokenUsage(
        int inputTokens,
        int outputTokens,
        int totalTokens
) {
    public static final OpenAiTokenUsage EMPTY = new OpenAiTokenUsage(0, 0, 0);

    public OpenAiTokenUsage {
        inputTokens = Math.max(0, inputTokens);
        outputTokens = Math.max(0, outputTokens);
        totalTokens = Math.max(0, totalTokens);
        if (totalTokens == 0 && (inputTokens > 0 || outputTokens > 0)) {
            totalTokens = inputTokens + outputTokens;
        }
    }

    public boolean available() {
        return totalTokens > 0;
    }
}
