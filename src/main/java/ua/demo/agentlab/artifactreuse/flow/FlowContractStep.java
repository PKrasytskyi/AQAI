package ua.demo.agentlab.artifactreuse.flow;

public record FlowContractStep(
        int order,
        FlowActionType action,
        String ownerPage,
        String route,
        String dataKey,
        boolean setup
) {
    public FlowContractStep {
        order = Math.max(1, order);
        action = action == null ? FlowActionType.CLICK : action;
        ownerPage = safe(ownerPage);
        route = safe(route);
        dataKey = safe(dataKey);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
