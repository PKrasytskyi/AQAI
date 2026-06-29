package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Path snapshotPath = artifactWriter.writeJson("quality", "pipeline-snapshot.json", snapshot);
        Path path = artifactWriter.writeJson("quality", "run-quality-summary.json", summary);
        AiRunArtifactDiffResult diffResult = artifactDiffWriter.write(summary);
        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.put("ai.run.quality.summary.file", path.toString());
        artifacts.put("ai.run.quality.score", String.valueOf(summary.qualityScore()));
        artifacts.put("ai.run.quality.prompt.blocking.issues", String.valueOf(summary.promptBlockingIssues()));
        artifacts.put("ai.run.artifact.diff.regressions", String.valueOf(diffResult.report().regressions().size()));
        return new AiRunQualityArtifactResult(
                summary,
                diffResult.report(),
                List.of(snapshotPath.toString(), path.toString(), diffResult.artifactFile()),
                artifacts
        );
    }
}
