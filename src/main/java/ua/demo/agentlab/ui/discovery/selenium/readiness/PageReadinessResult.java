package ua.demo.agentlab.ui.discovery.selenium.readiness;

/**
 * Structured readiness evidence. Degraded states remain diagnostic evidence and must not be
 * promoted into mapper input or treated as a valid live-verification precondition.
 */
public record PageReadinessResult(
        boolean ready,
        boolean degraded,
        String route,
        String source,
        String reason
) {
    public PageReadinessResult {
        route = route == null ? "" : route.trim();
        source = source == null ? "" : source.trim();
        reason = reason == null ? "" : reason.trim();
        degraded = degraded || !ready;
    }

    public static PageReadinessResult ready(PageReadinessRule rule) {
        return new PageReadinessResult(true, false, rule == null ? "" : rule.route(),
                rule == null ? "default" : rule.source(), "");
    }

    public static PageReadinessResult degraded(PageReadinessRule rule, String reason) {
        return new PageReadinessResult(false, true, rule == null ? "" : rule.route(),
                rule == null ? "default" : rule.source(), reason);
    }
}
