package ua.demo.agentlab.ui.discovery.selenium.auth;

public record DiscoveryAuthenticationResult(
        boolean attempted,
        boolean protectedTarget,
        boolean credentialsPresent,
        boolean success,
        String loginUrl,
        String targetUrl,
        String finalUrl,
        String reason
) {
    public DiscoveryAuthenticationResult {
        loginUrl = loginUrl == null ? "" : loginUrl.trim();
        targetUrl = targetUrl == null ? "" : targetUrl.trim();
        finalUrl = finalUrl == null ? "" : finalUrl.trim();
        reason = reason == null ? "" : reason.trim();
    }

    public static DiscoveryAuthenticationResult skipped(
            boolean protectedTarget,
            boolean credentialsPresent,
            String targetUrl,
            String reason
    ) {
        return new DiscoveryAuthenticationResult(
                false,
                protectedTarget,
                credentialsPresent,
                false,
                "",
                targetUrl,
                "",
                reason
        );
    }
}
