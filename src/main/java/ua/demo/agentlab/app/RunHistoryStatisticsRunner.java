package ua.demo.agentlab.app;

import ua.demo.agentlab.artifactreuse.metrics.RunHistoryStatisticsReport;
import ua.demo.agentlab.artifactreuse.metrics.RunHistoryStatisticsReporter;

/** Rebuilds the single latest-runs table without starting a workflow. */
public final class RunHistoryStatisticsRunner {

    private RunHistoryStatisticsRunner() {
    }

    public static void main(String[] args) {
        int limit = args != null && args.length > 0 ? parseLimit(args[0]) : RunHistoryStatisticsReporter.DEFAULT_RUN_LIMIT;
        RunHistoryStatisticsReport report = new RunHistoryStatisticsReporter().writeLatestRuns(limit);
        System.out.println("Run history statistics written to " + report.markdownFile());
    }

    private static int parseLimit(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception ignored) {
            return RunHistoryStatisticsReporter.DEFAULT_RUN_LIMIT;
        }
    }
}
