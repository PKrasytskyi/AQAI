package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;

import java.util.List;

public class PomContractCompatibilityAdapter {

    private final DeterministicPomJavaWriter writer;

    public PomContractCompatibilityAdapter(String pagePackage) {
        this.writer = new DeterministicPomJavaWriter(pagePackage);
    }

    public AiPageObjectSpec toAiPageObjectSpec(PomContractSpec contract) {
        return writer.toAiPageObjectSpec(contract);
    }

    public List<AiPageObjectSpec> toAiPageObjectSpecs(List<PomContractSpec> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return List.of();
        }
        return contracts.stream().map(this::toAiPageObjectSpec).toList();
    }
}
