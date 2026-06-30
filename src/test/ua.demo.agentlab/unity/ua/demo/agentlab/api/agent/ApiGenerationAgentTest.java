package ua.demo.agentlab.api.agent;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;

import java.util.ArrayList;
import java.util.List;

public class ApiGenerationAgentTest {

    @Test
    public void preparesApiGenerationArtifactsFromConfiguredEndpointSeed() {
        WorkflowState state = new WorkflowState(
                "Generate API automation preview",
                new RequirementInput(SourceType.FILE, "requirements/api.md")
        );

        ApiGenerationAgent agent = new ApiGenerationAgent();
        ApiGenerationResult result = agent.execute(
                agent.inputFrom(PipelineArtifactStore.from(state), state),
                WorkflowRunEnvelope.from(state)
        );
        agent.applyOutput(result, state);

        Assert.assertEquals(state.getArtifacts().get("api.generation.completed"), "true");
        Assert.assertEquals(state.getArtifacts().get("api.generated.source.persistable"), "true");
        Assert.assertTrue(Integer.parseInt(state.getArtifacts().get("api.endpoint.count")) > 0);
        Assert.assertTrue(Integer.parseInt(state.getArtifacts().get("api.client.spec.count")) > 0);
        Assert.assertTrue(Integer.parseInt(state.getArtifacts().get("api.crud.spec.count")) > 0);
        Assert.assertTrue(Integer.parseInt(state.getArtifacts().get("api.source.preview.count")) > 0);
        Assert.assertEquals(state.getArtifacts().get("api.quality.blocking.count"), "0");
        Assert.assertTrue(state.getApiSourceFiles().stream()
                .allMatch(file -> file.relativePath().startsWith("src/main/java/")));
        Assert.assertTrue(state.getApiTestFiles().stream()
                .allMatch(file -> file.relativePath().startsWith("src/test/java/")));
        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("api-endpoint-bundle.json")));
        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("api-generation-spec.json")));
        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("preview-UserClient.java")));
        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("preview-UserCrudApiTest.java")));
    }

    @Test
    public void persistsApiSourcesAfterQualityGatePasses() {
        WorkflowState state = new WorkflowState(
                "Generate API automation preview",
                new RequirementInput(SourceType.FILE, "requirements/api.md")
        );
        ApiGenerationAgent generationAgent = new ApiGenerationAgent();
        ApiGenerationResult result = generationAgent.execute(
                generationAgent.inputFrom(PipelineArtifactStore.from(state), state),
                WorkflowRunEnvelope.from(state)
        );
        generationAgent.applyOutput(result, state);
        List<String> written = new ArrayList<>();

        ApiGeneratedSourcePersistenceAgent persistenceAgent =
                new ApiGeneratedSourcePersistenceAgent(file -> written.add(file.relativePath()));
        List<String> persisted = persistenceAgent.execute(result, WorkflowRunEnvelope.from(state));
        persistenceAgent.applyOutput(persisted, state);

        Assert.assertTrue(written.stream()
                .anyMatch(path -> path.equals("src/main/java/ua/demo/agentlab/api/generated/clients/UserClient.java")));
        Assert.assertTrue(written.stream()
                .anyMatch(path -> path.equals("src/main/java/ua/demo/agentlab/api/generated/models/request/CreateUserRequest.java")));
        Assert.assertTrue(written.stream()
                .anyMatch(path -> path.startsWith("src/test/java/ua/demo/agentlab/api/generated/tests/")));
        Assert.assertEquals(state.getArtifacts().get("api.generated.source.file.written"), String.valueOf(written.size()));
        Assert.assertEquals(state.getArtifacts().get("api.generated.source.persisted"), "true");
    }
}
