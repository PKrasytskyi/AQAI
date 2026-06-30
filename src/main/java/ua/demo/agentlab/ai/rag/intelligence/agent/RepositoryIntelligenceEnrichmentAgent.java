package ua.demo.agentlab.ai.rag.intelligence.agent;

import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRunReport;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryIntelligenceReport;
import ua.demo.agentlab.ai.rag.intelligence.service.KnowledgeEnrichmentRunReporter;
import ua.demo.agentlab.ai.rag.intelligence.service.RepositoryIntelligenceIndexer;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepositoryIntelligenceEnrichmentAgent implements WorkflowAgent,
        PipelineAgent<ProjectProfile, RepositoryIntelligenceEnrichmentResult> {

    private final RepositoryIntelligenceIndexer indexer;
    private final Path workspaceRoot;
    private final Path outputDirectory;
    private final KnowledgeEnrichmentRunReporter runReporter;
    private final AiRunArtifactWriter artifactWriter = new AiRunArtifactWriter();
    private final StageOutputPublisher publisher = new StageOutputPublisher();

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
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.PROJECT_PROFILE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.REPOSITORY_INTELLIGENCE_ENRICHMENT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.PROJECT_PROFILE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.REPOSITORY_INTELLIGENCE_ENRICHMENT;
    }

    @Override
    public boolean supports(PipelineArtifactStore store, WorkflowState state) {
        return store != null
                && store.get(WorkflowArtifact.REPOSITORY_INTELLIGENCE_ENRICHMENT).isEmpty()
                && store.get(WorkflowArtifact.PROJECT_PROFILE).isPresent();
    }

    @Override
    public RepositoryIntelligenceEnrichmentResult execute(ProjectProfile input, WorkflowRunEnvelope run) {
        RepositoryIntelligenceReport report = indexer.index(workspaceRoot, outputDirectory);
        KnowledgeEnrichmentRunReport enrichment = runReporter == null
                ? KnowledgeEnrichmentRunReport.notStarted("rule-based")
                : runReporter.lastRunReport();

        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.put("ai.enrichment.completed", "true");
        artifacts.put("ai.enrichment.output", report.outputDirectory().toString());
        artifacts.put("ai.enrichment.records", String.valueOf(report.knowledgeEnrichments()));
        artifacts.put("ai.enrichment.source", enrichment.source());
        artifacts.put("ai.enrichment.batches.requested", String.valueOf(enrichment.requestedBatches()));
        artifacts.put("ai.enrichment.batches.completed", String.valueOf(enrichment.completedBatches()));
        artifacts.put("ai.enrichment.records.openai", String.valueOf(enrichment.enrichedRecordCount()));
        artifacts.put("ai.enrichment.failures", String.valueOf(enrichment.failures().size()));

        String artifactFile = artifactWriter.writeJson(
                "enrichment",
                "repository-intelligence-report.json",
                Map.of("repository", report, "enrichment", enrichment)
        ).toString();
        String finding = "Repository intelligence enrichment completed: "
                + enrichment.source() + " batches=" + enrichment.completedBatches()
                + "/" + enrichment.requestedBatches();
        return new RepositoryIntelligenceEnrichmentResult(
                report,
                enrichment,
                artifacts,
                List.of(artifactFile),
                List.of(finding)
        );
    }

    @Override
    public void applyOutput(RepositoryIntelligenceEnrichmentResult output, WorkflowState state) {
        publisher.publishRepositoryIntelligenceEnrichment(output, state);
    }
}
