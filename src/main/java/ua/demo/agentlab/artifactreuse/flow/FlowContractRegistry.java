package ua.demo.agentlab.artifactreuse.flow;

public interface FlowContractRegistry {

    FlowContractPersistenceResult persist(FlowContractBundle bundle);

    default FlowContractLookupResult findConfirmedByIdsWithResult(
            java.util.List<String> flowIds,
            ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata namespace
    ) {
        return FlowContractLookupResult.skipped("flow registry exact lookup is not implemented");
    }

    default java.util.List<FlowContract> findConfirmedByIds(
            java.util.List<String> flowIds,
            ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata namespace
    ) {
        return findConfirmedByIdsWithResult(flowIds, namespace).contracts();
    }

    default FlowRuntimeFeedbackResult recordRuntimeFeedback(FlowContractBundle bundle, FlowRuntimeFeedback feedback) {
        return FlowRuntimeFeedbackResult.skipped("none", "flow runtime feedback persistence is not implemented");
    }
}
