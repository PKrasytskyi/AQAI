package ua.demo.agentlab.ai.ui.generation;

import java.util.Map;

/** Mutable run-local counter isolated from generation decisions. */
public final class PomGenerationMetricsCollector {
    private int attempts;
    private int successes;
    private int promptChars;
    private int responseChars;
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;

    public void attempted(PomLlmExecutionService.PomLlmExecutionResult result) {
        attempts++;
        promptChars += result.promptChars();
        responseChars += result.responseChars();
        inputTokens += result.inputTokens();
        outputTokens += result.outputTokens();
        totalTokens += result.totalTokens();
    }

    public void succeeded() { successes++; }

    public Snapshot snapshot() {
        return new Snapshot(attempts, successes, promptChars, responseChars,
                inputTokens, outputTokens, totalTokens);
    }

    public void publish(Map<String, String> artifacts) {
        Snapshot value = snapshot();
        artifacts.put("pom.contract.llm.attempt.count", String.valueOf(value.attempts()));
        artifacts.put("pom.contract.llm.success.count", String.valueOf(value.successes()));
        artifacts.put("pom.contract.llm.failure.count", String.valueOf(value.failures()));
        artifacts.put("pom.contract.llm.prompt.chars", String.valueOf(value.promptChars()));
        artifacts.put("pom.contract.llm.response.chars", String.valueOf(value.responseChars()));
        artifacts.put("pom.contract.llm.input.tokens", String.valueOf(value.inputTokens()));
        artifacts.put("pom.contract.llm.output.tokens", String.valueOf(value.outputTokens()));
        artifacts.put("pom.contract.llm.total.tokens", String.valueOf(value.totalTokens()));
    }

    public record Snapshot(int attempts, int successes, int promptChars, int responseChars,
                           int inputTokens, int outputTokens, int totalTokens) {
        public int failures() { return Math.max(0, attempts - successes); }
    }
}
