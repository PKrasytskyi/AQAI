package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;
import java.util.Map;

/** Requirement behavior bound only to current-run confirmed SPA evidence. */
public record BoundSpaBehaviorContract(String requirementId, String capability, String pageId, String route, String flowId,
                                       List<String> componentIds, List<BoundSpaBehaviorStep> steps,
                                       List<BoundSpaBehaviorAssertion> assertions, Map<String, String> resolvedData,
                                       boolean executable, List<String> reviewReasons) {
    public BoundSpaBehaviorContract {
        requirementId = safe(requirementId); capability = safe(capability); pageId = safe(pageId); route = safe(route); flowId = safe(flowId);
        componentIds = componentIds == null ? List.of() : List.copyOf(componentIds);
        steps = steps == null ? List.of() : List.copyOf(steps); assertions = assertions == null ? List.of() : List.copyOf(assertions);
        resolvedData = resolvedData == null ? Map.of() : Map.copyOf(resolvedData); reviewReasons = reviewReasons == null ? List.of() : List.copyOf(reviewReasons);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
