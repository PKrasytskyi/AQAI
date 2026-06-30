package ua.demo.agentlab.api.agent;

import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.quality.ApiQualityReport;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;
import java.util.Map;

public record ApiGenerationResult(
        ApiEndpointBundle endpoints,
        CanonicalApiTestCaseBundle testCases,
        ApiGenerationSpec generationSpec,
        ApiQualityReport qualityReport,
        List<GeneratedSourceFile> sourceFiles,
        Map<String, String> artifacts
) {
    public ApiGenerationResult {
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
    }
}
