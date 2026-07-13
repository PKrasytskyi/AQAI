package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

public record FlowContractBuilderInput(
        CanonicalTestCaseBundle canonicalTestCases,
        CanonicalPageFlowModel canonicalPageFlows,
        MappedUiKnowledge mappedUiKnowledge,
        KnowledgeRunMetadata runMetadata
) {
}
