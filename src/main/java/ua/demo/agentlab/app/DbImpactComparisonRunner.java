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
            List<Path> historyRuns = latestHistoryRuns();
            firstRun = historyRuns.stream().filter(DbImpactComparisonRunner::isCleanDbDisabledRun).findFirst().orElse(null);
            secondRun = historyRuns.stream().filter(DbImpactComparisonRunner::isFullDbEnabledRun).findFirst().orElse(null);
            if (firstRun == null || secondRun == null) {
                System.out.println("Usage: DbImpactComparisonRunner [<without-db-run-dir> <with-db-run-dir>]");
                System.out.println("No arguments requires the latest clean DB-disabled run and full DB-enabled stable-cache run.");
                System.out.println("Found " + historyRuns.size() + " run directorie(s), but no valid comparison pair.");
                return;
            }
            System.out.println("Using latest valid DB comparison pair:");
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
            return List.copyOf(uniqueByRunId.values());
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

    private static boolean isCleanDbDisabledRun(Path path) {
        return dbSignals(path, false, false, false);
    }

    private static boolean isFullDbEnabledRun(Path path) {
        return dbSignals(path, true, true, true);
    }

    private static boolean dbSignals(Path path, boolean neo4j, boolean qdrant, boolean stableCache) {
        Path summary = Files.isRegularFile(path.resolve("quality").resolve("run-quality-summary.json"))
                ? path.resolve("quality").resolve("run-quality-summary.json")
                : path.resolve("ai-run").resolve("quality").resolve("run-quality-summary.json");
        try {
            String content = Files.readString(summary);
            return booleanField(content, "neo4jHit") == neo4j
                    && booleanField(content, "qdrantHit") == qdrant
                    && booleanField(content, "stableCacheUsed") == stableCache;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean booleanField(String content, String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + java.util.regex.Pattern.quote(name) + "\\\"\\s*:\\s*(true|false)",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(content == null ? "" : content);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }
}
