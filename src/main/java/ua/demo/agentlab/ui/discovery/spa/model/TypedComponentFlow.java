package ua.demo.agentlab.ui.discovery.spa.model;

import java.util.List;

/** Candidate flow inside a page. It becomes reusable only after live verification and smoke feedback. */
public record TypedComponentFlow(
        String flowId, String pageId, String route, ComponentFlowType type,
        List<String> componentIds, List<String> actionIds, List<String> requiredLocatorIds,
        String targetRoute, List<FlowPostconditionContract> postconditions, double confidence, SpaEvidenceStatus status, List<String> sourceTrace
) {
    public TypedComponentFlow(String flowId, String pageId, String route, ComponentFlowType type,
                              List<String> componentIds, List<String> actionIds, List<String> requiredLocatorIds,
                              String targetRoute, double confidence, SpaEvidenceStatus status, List<String> sourceTrace) {
        this(flowId, pageId, route, type, componentIds, actionIds, requiredLocatorIds, targetRoute, List.of(), confidence, status, sourceTrace);
    }
    public TypedComponentFlow {
        flowId=safe(flowId); pageId=safe(pageId); route=safe(route); type=type==null?ComponentFlowType.MODULE_NAVIGATION:type;
        componentIds=copy(componentIds); actionIds=copy(actionIds); requiredLocatorIds=copy(requiredLocatorIds);
        targetRoute=safe(targetRoute); postconditions=postconditions==null?List.of():List.copyOf(postconditions); confidence=Double.isFinite(confidence)?Math.max(0d, Math.min(1d, confidence)):0d;
        status=status==null?SpaEvidenceStatus.CANDIDATE:status; sourceTrace=copy(sourceTrace);
    }
    private static String safe(String v){ return v==null?"":v.trim(); }
    private static List<String> copy(List<String> v){ return v==null?List.of():List.copyOf(v); }
}
