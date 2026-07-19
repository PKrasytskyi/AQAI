package ua.demo.agentlab.ui.discovery.browser;

public record BrowserCapabilityRequest(
        BrowserCapabilityAction action,
        String strategy,
        String locator,
        String value,
        String username,
        String password,
        String targetUrl
) {
    public BrowserCapabilityRequest {
        strategy = safe(strategy);
        locator = safe(locator);
        value = safe(value);
        username = safe(username);
        password = password == null ? "" : password;
        targetUrl = safe(targetUrl);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
