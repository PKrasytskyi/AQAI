package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.List;

public class FlowContractPersistencePolicy {

    public FlowContractBundle eligible(FlowContractBundle bundle) {
        if (bundle == null) {
            return new FlowContractBundle("flow-contract-bundle.v1", null, List.of());
        }
        return new FlowContractBundle(
                bundle.schemaVersion(),
                bundle.runMetadata(),
                bundle.contracts().stream().filter(this::isEligible).toList()
        );
    }

    public boolean isEligible(FlowContract contract) {
        if (contract == null || !contract.confirmed() || contract.confidence() < 0.80d) {
            return false;
        }
        if (!validEndpoint(contract.source()) || !validEndpoint(contract.target()) || contract.steps().isEmpty()) {
            return false;
        }
        if (contract.requirementIds().isEmpty() || contract.evidence().stream().noneMatch(this::liveEvidence)) {
            return false;
        }
        return contract.steps().stream().allMatch(step -> step != null
                && step.action() != null
                && !step.ownerPage().isBlank()
                && !RouteCanonicalizer.canonicalize(step.route()).isBlank());
    }

    private boolean validEndpoint(FlowEndpoint endpoint) {
        return endpoint != null
                && !endpoint.pageName().isBlank()
                && !RouteCanonicalizer.canonicalize(endpoint.route()).isBlank();
    }

    private boolean liveEvidence(String source) {
        String normalized = source == null ? "" : source.toLowerCase();
        return normalized.startsWith("transition:")
                || normalized.startsWith("action-evidence:")
                || normalized.startsWith("runtime:")
                || normalized.startsWith("smoke:");
    }
}
