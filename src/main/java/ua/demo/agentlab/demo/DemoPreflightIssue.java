package ua.demo.agentlab.demo;

public record DemoPreflightIssue(String code, String message) {
    public DemoPreflightIssue {
        code = code == null ? "" : code.trim();
        message = message == null ? "" : message.trim();
    }
}
