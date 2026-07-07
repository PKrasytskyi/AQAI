package unit.tests.orchestration.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public class WorkflowPipelineSnapshotTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void snapshotCapturesTypedStagesFromWorkflowState() {
        WorkflowState state = new WorkflowState("Generate POM", new RequirementInput(SourceType.FILE, "requirements.md"));
        state.setRequirementDocument(new RequirementDocument("requirements.md", "REQ-001"));
        state.setNormalizedRequirementBundle(new NormalizedRequirementBundle("requirements.md", List.of(), List.of(), List.of()));
        state.setMappedUiKnowledge(new MappedUiKnowledge(List.of(), List.of(), List.of(), List.of(), List.of()));
        state.addArtifact("run.key", "value");

        WorkflowPipelineSnapshot snapshot = WorkflowPipelineSnapshot.from(state);

        Assert.assertEquals(snapshot.runEnvelope().objective(), "Generate POM");
        Assert.assertEquals(snapshot.requirements().requirementDocument().source(), "requirements.md");
        Assert.assertEquals(snapshot.requirements().normalizedRequirementBundle().source(), "requirements.md");
        Assert.assertNotNull(snapshot.mapping().mappedUiKnowledge());
    }

    @Test
    public void runEnvelopeIsImmutableCopyOfArtifacts() {
        WorkflowState state = new WorkflowState("Generate POM", new RequirementInput(SourceType.FILE, "requirements.md"));
        state.addArtifact("before", "1");

        WorkflowPipelineSnapshot snapshot = WorkflowPipelineSnapshot.from(state);
        state.addArtifact("after", "2");

        Assert.assertTrue(snapshot.runEnvelope().artifacts().containsKey("before"));
        Assert.assertFalse(snapshot.runEnvelope().artifacts().containsKey("after"));
    }

    @Test
    public void snapshotIsJsonSerializableForQualityArtifacts() throws Exception {
        WorkflowState state = new WorkflowState("Generate POM", new RequirementInput(SourceType.FILE, "requirements.md"));
        state.addArtifact("before", "1");
        state.addFinding("finding");
        state.addAudit("audit");

        WorkflowPipelineSnapshot snapshot = WorkflowPipelineSnapshot.from(state);
        String json = objectMapper.writeValueAsString(snapshot);

        Assert.assertTrue(json.contains("\"runMetadata\""));
        Assert.assertTrue(json.contains("\"runId\""));
        Assert.assertTrue(json.contains("\"artifactRefs\""));
    }
}
