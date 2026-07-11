package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class AiRunQualitySummaryWriter {

    private final AiRunQualitySummaryService summaryService;
    private final AiRunArtifactWriter artifactWriter;
    private final AiRunArtifactDiffWriter artifactDiffWriter;

    public AiRunQualitySummaryWriter() {
        this(new AiRunQualitySummaryService(), new AiRunArtifactWriter(), new AiRunArtifactDiffWriter());
    }

    AiRunQualitySummaryWriter(
            AiRunQualitySummaryService summaryService,
            AiRunArtifactWriter artifactWriter,
            AiRunArtifactDiffWriter artifactDiffWriter
    ) {
        if (summaryService == null) {
            throw new IllegalArgumentException("summaryService cannot be null");
        }
        if (artifactWriter == null) {
            throw new IllegalArgumentException("artifactWriter cannot be null");
        }
        if (artifactDiffWriter == null) {
            throw new IllegalArgumentException("artifactDiffWriter cannot be null");
        }
        this.summaryService = summaryService;
        this.artifactWriter = artifactWriter;
        this.artifactDiffWriter = artifactDiffWriter;
    }

    public AiRunQualitySummary write(WorkflowState state) {
        AiRunQualityArtifactResult result = write(
                state == null ? null : new AiRunQualitySummaryInput(
                        state.getKnowledgeRunMetadata(),
                        state.getNormalizedRequirementBundle(),
                        state.getCanonicalTestCaseBundle(),
                        state.getMappedUiKnowledge(),
                        state.getArtifacts()
                ),
                WorkflowPipelineSnapshot.from(state)
        );
        if (state != null) {
            result.artifactFiles().forEach(state::addAiArtifactFile);
            result.artifacts().forEach(state::addArtifact);
        }
        return result.summary();
    }

    public AiRunQualityArtifactResult write(
            AiRunQualitySummaryInput input,
            WorkflowPipelineSnapshot snapshot
    ) {
        AiRunQualitySummary summary = summaryService.summarize(input);
        List<String> artifactFiles = new ArrayList<>();
        artifactWriter.writeDebugJson("quality", "pipeline-snapshot.json", snapshot)
                .map(Path::toString)
                .ifPresent(artifactFiles::add);
        Path path = artifactWriter.writeJson("quality", "run-quality-summary.json", summary);
        Path compactJson = artifactWriter.writeJson("", "run-summary.json", compactSummary(summary));
        Path compactMarkdown = artifactWriter.writeText("", "run-summary.md", compactSummaryMarkdown(summary));
        AiRunArtifactDiffResult diffResult = artifactDiffWriter.write(summary);
        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.put("ai.run.quality.summary.file", path.toString());
        artifacts.put("ai.run.summary.file", compactJson.toString());
        artifacts.put("ai.run.summary.markdown.file", compactMarkdown.toString());
        artifacts.put("ai.run.quality.score", String.valueOf(summary.qualityScore()));
        artifacts.put("ai.run.quality.prompt.blocking.issues", String.valueOf(summary.promptBlockingIssues()));
        artifacts.put("ai.run.artifact.diff.regressions", String.valueOf(diffResult.report().regressions().size()));
        artifactFiles.add(path.toString());
        artifactFiles.add(compactJson.toString());
        artifactFiles.add(compactMarkdown.toString());
        artifactFiles.add(diffResult.artifactFile());
        return new AiRunQualityArtifactResult(
                summary,
                diffResult.report(),
                artifactFiles,
                artifacts
        );
    }

    private Map<String, Object> compactSummary(AiRunQualitySummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("runId", summary.runId());
        values.put("qualityScore", summary.qualityScore());
        values.put("requirements", summary.requirements());
        values.put("canonicalTestCases", summary.canonicalTestCases());
        values.put("mappedPages", summary.mappedPages());
        values.put("confirmedLocators", summary.confirmedLocators());
        values.put("candidateLocators", summary.candidateLocators());
        values.put("fallbackLocators", summary.fallbackLocators());
        values.put("lowConfidenceLocators", summary.lowConfidenceLocators());
        values.put("promptBlockingIssues", summary.promptBlockingIssues());
        values.put("expectedResultsNeedsReview", summary.expectedResultsNeedsReview());
        values.put("neo4jHit", summary.neo4jHit());
        values.put("qdrantHit", summary.qdrantHit());
        values.put("retrievalMode", summary.retrievalMode());
        values.put("stableCacheUsed", summary.stableCacheUsed());
        values.put("pageEnrichmentOpenAiAttempts", summary.pageEnrichmentOpenAiAttempts());
        values.put("pageEnrichmentOpenAiFailures", summary.pageEnrichmentOpenAiFailures());
        return values;
    }

    private String compactSummaryMarkdown(AiRunQualitySummary summary) {
        return """
                # AI Run Summary

                | Metric | Value |
                | --- | --- |
                | runId | %s |
                | qualityScore | %d |
                | requirements | %d |
                | canonicalTestCases | %d |
                | mappedPages | %d |
                | confirmedLocators | %d |
                | candidateLocators | %d |
                | fallbackLocators | %d |
                | promptBlockingIssues | %d |
                | expectedResultsNeedsReview | %d |
                | retrievalMode | %s |
                | stableCacheUsed | %s |
                | neo4jHit | %s |
                | qdrantHit | %s |
                """.formatted(
                summary.runId(),
                summary.qualityScore(),
                summary.requirements(),
                summary.canonicalTestCases(),
                summary.mappedPages(),
                summary.confirmedLocators(),
                summary.candidateLocators(),
                summary.fallbackLocators(),
                summary.promptBlockingIssues(),
                summary.expectedResultsNeedsReview(),
                summary.retrievalMode(),
                summary.stableCacheUsed(),
                summary.neo4jHit(),
                summary.qdrantHit()
        );
    }
}
