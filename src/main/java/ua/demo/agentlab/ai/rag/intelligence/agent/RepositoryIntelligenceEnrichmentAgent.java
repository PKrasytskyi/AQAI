package ua.demo.agentlab.ai.rag.intelligence.agent;

import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRunReport;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryIntelligenceReport;
import ua.demo.agentlab.ai.rag.intelligence.service.KnowledgeEnrichmentRunReporter;
import ua.demo.agentlab.ai.rag.intelligence.service.RepositoryIntelligenceIndexer;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.nio.file.Path;
import java.util.Map;

public class RepositoryIntelligenceEnrichmentAgent implements WorkflowAgent {

    private final RepositoryIntelligenceIndexer indexer;
    private final Path workspaceRoot;
    private final Path outputDirectory;
    private final KnowledgeEnrichmentRunReporter runReporter;
    private final AiRunArtifactWriter artifactWriter = new AiRunArtifactWriter();

    public RepositoryIntelligenceEnrichmentAgent(
            RepositoryIntelligenceIndexer indexer,
            Path workspaceRoot,
            Path outputDirectory,
            KnowledgeEnrichmentRunReporter runReporter
    ) {
        if (indexer == null) {
            throw new IllegalArgumentException("indexer cannot be null");
        }
        this.indexer = indexer;
        this.workspaceRoot = workspaceRoot == null ? Path.of("").toAbsolutePath().normalize() : workspaceRoot;
        this.outputDirectory = outputDirectory == null
                ? Path.of("target", "ai-run", "repository-intelligence").toAbsolutePath().normalize()
                : outputDirectory;
        this.runReporter = runReporter;
    }

    @Override
    public String name() {
        return "repository-intelligence-enrichment-agent";
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state != null && !state.getArtifacts().containsKey("ai.enrichment.completed");
    }

    @Override
    public void execute(WorkflowState state) {
        RepositoryIntelligenceReport report = indexer.index(workspaceRoot, outputDirectory);
        KnowledgeEnrichmentRunReport enrichment = runReporter == null
                ? KnowledgeEnrichmentRunReport.notStarted("rule-based")
                : runReporter.lastRunReport();

        state.addArtifact("ai.enrichment.completed", "true");
        state.addArtifact("ai.enrichment.output", report.outputDirectory().toString());
        state.addArtifact("ai.enrichment.records", String.valueOf(report.knowledgeEnrichments()));
        state.addArtifact("ai.enrichment.source", enrichment.source());
        state.addArtifact("ai.enrichment.batches.requested", String.valueOf(enrichment.requestedBatches()));
        state.addArtifact("ai.enrichment.batches.completed", String.valueOf(enrichment.completedBatches()));
        state.addArtifact("ai.enrichment.records.openai", String.valueOf(enrichment.enrichedRecordCount()));
        state.addArtifact("ai.enrichment.failures", String.valueOf(enrichment.failures().size()));
        state.addFinding("Repository intelligence enrichment completed: "
                + enrichment.source() + " batches=" + enrichment.completedBatches()
                + "/" + enrichment.requestedBatches());
        state.addAiArtifactFile(artifactWriter.writeJson(
                "enrichment",
                "repository-intelligence-report.json",
                Map.of("repository", report, "enrichment", enrichment)
        ).toString());
    }
}
