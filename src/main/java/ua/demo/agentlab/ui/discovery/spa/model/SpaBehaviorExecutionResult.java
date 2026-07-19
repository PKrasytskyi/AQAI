package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** Outcome of one isolated live browser execution of a bound structured behavior contract. */
public record SpaBehaviorExecutionResult(String requirementId, String capability, String pageId, String route, String flowId,
                                         String status, List<String> locatorIds, List<String> actionIds,
                                         List<String> reasons, List<String> sourceTrace) {
    public SpaBehaviorExecutionResult {
        requirementId = safe(requirementId); capability = safe(capability); pageId = safe(pageId); route = safe(route); flowId = safe(flowId); status = safe(status);
        locatorIds = locatorIds == null ? List.of() : List.copyOf(locatorIds); actionIds = actionIds == null ? List.of() : List.copyOf(actionIds);
        reasons = reasons == null ? List.of() : List.copyOf(reasons); sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }
    public boolean passed() { return "PASSED".equalsIgnoreCase(status); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
