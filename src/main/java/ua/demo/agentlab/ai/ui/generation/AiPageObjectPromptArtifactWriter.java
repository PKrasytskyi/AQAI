package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.debug.AiPromptTraceSnapshot;
import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.ui.prompt.quality.PromptQualityReport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiPageObjectPromptArtifactWriter {

    private static final String STAGE = "page-object-spec";

    private final AiRunArtifactWriter artifactWriter;

    public AiPageObjectPromptArtifactWriter() {
        this(new AiRunArtifactWriter());
    }

    AiPageObjectPromptArtifactWriter(AiRunArtifactWriter artifactWriter) {
        if (artifactWriter == null) {
            throw new IllegalArgumentException("artifactWriter cannot be null");
        }
        this.artifactWriter = artifactWriter;
    }

    public AiPageObjectPromptArtifactResult write(
            AiPageObjectPromptDraft draft,
            PromptQualityReport qualityReport
    ) {
        if (draft == null || draft.scope() == null) {
            throw new IllegalArgumentException("prompt draft cannot be null");
        }
        List<String> files = new ArrayList<>();
        Map<String, String> artifacts = new LinkedHashMap<>();

        files.add(artifactWriter.writeJson(STAGE, draft.scope().fileStem() + "-scope-trace.json",
                draft.scope().scopeTrace()).toString());
        files.add(artifactWriter.writeJson(STAGE, draft.scope().fileStem() + "-prompt-quality-report.json",
                qualityReport).toString());
        Path promptPath = artifactWriter.writeText(STAGE, draft.scope().fileStem() + "-prompt.txt", draft.prompt());
        files.add(promptPath.toString());

        AiPromptTraceSnapshot traceSnapshot = new AiPromptTraceSnapshot(
                STAGE,
                "page:" + draft.scope().fileStem(),
                draft.scope().pageName(),
                "page-object-spec",
                promptPath.toString(),
                traceMetadata(draft)
        );
        files.add(artifactWriter.writeJson(STAGE, draft.scope().fileStem() + "-prompt-trace.json", traceSnapshot).toString());

        artifacts.put(
                "ai.page.object.prompt.quality." + draft.scope().fileStem() + ".blocking",
                String.valueOf(qualityReport.blockingIssueCount())
        );
        artifacts.put(
                "ai.page.object.prompt.quality." + draft.scope().fileStem() + ".warnings",
                String.valueOf(qualityReport.warningIssueCount())
        );
        return new AiPageObjectPromptArtifactResult(files, artifacts);
    }

    public String writeText(String fileName, String content) {
        return artifactWriter.writeText(STAGE, fileName, content).toString();
    }

    public String writeJson(String fileName, Object payload) {
        return artifactWriter.writeJson(STAGE, fileName, payload).toString();
    }

    private Map<String, Object> traceMetadata(AiPageObjectPromptDraft draft) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.putAll(draft.metadata());
        metadata.put("promptLength", draft.prompt().length());
        return metadata;
    }
}
