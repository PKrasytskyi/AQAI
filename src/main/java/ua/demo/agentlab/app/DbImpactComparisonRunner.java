package ua.demo.agentlab.app;

import ua.demo.agentlab.ai.comparison.DbImpactComparisonReport;
import ua.demo.agentlab.ai.comparison.DbImpactComparisonReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class DbImpactComparisonRunner {

    private static final Path DEFAULT_HISTORY_ROOT = Path.of("target", "ai-run-history");
    private static final Path CURRENT_RUN_ROOT = Path.of("target", "ai-run");

    public static void main(String[] args) {
        Path firstRun;
        Path secondRun;
        if (args == null || args.length < 2) {
            List<Path> latestRuns = latestHistoryRuns();
            if (latestRuns.size() < 2) {
                System.out.println("Usage: DbImpactComparisonRunner [<without-db-run-dir> <with-db-run-dir>]");
                System.out.println("No arguments means: compare the latest two runs from "
                        + DEFAULT_HISTORY_ROOT + " plus " + CURRENT_RUN_ROOT);
                System.out.println("Found " + latestRuns.size() + " run directorie(s).");
                return;
            }
            firstRun = latestRuns.get(0);
            secondRun = latestRuns.get(1);
            System.out.println("Using latest two run directories:");
            System.out.println(" - " + firstRun);
            System.out.println(" - " + secondRun);
        } else {
            firstRun = Path.of(args[0]);
            secondRun = Path.of(args[1]);
        }

        DbImpactComparisonReport report = new DbImpactComparisonReporter().compare(firstRun, secondRun);
        System.out.println("DB impact comparison written to target/ai-run/comparison");
        printRun("Without DB", report.withoutDbRun(), report.withoutDb().dbUsageMode(),
                report.withoutDb().neo4jHit(), report.withoutDb().qdrantHit(),
                report.withoutDb().stableCacheUsed());
        printRun("With DB", report.withDbRun(), report.withDb().dbUsageMode(),
                report.withDb().neo4jHit(), report.withDb().qdrantHit(),
                report.withDb().stableCacheUsed());
    }

    private static void printRun(
            String label,
            String runId,
            String dbUsageMode,
            boolean neo4jHit,
            boolean qdrantHit,
            boolean stableCacheUsed
    ) {
        System.out.println(label + ": " + runId
                + " [" + dbUsageMode
                + ", neo4jHit=" + neo4jHit
                + ", qdrantHit=" + qdrantHit
                + ", stableCacheUsed=" + stableCacheUsed + "]");
    }

    private static List<Path> latestHistoryRuns() {
        if (!Files.isDirectory(DEFAULT_HISTORY_ROOT)) {
            return hasRunSummary(CURRENT_RUN_ROOT) ? List.of(CURRENT_RUN_ROOT) : List.of();
        }
        try (var stream = Files.list(DEFAULT_HISTORY_ROOT)) {
            List<Path> historyRuns = stream
                    .filter(Files::isDirectory)
                    .filter(DbImpactComparisonRunner::hasRunSummary)
                    .toList();
            java.util.ArrayList<Path> candidates = new java.util.ArrayList<>(historyRuns);
            if (hasRunSummary(CURRENT_RUN_ROOT)) {
                candidates.add(CURRENT_RUN_ROOT);
            }
            java.util.LinkedHashMap<String, Path> uniqueByRunId = new java.util.LinkedHashMap<>();
            candidates.stream()
                    .sorted(Comparator.comparing(DbImpactComparisonRunner::lastModified).reversed())
                    .forEach(path -> uniqueByRunId.putIfAbsent(runId(path), path));
            return uniqueByRunId.values().stream().limit(2).toList();
        } catch (IOException exception) {
            System.out.println("Failed to read " + DEFAULT_HISTORY_ROOT + ": " + exception.getMessage());
            return List.of();
        }
    }

    private static boolean hasRunSummary(Path path) {
        return Files.isRegularFile(path.resolve("quality").resolve("run-quality-summary.json"))
                || Files.isRegularFile(path.resolve("ai-run").resolve("quality").resolve("run-quality-summary.json"));
    }

    private static String runId(Path path) {
        Path summary = Files.isRegularFile(path.resolve("quality").resolve("run-quality-summary.json"))
                ? path.resolve("quality").resolve("run-quality-summary.json")
                : path.resolve("ai-run").resolve("quality").resolve("run-quality-summary.json");
        try {
            String content = Files.readString(summary);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"runId\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException ignored) {
            // Fall back to directory identity when the summary cannot be read.
        }
        return path.toAbsolutePath().normalize().toString();
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }
}
