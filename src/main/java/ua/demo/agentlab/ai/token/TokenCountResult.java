package ua.demo.agentlab.ai.token;

public record TokenCountResult(
        int tokens,
        String countingMode,
        String tokenizerModel
) {
    public TokenCountResult {
        tokens = Math.max(0, tokens);
        countingMode = countingMode == null || countingMode.isBlank() ? "unknown" : countingMode.trim();
        tokenizerModel = tokenizerModel == null || tokenizerModel.isBlank() ? "unknown" : tokenizerModel.trim();
    }
}
