package ua.demo.agentlab.ui.discovery.pagemodel.model;

public record PageFlowModel(
        String flowId,
        String fromPageId,
        String actionLabel,
        String actionType,
        String toPageId,
        String toUrl,
        boolean success
) {
    public PageFlowModel {
        flowId = flowId == null ? "" : flowId.trim();
        fromPageId = fromPageId == null ? "" : fromPageId.trim();
        actionLabel = actionLabel == null ? "" : actionLabel.trim();
        actionType = actionType == null ? "" : actionType.trim();
        toPageId = toPageId == null ? "" : toPageId.trim();
        toUrl = toUrl == null ? "" : toUrl.trim();
    }
}
