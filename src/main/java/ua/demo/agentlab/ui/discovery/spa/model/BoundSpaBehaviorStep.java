package ua.demo.agentlab.ui.discovery.spa.model;

/** A requirement action bound to an existing, browser-confirmed SPA action and locator. */
public record BoundSpaBehaviorStep(String kind, String actionId, String locatorId, String dataKey, String value) {
    public BoundSpaBehaviorStep {
        kind = safe(kind); actionId = safe(actionId); locatorId = safe(locatorId); dataKey = safe(dataKey); value = safe(value);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
