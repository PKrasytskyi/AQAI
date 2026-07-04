package unit.tests.orchestration.pipeline;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;

public class WorkflowRunEnvelopeTest {

    @Test
    public void workflowStateDelegatesRunLevelDataToEnvelope() {
        WorkflowState state = new WorkflowState("Generate POM", new RequirementInput(SourceType.FILE, "requirements.md"));

        state.addArtifact("key", "value");
        state.addFinding("finding");
        state.addAudit("audit");
        state.fail("boom");

        WorkflowRunEnvelope envelope = state.runEnvelope();

        Assert.assertEquals(envelope.objective(), "Generate POM");
        Assert.assertEquals(envelope.artifactRefs().asMap().get("key"), "value");
        Assert.assertEquals(envelope.findings().entries(), java.util.List.of("finding"));
        Assert.assertEquals(envelope.audit().entries(), java.util.List.of("audit"));
        Assert.assertTrue(envelope.runMetadata().failed());
        Assert.assertEquals(envelope.runMetadata().failureReason(), "boom");
    }

    @Test
    public void envelopeFromStateReturnsSnapshot() {
        WorkflowState state = new WorkflowState("Generate POM", new RequirementInput(SourceType.FILE, "requirements.md"));
        state.addArtifact("before", "1");

        WorkflowRunEnvelope snapshot = WorkflowRunEnvelope.from(state);
        state.addArtifact("after", "2");

        Assert.assertTrue(snapshot.artifacts().containsKey("before"));
        Assert.assertFalse(snapshot.artifacts().containsKey("after"));
    }
}
