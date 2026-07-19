package ua.demo.agentlab.ui.discovery.selenium.auth;

public record DiscoveryAuthenticationResult(
        boolean attempted,
        boolean protectedTarget,
        boolean credentialsPresent,
        boolean success,
        String loginUrl,
        String targetUrl,
        String finalUrl,
        String reason,
        LoginReadinessPreflightResult readinessPreflight
) {
    public DiscoveryAuthenticationResult {
        loginUrl = loginUrl == null ? "" : loginUrl.trim();
        targetUrl = targetUrl == null ? "" : targetUrl.trim();
        finalUrl = finalUrl == null ? "" : finalUrl.trim();
        reason = reason == null ? "" : reason.trim();
        readinessPreflight = readinessPreflight == null
                ? LoginReadinessPreflightResult.skipped("login readiness preflight was not required")
                : readinessPreflight;
    }

    public DiscoveryAuthenticationResult(boolean attempted, boolean protectedTarget, boolean credentialsPresent,
                                         boolean success, String loginUrl, String targetUrl, String finalUrl, String reason) {
        this(attempted, protectedTarget, credentialsPresent, success, loginUrl, targetUrl, finalUrl, reason,
                LoginReadinessPreflightResult.skipped("login readiness preflight was not captured"));
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
                reason,
                LoginReadinessPreflightResult.skipped(reason)
        );
    }
}
