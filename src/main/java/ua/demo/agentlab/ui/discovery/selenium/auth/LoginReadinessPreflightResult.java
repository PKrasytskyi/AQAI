package ua.demo.agentlab.ui.discovery.selenium.auth;

import java.util.List;

/** Evidence captured before discovery submits credentials to a protected SPA. */
public record LoginReadinessPreflightResult(
        boolean ready,
        boolean usernameVisible,
        boolean passwordVisible,
        boolean submitEnabled,
        boolean spaReady,
        String currentUrl,
        String documentReadyState,
        int visibleInputCount,
        int visibleButtonCount,
        String renderedDomSummary,
        List<String> consoleEvents,
        String screenshotPath,
        String failureReason
) {
    public LoginReadinessPreflightResult {
        currentUrl = safe(currentUrl);
        documentReadyState = safe(documentReadyState);
        renderedDomSummary = safe(renderedDomSummary);
        consoleEvents = consoleEvents == null ? List.of() : List.copyOf(consoleEvents);
        screenshotPath = safe(screenshotPath);
        failureReason = safe(failureReason);
        visibleInputCount = Math.max(0, visibleInputCount);
        visibleButtonCount = Math.max(0, visibleButtonCount);
    }

    public static LoginReadinessPreflightResult skipped(String reason) {
        return new LoginReadinessPreflightResult(false, false, false, false, false,
                "", "", 0, 0, "", List.of(), "", reason);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
