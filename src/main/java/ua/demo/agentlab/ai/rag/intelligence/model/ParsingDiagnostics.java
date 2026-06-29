package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record ParsingDiagnostics(
        int scannedFiles,
        int javaFiles,
        int yamlFiles,
        int jsonFiles,
        int markdownFiles,
        int featureFiles,
        int xmlFiles,
        IncrementalIndexStats incrementalIndexStats,
        List<ParserMetric> parserMetrics,
        List<ConfidenceAssessment> confidenceAssessments,
        List<IndexingWarning> warnings
) {
    public ParsingDiagnostics {
        incrementalIndexStats = incrementalIndexStats == null
                ? new IncrementalIndexStats(false, 0, 0, 0, 0)
                : incrementalIndexStats;
        parserMetrics = parserMetrics == null ? List.of() : List.copyOf(parserMetrics);
        confidenceAssessments = confidenceAssessments == null ? List.of() : List.copyOf(confidenceAssessments);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
