package ua.demo.agentlab.ai.comparison;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.ui.contract.PomContractQualityGate;
import ua.demo.agentlab.ai.ui.parser.PomContractSpecParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class DbImpactComparisonReporter {

    private static final Path DEFAULT_OUTPUT = Path.of("target", "ai-run", "comparison");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PomContractSpecParser pomParser = new PomContractSpecParser();
    private final PomContractQualityGate pomQualityGate = new PomContractQualityGate();

    public DbImpactComparisonReport compare(Path withoutDbRun, Path withDbRun) {
        return compare(withoutDbRun, withDbRun, DEFAULT_OUTPUT);
    }

    public DbImpactComparisonReport compare(Path withoutDbRun, Path withDbRun, Path outputDirectory) {
        DbImpactRunMetrics first = collect(withoutDbRun);
        DbImpactRunMetrics second = collect(withDbRun);
        DbImpactRunMetrics withoutDb = dbReadinessScore(first) <= dbReadinessScore(second) ? first : second;
        DbImpactRunMetrics withDb = withoutDb == first ? second : first;
        List<String> notes = new ArrayList<>();
        if (withoutDb.tokenUsageEstimated() || withDb.tokenUsageEstimated()) {
            notes.add("Token usage is estimated from prompt/response text because no exact LLM usage artifact was found.");
        }
        if (!withoutDb.withoutDbRun()) {
            notes.add("Without DB run is not a clean DB-disabled run; dbUsageMode=" + withoutDb.dbUsageMode() + ".");
        }
        if (!withDb.fullDbRun()) {
            notes.add("With DB run is not a full stable-cache DB run; dbUsageMode=" + withDb.dbUsageMode() + ".");
        }
        DbImpactComparisonReport report = new DbImpactComparisonReport(
                Instant.now().toString(),
                withoutDb.runId(),
                withDb.runId(),
                withoutDb,
                withDb,
                rows(withoutDb, withDb),
                notes
        );
        write(report, outputDirectory);
        return report;
    }

    public void write(DbImpactComparisonReport report, Path outputDirectory) {
        Path output = outputDirectory == null ? DEFAULT_OUTPUT : outputDirectory;
        try {
            Files.createDirectories(output);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(output.resolve("db-impact-summary.json").toFile(), report);
            Files.writeString(output.resolve("db-impact-summary.md"), markdown(report), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write DB impact comparison report", exception);
        }
    }

    private DbImpactRunMetrics collect(Path runDirectory) {
        Path root = runDirectory == null ? Path.of("target", "ai-run") : runDirectory;
        Path aiRun = aiRunRoot(root);
        Path discovery = discoveryRoot(root);
        JsonNode summary = readJson(aiRun.resolve("quality").resolve("run-quality-summary.json"));
        List<Path> promptFiles = files(aiRun)
                .stream()
                .filter(this::isPromptFile)
                .toList();
        int exactTokens = exactTokenUsage(aiRun);
        int estimatedTokens = estimateTokens(promptFiles, responseFiles(aiRun));
        int tokenUsage = exactTokens > 0 ? exactTokens : estimatedTokens;
        boolean tokenEstimated = exactTokens <= 0;
        int promptChars = promptFiles.stream().mapToInt(this::fileLength).sum();
        int llmCalls = promptFiles.size();
        int averagePromptSize = llmCalls == 0 ? 0 : Math.round((float) promptChars / llmCalls);
        PomContractCount contracts = pomContractCount(aiRun);
        int generatedFiles = generatedPageObjectCount(aiRun);
        int compileReady = compileReadyGeneratedCode(aiRun, generatedFiles);
        int reusedKnowledge = reusedPageKnowledge(aiRun, summary);
        boolean neo4jHit = booleanValue(summary, "neo4jHit", false);
        boolean qdrantHit = booleanValue(summary, "qdrantHit", false);
        boolean stableCacheUsed = booleanValue(summary, "stableCacheUsed", false) || reusedKnowledge > 0;
        String retrievalMode = text(summary, "retrievalMode", "unknown");
        String dbUsageMode = dbUsageMode(neo4jHit, qdrantHit, stableCacheUsed);
        return new DbImpactRunMetrics(
                text(summary, "runId", root.getFileName() == null ? root.toString() : root.getFileName().toString()),
                root.toString(),
                tokenUsage,
                tokenEstimated,
                llmCalls,
                averagePromptSize,
                reusedKnowledge,
                repeatedContextFragments(promptFiles),
                integer(summary, "qualityScore", 0),
                integer(summary, "lowConfidenceLocators", 0),
                integer(summary, "expectedResultsNeedsReview", 0),
                integer(summary, "promptBlockingIssues", 0),
                contracts.valid(),
                contracts.total(),
                compileReady,
                Math.max(generatedFiles, contracts.total()),
                artifactDiffSize(aiRun, discovery),
                neo4jHit,
                qdrantHit,
                stableCacheUsed,
                retrievalMode,
                dbUsageMode,
                integer(summary, "pageEnrichmentGenerated", 0),
                integer(summary, "pageEnrichmentCacheHits", 0),
                integer(summary, "pageEnrichmentOpenAiCalls", 0)
        );
    }

    private List<DbImpactMetricRow> rows(DbImpactRunMetrics withoutDb, DbImpactRunMetrics withDb) {
        List<DbImpactMetricRow> rows = new ArrayList<>();
        rows.add(new DbImpactMetricRow("DB mode", withoutDb.dbUsageMode(), withDb.dbUsageMode(), ""));
        rows.add(new DbImpactMetricRow("Neo4j hit", String.valueOf(withoutDb.neo4jHit()),
                String.valueOf(withDb.neo4jHit()), booleanChange(withoutDb.neo4jHit(), withDb.neo4jHit())));
        rows.add(new DbImpactMetricRow("Qdrant hit", String.valueOf(withoutDb.qdrantHit()),
                String.valueOf(withDb.qdrantHit()), booleanChange(withoutDb.qdrantHit(), withDb.qdrantHit())));
        rows.add(new DbImpactMetricRow("Stable cache used", String.valueOf(withoutDb.stableCacheUsed()),
                String.valueOf(withDb.stableCacheUsed()), booleanChange(withoutDb.stableCacheUsed(), withDb.stableCacheUsed())));
        rows.add(new DbImpactMetricRow("Retrieval mode", withoutDb.retrievalMode(), withDb.retrievalMode(), ""));
        rows.add(numberRow("Total tokens", withoutDb.totalTokens(), withDb.totalTokens(), true));
        rows.add(numberRow("LLM calls", withoutDb.llmCalls(), withDb.llmCalls(), true));
        rows.add(numberRow("Avg prompt size", withoutDb.averagePromptSize(), withDb.averagePromptSize(), true));
        rows.add(numberRow("Reused page knowledge", withoutDb.reusedPageKnowledgeArtifacts(),
                withDb.reusedPageKnowledgeArtifacts(), false, " artifacts"));
        rows.add(numberRow("Page enrichment generated", withoutDb.pageEnrichmentGenerated(),
                withDb.pageEnrichmentGenerated(), true));
        rows.add(numberRow("Page enrichment cache hits", withoutDb.pageEnrichmentCacheHits(),
                withDb.pageEnrichmentCacheHits(), false));
        rows.add(numberRow("Page enrichment OpenAI calls", withoutDb.pageEnrichmentOpenAiCalls(),
                withDb.pageEnrichmentOpenAiCalls(), true));
        rows.add(numberRow("Repeated context fragments", withoutDb.repeatedContextFragments(),
                withDb.repeatedContextFragments(), true));
        rows.add(new DbImpactMetricRow(
                "Quality score",
                withoutDb.qualityScore() + "/100",
                withDb.qualityScore() + "/100",
                signed(withDb.qualityScore() - withoutDb.qualityScore())
        ));
        rows.add(numberRow("Low-confidence locators", withoutDb.lowConfidenceLocators(), withDb.lowConfidenceLocators(), true));
        rows.add(numberRow("Expected results for review", withoutDb.expectedResultsForReview(),
                withDb.expectedResultsForReview(), true));
        rows.add(numberRow("Prompt-safety blocks", withoutDb.promptSafetyBlocks(), withDb.promptSafetyBlocks(), true));
        rows.add(ratioRow("Valid POM contracts", withoutDb.validPomContracts(), withoutDb.totalPomContracts(),
                withDb.validPomContracts(), withDb.totalPomContracts()));
        rows.add(ratioRow("Compile-ready generated code", withoutDb.compileReadyGeneratedCode(),
                withoutDb.totalGeneratedCode(), withDb.compileReadyGeneratedCode(), withDb.totalGeneratedCode()));
        rows.add(numberRow("Artifact diff size", withoutDb.artifactDiffSize(), withDb.artifactDiffSize(), true));
        return List.copyOf(rows);
    }

    private int dbReadinessScore(DbImpactRunMetrics metrics) {
        int score = 0;
        if (metrics.neo4jHit()) {
            score++;
        }
        if (metrics.qdrantHit()) {
            score++;
        }
        if (metrics.stableCacheUsed()) {
            score++;
        }
        return score;
    }

    private String dbUsageMode(boolean neo4jHit, boolean qdrantHit, boolean stableCacheUsed) {
        if (neo4jHit && qdrantHit && stableCacheUsed) {
            return "with-db";
        }
        if (!neo4jHit && !qdrantHit && !stableCacheUsed) {
            return "without-db";
        }
        return "partial-db";
    }

    private String booleanChange(boolean before, boolean after) {
        if (before == after) {
            return "0";
        }
        return after ? "+true" : "-true";
    }

    private DbImpactMetricRow numberRow(String metric, int withoutDb, int withDb, boolean percentChange) {
        return numberRow(metric, withoutDb, withDb, percentChange, "");
    }

    private DbImpactMetricRow numberRow(String metric, int withoutDb, int withDb, boolean percentChange, String suffix) {
        String change = percentChange
                ? percentChange(withoutDb, withDb)
                : signed(withDb - withoutDb);
        return new DbImpactMetricRow(
                metric,
                format(withoutDb) + suffix,
                format(withDb) + suffix,
                change
        );
    }

    private DbImpactMetricRow ratioRow(String metric, int withoutValid, int withoutTotal, int withValid, int withTotal) {
        return new DbImpactMetricRow(
                metric,
                withoutValid + "/" + withoutTotal,
                withValid + "/" + withTotal,
                signed(withValid - withoutValid)
        );
    }

    private String markdown(DbImpactComparisonReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("# DB Impact Summary").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| Metric | Without DB | With DB | Change |").append(System.lineSeparator());
        builder.append("|---|---:|---:|---:|").append(System.lineSeparator());
        for (DbImpactMetricRow row : report.rows()) {
            builder.append("| ")
                    .append(escapeMarkdown(row.metric()))
                    .append(" | ")
                    .append(escapeMarkdown(row.withoutDb()))
                    .append(" | ")
                    .append(escapeMarkdown(row.withDb()))
                    .append(" | ")
                    .append(escapeMarkdown(row.change()))
                    .append(" |")
                    .append(System.lineSeparator());
        }
        if (!report.notes().isEmpty()) {
            builder.append(System.lineSeparator()).append("Notes:").append(System.lineSeparator());
            for (String note : report.notes()) {
                builder.append("- ").append(note).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private int reusedPageKnowledge(Path aiRun, JsonNode summary) {
        JsonNode cacheLookup = readJson(aiRun.resolve("enrichment").resolve("page-knowledge-cache-lookup.json"));
        int hitEntries = 0;
        for (JsonNode entry : cacheLookup.path("entries")) {
            if (entry.path("hit").asBoolean(false)) {
                hitEntries++;
            }
        }
        int summaryHits = Math.max(
                Math.max(
                        integer(summary, "pageKnowledgeCacheHits", 0),
                        integer(summary, "pageEnrichmentCacheHits", 0)
                ),
                integer(readJson(aiRun.resolve("quality").resolve("pipeline-snapshot.json")), "page.knowledge.cache.hit.count", 0)
        );
        int enrichmentCacheHits = integer(readJson(aiRun.resolve("enrichment").resolve("page-model-enrichment-report.json")),
                "cacheHits", 0);
        return Math.max(hitEntries, Math.max(summaryHits, enrichmentCacheHits));
    }

    private PomContractCount pomContractCount(Path aiRun, boolean ignored) {
        List<Path> contracts = files(aiRun.resolve("page-object-spec")).stream()
                .filter(path -> path.getFileName().toString().endsWith("-pom-contract.json"))
                .toList();
        int valid = 0;
        for (Path path : contracts) {
            try {
                if (!pomQualityGate.validate(pomParser.parse(Files.readString(path))).hasBlockingIssues()) {
                    valid++;
                }
            } catch (Exception ignoredException) {
                // Invalid contracts are counted through total-valid delta.
            }
        }
        return new PomContractCount(valid, contracts.size());
    }

    private PomContractCount pomContractCount(Path aiRun) {
        return pomContractCount(aiRun, true);
    }

    private int generatedPageObjectCount(Path aiRun) {
        JsonNode smoke = readJson(aiRun.resolve("validation").resolve("generated-ui-smoke-result.json"));
        int filesChecked = integer(smoke, "filesChecked", -1);
        if (filesChecked >= 0) {
            return filesChecked;
        }
        return files(aiRun.resolve("generated")).stream()
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .toList()
                .size();
    }

    private int compileReadyGeneratedCode(Path aiRun, int generatedFiles) {
        JsonNode smoke = readJson(aiRun.resolve("validation").resolve("generated-ui-smoke-result.json"));
        String status = text(smoke, "status", "");
        if ("PASSED".equalsIgnoreCase(status)) {
            return generatedFiles;
        }
        return 0;
    }

    private int artifactDiffSize(Path aiRun, Path discovery) {
        JsonNode diff = readJson(aiRun.resolve("quality").resolve("artifact-diff.json"));
        int changedCategories = 0;
        for (JsonNode category : diff.path("categories")) {
            String status = category.path("status").asText("");
            if ("changed".equalsIgnoreCase(status) || "new".equalsIgnoreCase(status) || "removed".equalsIgnoreCase(status)) {
                changedCategories++;
            }
        }
        if (changedCategories > 0) {
            return changedCategories;
        }
        return files(aiRun).size() + files(discovery).size();
    }

    private int exactTokenUsage(Path aiRun) {
        int total = 0;
        for (Path path : files(aiRun)) {
            if (!path.getFileName().toString().endsWith(".json")) {
                continue;
            }
            total += sumTokenFields(readJson(path));
        }
        return total;
    }

    private int sumTokenFields(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        if (node.isObject()) {
            int total = 0;
            var fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey().toLowerCase(Locale.ROOT);
                if (entry.getValue().canConvertToInt()
                        && Set.of("totaltokens", "total_tokens", "tokentotal", "tokens_total").contains(key)) {
                    total += entry.getValue().asInt();
                } else {
                    total += sumTokenFields(entry.getValue());
                }
            }
            return total;
        }
        if (node.isArray()) {
            int total = 0;
            for (JsonNode child : node) {
                total += sumTokenFields(child);
            }
            return total;
        }
        return 0;
    }

    private int estimateTokens(List<Path> promptFiles, List<Path> responseFiles) {
        int chars = 0;
        for (Path path : promptFiles) {
            chars += fileLength(path);
        }
        for (Path path : responseFiles) {
            chars += fileLength(path);
        }
        return (int) Math.ceil(chars / 4.0d);
    }

    private List<Path> responseFiles(Path aiRun) {
        return files(aiRun).stream()
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    return name.endsWith(".txt") && name.contains("response");
                })
                .toList();
    }

    private boolean isPromptFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith("-prompt.txt") || name.endsWith("prompt.txt");
    }

    private int repeatedContextFragments(List<Path> promptFiles) {
        Map<String, Integer> counts = new HashMap<>();
        for (Path path : promptFiles) {
            Set<String> seenInFile = new HashSet<>();
            for (String line : readString(path).lines().toList()) {
                String normalized = line.trim().replaceAll("\\s+", " ");
                if (normalized.length() >= 40) {
                    seenInFile.add(normalized);
                }
            }
            for (String line : seenInFile) {
                counts.merge(line, 1, Integer::sum);
            }
        }
        return counts.values().stream()
                .filter(count -> count > 1)
                .mapToInt(count -> count - 1)
                .sum();
    }

    private Path aiRunRoot(Path root) {
        if (Files.exists(root.resolve("quality").resolve("run-quality-summary.json"))) {
            return root;
        }
        if (Files.isDirectory(root.resolve("ai-run"))) {
            return root.resolve("ai-run");
        }
        return root;
    }

    private Path discoveryRoot(Path root) {
        if (Files.isDirectory(root.resolve("discovery"))) {
            return root.resolve("discovery");
        }
        Path parentDiscovery = root.getParent() == null ? null : root.getParent().resolve("discovery");
        if (parentDiscovery != null && Files.isDirectory(parentDiscovery)) {
            return parentDiscovery;
        }
        return root.resolve("discovery");
    }

    private JsonNode readJson(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return objectMapper.missingNode();
        }
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            return objectMapper.missingNode();
        }
    }

    private List<Path> files(Path root) {
        if (root == null || !Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private int fileLength(Path path) {
        return readString(path).length();
    }

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            return "";
        }
    }

    private int integer(JsonNode node, String field, int defaultValue) {
        if (node == null || node.isMissingNode()) {
            return defaultValue;
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode()) {
            return defaultValue;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        try {
            return Integer.parseInt(value.asText());
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    private String text(JsonNode node, String field, String defaultValue) {
        if (node == null || node.isMissingNode()) {
            return defaultValue;
        }
        String value = node.path(field).asText("");
        return value.isBlank() ? defaultValue : value;
    }

    private boolean booleanValue(JsonNode node, String field, boolean defaultValue) {
        if (node == null || node.isMissingNode()) {
            return defaultValue;
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() ? defaultValue : value.asBoolean(defaultValue);
    }

    private String percentChange(int base, int current) {
        if (base == 0) {
            return current == 0 ? "0%" : signed(current);
        }
        double percent = (current - base) * 100.0d / base;
        return (percent > 0 ? "+" : "") + Math.round(percent) + "%";
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private String format(int value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|");
    }

    private record PomContractCount(int valid, int total) {
    }
}
