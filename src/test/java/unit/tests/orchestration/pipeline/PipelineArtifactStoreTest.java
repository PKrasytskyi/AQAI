package unit.tests.orchestration.pipeline;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;

import java.util.List;

class PipelineArtifactStoreTest {

    @Test
    void returnsATypeCheckedArtifactList() {
        PipelineArtifactStore store = store();
        store.put(WorkflowArtifact.WRITTEN_FILES, List.of("one", "two"));

        Assert.assertEquals(store.getList(WorkflowArtifact.WRITTEN_FILES, String.class), List.of("one", "two"));
    }

    @Test
    void rejectsAnArtifactListWithUnexpectedElementType() {
        PipelineArtifactStore store = store();
        store.put(WorkflowArtifact.WRITTEN_FILES, List.of("one"));

        Assert.expectThrows(
                ClassCastException.class,
                () -> store.getList(WorkflowArtifact.WRITTEN_FILES, Integer.class)
        );
    }

    private PipelineArtifactStore store() {
        return PipelineArtifactStore.from(new WorkflowState(
                "test",
                new RequirementInput(SourceType.FILE, "requirements.md")
        ));
    }
}
