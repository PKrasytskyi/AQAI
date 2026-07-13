package ua.demo.agentlab.artifactreuse.metrics;

import java.nio.file.Path;
import java.util.List;

public record RunHistoryStatisticsReport(
        int requestedRunCount,
        List<RunHistoryStatisticsRow> runs,
        Path markdownFile
) {
    public RunHistoryStatisticsReport {
        requestedRunCount = Math.max(1, requestedRunCount);
        runs = runs == null ? List.of() : List.copyOf(runs);
    }
}
