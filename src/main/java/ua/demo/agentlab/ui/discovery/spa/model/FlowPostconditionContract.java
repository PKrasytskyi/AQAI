package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** Expected state required before a candidate component flow is eligible for promotion. */
public record FlowPostconditionContract(FlowPostconditionType type, List<String> locatorIds,
                                        String expectedValue, boolean verifiable, String reason) {
    public FlowPostconditionContract {
        type = type == null ? FlowPostconditionType.RESULTS_CHANGED : type;
        locatorIds = locatorIds == null ? List.of() : List.copyOf(locatorIds);
        expectedValue = safe(expectedValue); reason = safe(reason);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
