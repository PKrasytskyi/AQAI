package ua.demo.agentlab.ai.comparison;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.token.OpenAiTokenCounter;
import ua.demo.agentlab.ai.token.TokenCountResult;
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
    private final OpenAiTokenCounter tokenCounter = new OpenAiTokenCounter(modelName());

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
            notes.add("Token usage uses local OpenAI tokenizer estimates when exact OpenAI usage artifacts are unavailable.");
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
        JsonNode enrichmentReport = readJson(aiRun.resolve("enrichment").resolve("page-model-enrichment-report.json"));
        EnrichmentLlmMetrics enrichmentLlmMetrics = enrichmentLlmMetrics(summary, enrichmentReport);
        int pomLlmCalls = promptFiles.size();
        int llmCalls = pomLlmCalls + enrichmentLlmMetrics.attempts();
        int llmSuccessfulCalls = Math.min(pomLlmCalls, pomContractCount(aiRun).total()) + enrichmentLlmMetrics.successes();
        int llmFailedCalls = Math.max(0, llmCalls - llmSuccessfulCalls);
        TokenUsageMetrics tokenUsage = tokenUsage(aiRun, promptFiles, responseFiles(aiRun), enrichmentLlmMetrics);
        int promptCountForAverage = llmCalls;
        int averagePromptSize = promptCountForAverage == 0 ? 0
                : Math.round((float) tokenUsage.promptTokens() / promptCountForAverage);
        PomContractCount contracts = pomContractCount(aiRun);
        int generatedFiles = generatedPageObjectCount(aiRun);
        String compileStatus = compileStatus(aiRun);
        int compileReady = compileReadyGeneratedCode(compileStatus, generatedFiles);
        int reusedKnowledge = reusedPageKnowledge(aiRun, summary);
        boolean neo4jHit = booleanValue(summary, "neo4jHit", false);
        boolean qdrantHit = booleanValue(summary, "qdrantHit", false);
        boolean stableCacheUsed = booleanValue(summary, "stableCacheUsed", false) || reusedKnowledge > 0;
        String retrievalMode = text(summary, "retrievalMode", "unknown");
        if (stableCacheUsed && "current-run".equals(retrievalMode)) {
            retrievalMode = "stable-page-cache";
        }
        String dbUsageMode = dbUsageMode(neo4jHit, qdrantHit, stableCacheUsed);
        return new DbImpactRunMetrics(
                text(summary, "runId", root.getFileName() == null ? root.toString() : root.getFileName().toString()),
                root.toString(),
                tokenUsage.totalTokens(),
                tokenUsage.promptTokens(),
                tokenUsage.responseTokens(),
                tokenUsage.actualTokens(),
                tokenUsage.estimated(),
                tokenUsage.countingMode(),
                tokenUsage.tokenizerModel(),
                llmCalls,
                llmSuccessfulCalls,
                llmFailedCalls,
                pomLlmCalls,
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
                compileStatus,
                artifactDiffSize(aiRun, discovery),
                neo4jHit,
                qdrantHit,
                stableCacheUsed,
                retrievalMode,
                dbUsageMode,
                integer(summary, "pageEnrichmentGenerated", 0),
                integer(summary, "pageEnrichmentCacheHits", 0),
                enrichmentLlmMetrics.attempts(),
                enrichmentLlmMetrics.attempts(),
                enrichmentLlmMetrics.successes(),
                enrichmentLlmMetrics.failures(),
                enrichmentLlmMetrics.fallbacks()
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
        rows.add(numberRow("Prompt tokens", withoutDb.promptTokens(), withDb.promptTokens(), true));
        rows.add(numberRow("Response tokens", withoutDb.responseTokens(), withDb.responseTokens(), true));
        rows.add(numberRow("Actual OpenAI tokens", withoutDb.actualTokens(), withDb.actualTokens(), true));
        rows.add(new DbImpactMetricRow("Token counting mode", withoutDb.tokenCountingMode(), withDb.tokenCountingMode(), ""));
        rows.add(numberRow("LLM calls", withoutDb.llmCalls(), withDb.llmCalls(), true));
        rows.add(numberRow("LLM successful calls", withoutDb.llmSuccessfulCalls(), withDb.llmSuccessfulCalls(), true));
        rows.add(numberRow("LLM failed calls", withoutDb.llmFailedCalls(), withDb.llmFailedCalls(), true));
        rows.add(numberRow("POM contract LLM calls", withoutDb.pomLlmCalls(), withDb.pomLlmCalls(), true));
        rows.add(numberRow("Avg prompt tokens", withoutDb.averagePromptSize(), withDb.averagePromptSize(), true));
        rows.add(numberRow("Reused page knowledge", withoutDb.reusedPageKnowledgeArtifacts(),
                withDb.reusedPageKnowledgeArtifacts(), false, " artifacts"));
        rows.add(numberRow("Page enrichment generated", withoutDb.pageEnrichmentGenerated(),
                withDb.pageEnrichmentGenerated(), true));
        rows.add(numberRow("Page enrichment cache hits", withoutDb.pageEnrichmentCacheHits(),
                withDb.pageEnrichmentCacheHits(), false));
        rows.add(numberRow("Page enrichment OpenAI calls", withoutDb.pageEnrichmentOpenAiCalls(),
                withDb.pageEnrichmentOpenAiCalls(), true));
        rows.add(numberRow("Page enrichment OpenAI successes", withoutDb.pageEnrichmentOpenAiSuccesses(),
                withDb.pageEnrichmentOpenAiSuccesses(), true));
        rows.add(numberRow("Page enrichment OpenAI failures", withoutDb.pageEnrichmentOpenAiFailures(),
                withDb.pageEnrichmentOpenAiFailures(), true));
        rows.add(numberRow("Page enrichment fallbacks", withoutDb.pageEnrichmentOpenAiFallbacks(),
                withDb.pageEnrichmentOpenAiFallbacks(), true));
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
        rows.add(new DbImpactMetricRow("Compile status", withoutDb.compileStatus(), withDb.compileStatus(), ""));
        rows.add(new DbImpactMetricRow(
                "Compile-ready generated code",
                compileReadyDisplay(withoutDb),
                compileReadyDisplay(withDb),
                "passed".equals(withoutDb.compileStatus()) && "passed".equals(withDb.compileStatus())
                        ? signed(withDb.compileReadyGeneratedCode() - withoutDb.compileReadyGeneratedCode())
                        : ""
        ));
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
                integer(summary, "pageKnowledgeCacheHits", 0),
                integer(summary, "pageEnrichmentCacheHits", 0)
        );
        int enrichmentCacheHits = integer(readJson(aiRun.resolve("enrichment").resolve("page-model-enrichment-report.json")),
                "cacheHits", 0);
        return Math.max(hitEntries, Math.max(summaryHits, enrichmentCacheHits));
    }

    private PomContractCount pomContractCount(Path aiRun, boolean ignored) {
        List<Path> contracts = files(pageObjectArtifactRoot(aiRun)).stream()
                .filter(path -> path.getFileName().toString().endsWith("-pom-contract.json"))
                .toList();
        int valid = 0;
        for (Path path : contracts) {
            try {
                pomParser.parse(Files.readString(path));
                valid++;
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

    private Path pageObjectArtifactRoot(Path aiRun) {
        Path current = aiRun.resolve("page-objects");
        if (Files.isDirectory(current)) {
            return current;
        }
        return aiRun.resolve("page-object-spec");
    }

    private Path existingPath(Path primary, Path fallback) {
        if (primary != null && Files.exists(primary)) {
            return primary;
        }
        return fallback;
    }

    private String compileStatus(Path aiRun) {
        Path artifact = aiRun.resolve("validation").resolve("generated-ui-smoke-result.json");
        if (!Files.exists(artifact)) {
            return "missing-artifact";
        }
        JsonNode smoke = readJson(artifact);
        String status = text(smoke, "status", "");
        if ("PASSED".equalsIgnoreCase(status)) {
            return "passed";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "failed";
        }
        return status.isBlank() ? "unknown" : status.trim().toLowerCase(Locale.ROOT);
    }

    private int compileReadyGeneratedCode(String compileStatus, int generatedFiles) {
        if ("passed".equalsIgnoreCase(compileStatus)) {
            return generatedFiles;
        }
        return 0;
    }

    private String compileReadyDisplay(DbImpactRunMetrics metrics) {
        if ("missing-artifact".equals(metrics.compileStatus())) {
            return "missing-artifact";
        }
        return metrics.compileReadyGeneratedCode() + "/" + metrics.totalGeneratedCode();
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

    private TokenUsageMetrics tokenUsage(
            Path aiRun,
            List<Path> promptFiles,
            List<Path> responseFiles,
            EnrichmentLlmMetrics enrichment
    ) {
        JsonNode pomUsage = readJson(existingPath(
                aiRun.resolve("page-objects").resolve("pom-llm-token-usage.json"),
                aiRun.resolve("page-object-spec").resolve("pom-llm-token-usage.json")
        ));
        JsonNode enrichmentUsage = readJson(aiRun.resolve("enrichment").resolve("page-model-enrichment-report.json"));
        int actualInput = tokenField(pomUsage, "inputTokens", "input_tokens", "prompt_tokens")
                + tokenField(enrichmentUsage, "inputTokens", "input_tokens", "prompt_tokens");
        int actualOutput = tokenField(pomUsage, "outputTokens", "output_tokens", "completion_tokens")
                + tokenField(enrichmentUsage, "outputTokens", "output_tokens", "completion_tokens");
        int actualTotal = tokenTotal(pomUsage) + tokenTotal(enrichmentUsage);
        TokenCountAggregate promptEstimate = countFiles(promptFiles);
        TokenCountAggregate responseEstimate = countFiles(responseFiles);
        int promptTokens = actualInput > 0 ? actualInput : promptEstimate.tokens() + enrichment.inputTokenEstimate();
        int responseTokens = actualOutput > 0 ? actualOutput : responseEstimate.tokens() + enrichment.outputTokenEstimate();
        int totalTokens = actualTotal > 0 ? actualTotal : promptTokens + responseTokens;
        String mode = actualTotal > 0 ? "openai-usage" : countingMode(promptEstimate, responseEstimate, enrichment);
        String tokenizerModel = tokenizerModel(promptEstimate, responseEstimate);
        return new TokenUsageMetrics(
                totalTokens,
                promptTokens,
                responseTokens,
                actualTotal,
                actualTotal <= 0,
                mode,
                tokenizerModel
        );
    }

    private TokenCountAggregate countFiles(List<Path> paths) {
        int tokens = 0;
        String mode = "openai-tokenizer";
        String tokenizerModel = "unknown";
        for (Path path : paths) {
            TokenCountResult result = tokenCounter.count(readString(path));
            tokens += result.tokens();
            tokenizerModel = result.tokenizerModel();
            if (!"openai-tokenizer".equals(result.countingMode())) {
                mode = result.countingMode();
            }
        }
        return new TokenCountAggregate(tokens, mode, tokenizerModel);
    }

    private int tokenTotal(JsonNode node) {
        int total = tokenField(node, "totalTokens", "total_tokens", "tokenTotal", "tokens_total");
        if (total > 0) {
            return total;
        }
        int input = tokenField(node, "inputTokens", "input_tokens", "prompt_tokens");
        int output = tokenField(node, "outputTokens", "output_tokens", "completion_tokens");
        return input + output;
    }

    private int tokenField(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode()) {
            return 0;
        }
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isInt() || value.isLong()) {
                return Math.max(0, value.asInt());
            }
            if (value.isTextual()) {
                try {
                    return Math.max(0, Integer.parseInt(value.asText()));
                } catch (NumberFormatException ignored) {
                    // Continue to the next alias.
                }
            }
        }
        return 0;
    }

    private String countingMode(
            TokenCountAggregate promptTokens,
            TokenCountAggregate responseTokens,
            EnrichmentLlmMetrics enrichment
    ) {
        if (enrichment.usesCharEstimate()
                || "char-estimate-fallback".equals(promptTokens.mode())
                || "char-estimate-fallback".equals(responseTokens.mode())) {
            return "mixed-tokenizer-char-estimate";
        }
        return "openai-tokenizer-estimate";
    }

    private String tokenizerModel(TokenCountAggregate promptTokens, TokenCountAggregate responseTokens) {
        if (!"unknown".equals(promptTokens.tokenizerModel())) {
            return promptTokens.tokenizerModel();
        }
        return responseTokens.tokenizerModel();
    }

    private EnrichmentLlmMetrics enrichmentLlmMetrics(JsonNode summary, JsonNode enrichmentReport) {
        int summaryCalls = integer(summary, "pageEnrichmentOpenAiCalls", -1);
        int summaryAttempts = integer(summary, "pageEnrichmentOpenAiAttempts", -1);
        int summarySuccesses = integer(summary, "pageEnrichmentOpenAiSuccesses", -1);
        int summaryFailures = integer(summary, "pageEnrichmentOpenAiFailures", -1);
        int summaryFallbacks = integer(summary, "pageEnrichmentOpenAiFallbacks", -1);
        int reportAttempts = integer(enrichmentReport, "openAiAttempts", -1);
        int reportSuccesses = firstNonNegative(
                integer(enrichmentReport, "openAiSuccesses", -1),
                integer(enrichmentReport, "openAiRecords", -1)
        );
        int reportFailures = firstNonNegative(
                integer(enrichmentReport, "openAiFailures", -1),
                failureCount(enrichmentReport)
        );
        int successes = firstNonNegative(summarySuccesses, summaryCalls, reportSuccesses, 0);
        int attempts = firstNonNegative(summaryAttempts, reportAttempts, successes + reportFailures, successes);
        int failures = firstNonNegative(summaryFailures, reportFailures, Math.max(0, attempts - successes));
        int fallbacks = firstNonNegative(summaryFallbacks, integer(enrichmentReport, "openAiFallbacks", -1), failures);
        int promptChars = firstNonNegative(
                integer(summary, "pageEnrichmentOpenAiPromptChars", -1),
                integer(enrichmentReport, "promptChars", -1),
                0
        );
        int responseChars = firstNonNegative(
                integer(summary, "pageEnrichmentOpenAiResponseChars", -1),
                integer(enrichmentReport, "responseChars", -1),
                0
        );
        int inputTokens = firstNonNegative(
                integer(summary, "pageEnrichmentOpenAiInputTokens", -1),
                integer(enrichmentReport, "inputTokens", -1),
                0
        );
        int outputTokens = firstNonNegative(
                integer(summary, "pageEnrichmentOpenAiOutputTokens", -1),
                integer(enrichmentReport, "outputTokens", -1),
                0
        );
        int totalTokens = firstNonNegative(
                integer(summary, "pageEnrichmentOpenAiTotalTokens", -1),
                integer(enrichmentReport, "totalTokens", -1),
                0
        );
        return new EnrichmentLlmMetrics(
                attempts,
                successes,
                failures,
                fallbacks,
                promptChars,
                responseChars,
                inputTokens,
                outputTokens,
                totalTokens
        );
    }

    private int failureCount(JsonNode report) {
        JsonNode failures = report == null ? null : report.path("failures");
        return failures != null && failures.isArray() ? failures.size() : -1;
    }

    private int firstNonNegative(int... values) {
        for (int value : values) {
            if (value >= 0) {
                return value;
            }
        }
        return 0;
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

    private static String modelName() {
        String systemProperty = System.getProperty("openai.model");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty.trim();
        }
        String env = System.getenv("OPENAI_MODEL");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return "gpt-5-mini";
    }

    private record PomContractCount(int valid, int total) {
    }

    private record TokenUsageMetrics(
            int totalTokens,
            int promptTokens,
            int responseTokens,
            int actualTokens,
            boolean estimated,
            String countingMode,
            String tokenizerModel
    ) {
        private TokenUsageMetrics {
            totalTokens = Math.max(0, totalTokens);
            promptTokens = Math.max(0, promptTokens);
            responseTokens = Math.max(0, responseTokens);
            actualTokens = Math.max(0, actualTokens);
            countingMode = countingMode == null || countingMode.isBlank() ? "unknown" : countingMode.trim();
            tokenizerModel = tokenizerModel == null || tokenizerModel.isBlank() ? "unknown" : tokenizerModel.trim();
        }
    }

    private record TokenCountAggregate(
            int tokens,
            String mode,
            String tokenizerModel
    ) {
        private TokenCountAggregate {
            tokens = Math.max(0, tokens);
            mode = mode == null || mode.isBlank() ? "unknown" : mode.trim();
            tokenizerModel = tokenizerModel == null || tokenizerModel.isBlank() ? "unknown" : tokenizerModel.trim();
        }
    }

    private record EnrichmentLlmMetrics(
            int attempts,
            int successes,
            int failures,
            int fallbacks,
            int promptChars,
            int responseChars,
            int inputTokens,
            int outputTokens,
            int totalTokens
    ) {
        private EnrichmentLlmMetrics {
            attempts = Math.max(0, attempts);
            successes = Math.max(0, successes);
            failures = Math.max(0, failures);
            fallbacks = Math.max(0, fallbacks);
            promptChars = Math.max(0, promptChars);
            responseChars = Math.max(0, responseChars);
            inputTokens = Math.max(0, inputTokens);
            outputTokens = Math.max(0, outputTokens);
            totalTokens = Math.max(0, totalTokens);
        }

        private int inputTokenEstimate() {
            if (inputTokens > 0) {
                return inputTokens;
            }
            return (int) Math.ceil(promptChars / 4.0d);
        }

        private int outputTokenEstimate() {
            if (outputTokens > 0) {
                return outputTokens;
            }
            return (int) Math.ceil(responseChars / 4.0d);
        }

        private boolean usesCharEstimate() {
            return (inputTokens <= 0 && promptChars > 0) || (outputTokens <= 0 && responseChars > 0);
        }
    }
}
