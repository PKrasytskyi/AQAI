package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceLifecycleResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;

import java.util.List;

public record SpaTargetedVerificationOutput(
        SpaTargetedVerificationResult verification,
        SpaEvidenceLifecycleResult lifecycle,
        List<String> writtenFiles
) {
    public SpaTargetedVerificationOutput {
        writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
    }
}
