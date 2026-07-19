package ua.demo.agentlab.ui.discovery.browser;

public record BrowserCapabilityResult(boolean executed, boolean verified, String reason) {
    public static BrowserCapabilityResult passed(String reason) {
        return new BrowserCapabilityResult(true, true, reason);
    }

    public static BrowserCapabilityResult failed(String reason) {
        return new BrowserCapabilityResult(true, false, reason);
    }

    public static BrowserCapabilityResult unsupported(String reason) {
        return new BrowserCapabilityResult(false, false, reason);
    }
}
