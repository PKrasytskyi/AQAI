package ua.demo.agentlab.ai.rag.intelligence.agent;

import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRunReport;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryIntelligenceReport;

import java.util.List;
import java.util.Map;

public record RepositoryIntelligenceEnrichmentResult(
        RepositoryIntelligenceReport repositoryReport,
        KnowledgeEnrichmentRunReport enrichmentReport,
        Map<String, String> artifacts,
        List<String> artifactFiles,
        List<String> findings
) {
    public RepositoryIntelligenceEnrichmentResult {
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        artifactFiles = artifactFiles == null ? List.of() : List.copyOf(artifactFiles);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
