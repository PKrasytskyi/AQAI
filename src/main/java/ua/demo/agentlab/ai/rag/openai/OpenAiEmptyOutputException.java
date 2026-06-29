package ua.demo.agentlab.ai.rag.openai;

public class OpenAiEmptyOutputException extends IllegalStateException {

    private final String rawResponse;

    public OpenAiEmptyOutputException(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse == null ? "" : rawResponse;
    }

    public String rawResponse() {
        return rawResponse;
    }
}
