package ua.demo.agentlab.requirements.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.orchestration.*;
import ua.demo.agentlab.orchestration.pipeline.*;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import java.nio.file.*;
import java.util.List;
import java.util.Set;

/** Publishes structured behavior contracts for live SPA verification and review. */
public final class StructuredBehaviorContractAgent implements WorkflowAgent, PipelineAgent<NormalizedRequirementBundle,List<StructuredBehaviorContract>> {
    private final StructuredBehaviorContractBuilder builder=new StructuredBehaviorContractBuilder();
    @Override public String name(){return "structured-behavior-contract-agent";}
    @Override public Set<WorkflowArtifact> requires(){return Set.of(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);}
    @Override public Set<WorkflowArtifact> produces(){return Set.of(WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS);}
    @Override public WorkflowArtifact input(){return WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE;}
    @Override public WorkflowArtifact output(){return WorkflowArtifact.STRUCTURED_BEHAVIOR_CONTRACTS;}
    @Override public NormalizedRequirementBundle inputFrom(PipelineArtifactStore store,WorkflowState state){return store.require(WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);}
    @Override public boolean supports(NormalizedRequirementBundle input,WorkflowRunEnvelope run){return input!=null;}
    @Override public List<StructuredBehaviorContract> execute(NormalizedRequirementBundle input,WorkflowRunEnvelope run){return builder.build(input.requirements());}
    @Override public void applyOutput(List<StructuredBehaviorContract> output,WorkflowState state){
        try{Path path=Path.of("target","ai-run","requirements","structured-behavior-contracts.json");Files.createDirectories(path.getParent());new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(),output);if(state!=null)state.addArtifact("structured.behavior.contracts",path.toAbsolutePath().toString());}
        catch(Exception exception){throw new IllegalStateException("Failed to write structured behavior contracts",exception);}
    }
}
