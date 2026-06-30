package ua.demo.agentlab.ui.discovery.agent;

import java.util.List;
import java.util.Map;

public record UiDiscoveryArtifactPersistenceResult(
        List<String> writtenFiles,
        Map<String, String> artifacts,
        List<String> findings
) {
    public UiDiscoveryArtifactPersistenceResult {
        writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
