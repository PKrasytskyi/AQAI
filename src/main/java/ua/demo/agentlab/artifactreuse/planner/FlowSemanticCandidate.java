package ua.demo.agentlab.artifactreuse.planner;

import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;

public record FlowSemanticCandidate(
        String flowId,
        FlowContractType flowType,
        String sourceRoute,
        String targetRoute,
        double semanticScore,
        boolean graphConfirmed,
        FlowContract contract,
        String source
) {
    public FlowSemanticCandidate {
        flowId = flowId == null ? "" : flowId.trim();
        flowType = flowType == null ? FlowContractType.GENERIC : flowType;
        sourceRoute = sourceRoute == null ? "" : sourceRoute.trim();
        targetRoute = targetRoute == null ? "" : targetRoute.trim();
        semanticScore = Double.isFinite(semanticScore) ? Math.max(0.0d, Math.min(1.0d, semanticScore)) : 0.0d;
        source = source == null ? "" : source.trim();
    }

    public FlowSemanticCandidate(
            String flowId,
            double semanticScore,
            boolean graphConfirmed,
            FlowContract contract,
            String source
    ) {
        this(flowId,
                contract == null ? inferType(flowId) : contract.type(),
                contract == null ? "" : contract.source().route(),
                contract == null ? "" : contract.target().route(),
                semanticScore, graphConfirmed, contract, source);
    }

    public boolean reusable() {
        return graphConfirmed && contract != null && contract.reuseQuality().reusable() && !flowId.isBlank();
    }

    public boolean confirmedButDegraded() {
        return graphConfirmed && contract != null && contract.confirmed() && !contract.reuseQuality().reusable();
    }

    private static FlowContractType inferType(String flowId) {
        if (flowId == null || flowId.isBlank()) return FlowContractType.GENERIC;
        int delimiter = flowId.indexOf('_');
        if (delimiter < 1) return FlowContractType.GENERIC;
        try {
            return FlowContractType.valueOf(flowId.substring(0, delimiter));
        } catch (IllegalArgumentException ignored) {
            return FlowContractType.GENERIC;
        }
    }
}
