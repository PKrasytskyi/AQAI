package unit.tests.ui.discovery.persistence;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

public class KnowledgeRunMetadataTest {

    @Test
    public void usesWorkflowRunIdAsTheDiscoveryAndKnowledgeNamespace() {
        WorkflowState state = new WorkflowState(
                "test",
                new RequirementInput(SourceType.FILE, "requirements/test.md")
        );

        KnowledgeRunMetadata metadata = KnowledgeRunMetadata.from(state, "test-agent");

        Assert.assertEquals(metadata.runId(), state.runEnvelope().runMetadata().runId());
    }
}
