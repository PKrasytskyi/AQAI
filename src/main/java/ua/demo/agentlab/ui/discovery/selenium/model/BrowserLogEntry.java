package ua.demo.agentlab.ui.discovery.selenium.model;

public record BrowserLogEntry(
        String level,
        String message,
        long timestamp
) {
    public BrowserLogEntry {
        level = level == null ? "" : level.trim();
        message = message == null ? "" : message.trim();
    }
}
