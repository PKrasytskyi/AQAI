package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

public record FlowContractBundle(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        List<FlowContract> contracts
) {
    public FlowContractBundle {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? "flow-contract-bundle.v1" : schemaVersion.trim();
        contracts = contracts == null ? List.of() : List.copyOf(contracts);
    }

    public int confirmedCount() {
        return (int) contracts.stream().filter(FlowContract::confirmed).count();
    }

    public int needsReviewCount() {
        return contracts.size() - confirmedCount();
    }
}
