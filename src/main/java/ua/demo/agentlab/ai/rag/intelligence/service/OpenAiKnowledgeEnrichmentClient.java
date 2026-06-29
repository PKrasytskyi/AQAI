package ua.demo.agentlab.ai.rag.intelligence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRunReport;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiKnowledgeEnrichmentClient implements KnowledgeEnrichmentClient, KnowledgeEnrichmentRunReporter {

    private final KnowledgeEnrichmentClient baselineClient;
    private final OpenAiResponseGenerationClient generationClient;
    private final ObjectMapper objectMapper;
    private final int maxLlmRecords;
    private final int batchSize;
    private KnowledgeEnrichmentRunReport lastRunReport = KnowledgeEnrichmentRunReport.notStarted("openai");

    public OpenAiKnowledgeEnrichmentClient(RagRuntimeConfig config) {
        this(config, new RuleBasedKnowledgeEnrichmentClient(), 80, 5);
    }

    public OpenAiKnowledgeEnrichmentClient(
            RagRuntimeConfig config,
            KnowledgeEnrichmentClient baselineClient,
            int maxLlmRecords,
            int batchSize
    ) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.baselineClient = baselineClient == null ? new RuleBasedKnowledgeEnrichmentClient() : baselineClient;
        this.generationClient = new OpenAiResponseGenerationClient(config);
        this.objectMapper = new ObjectMapper();
        this.maxLlmRecords = Math.max(1, maxLlmRecords);
        this.batchSize = Math.max(1, Math.min(10, batchSize));
    }

    @Override
    public List<KnowledgeEnrichmentRecord> enrich(KnowledgeEnrichmentRequest request) {
        List<KnowledgeEnrichmentRecord> baselineRecords = baselineClient.enrich(request);
        if (baselineRecords.isEmpty()) {
            lastRunReport = new KnowledgeEnrichmentRunReport("openai", 0, 0, 0, 0, List.of());
            return baselineRecords;
        }

        Map<String, KnowledgeEnrichmentRecord> enrichedById = new LinkedHashMap<>();
        baselineRecords.forEach(record -> enrichedById.put(record.id(), record));
        List<KnowledgeEnrichmentRecord> llmCandidates = baselineRecords.stream()
                .sorted(Comparator.comparingInt(this::priority))
                .limit(maxLlmRecords)
                .toList();

        int requestedBatches = (llmCandidates.size() + batchSize - 1) / batchSize;
        int completedBatches = 0;
        int enrichedRecordCount = 0;
        List<String> failures = new ArrayList<>();
        for (int start = 0; start < llmCandidates.size(); start += batchSize) {
            List<KnowledgeEnrichmentRecord> batch = llmCandidates.subList(start, Math.min(llmCandidates.size(), start + batchSize));
            try {
                String response = generationClient.generate(buildPrompt(batch));
                List<KnowledgeEnrichmentRecord> enrichedRecords = parseResponse(response, enrichedById);
                for (KnowledgeEnrichmentRecord enriched : enrichedRecords) {
                    enrichedById.put(enriched.id(), enriched);
                }
                completedBatches++;
                enrichedRecordCount += enrichedRecords.size();
            } catch (Exception exception) {
                // Keep deterministic baseline enrichment when LLM enrichment is unavailable or malformed.
                failures.add("batch " + (start / batchSize + 1) + ": " + safeMessage(exception));
            }
        }
        lastRunReport = new KnowledgeEnrichmentRunReport(
                "openai",
                baselineRecords.size(),
                requestedBatches,
                completedBatches,
                enrichedRecordCount,
                failures
        );

        return baselineRecords.stream()
                .map(record -> enrichedById.getOrDefault(record.id(), record))
                .toList();
    }

    @Override
    public KnowledgeEnrichmentRunReport lastRunReport() {
        return lastRunReport;
    }

    private String safeMessage(Exception exception) {
        String message = exception == null ? "unknown failure" : exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.trim();
    }

    private String buildPrompt(List<KnowledgeEnrichmentRecord> records) throws com.fasterxml.jackson.core.JsonProcessingException {
        return """
                # Goal
                Enrich code knowledge records for a Java Selenium/TestNG automation platform.

                # Context
                The records come from static repository indexing and already contain deterministic baseline values.
                Improve business meaning, intent, risks, coverage gaps, failure classification, and traceability without inventing code.

                # Constraints
                1. Return JSON only.
                2. Preserve every input id exactly.
                3. Do not invent files, classes, methods, locators, tests, requirements, or APIs not supported by input.
                4. Keep locatorStabilityScore between 0 and 1.
                5. Prefer concise strings and short arrays.
                6. If evidence is weak, keep baseline meaning and add a risk or coverage gap instead of guessing.

                # Input
                Baseline enrichment records:
                %s

                # Expected Output
                Return JSON in this exact shape:
                {
                  "records": [
                    {
                      "id": "<same id as input>",
                      "codeSummary": "<short class/method summary>",
                      "businessIntent": "<what this does from business/user perspective>",
                      "businessMeaning": "<why this matters in the product or automation workflow>",
                      "tags": ["<short tag>"],
                      "dependencies": ["<page object/helper/API/dependency>"],
                      "risks": ["<risk>"],
                      "stableLocators": ["<stable locator evidence>"],
                      "locatorStabilityScore": 0.0,
                      "preconditions": ["<precondition>"],
                      "postconditions": ["<postcondition>"],
                      "testCoverageGaps": ["<missing coverage>"],
                      "failureClassifications": ["<failure class>"],
                      "requirementTraceability": ["<Requirement -> Test -> Page -> Method evidence>"]
                    }
                  ]
                }

                # Success Criteria
                - Output records are directly usable for vector search and graph metadata.
                - Business intent is more specific than a technical method signature when evidence allows it.
                - Coverage gaps and failure classifications are practical for QA triage.

                # Notes
                This enrichment is metadata only; do not generate tests or page objects.
                """.formatted(objectMapper.writeValueAsString(records.stream().map(this::compactRecord).toList()));
    }

    private Map<String, Object> compactRecord(KnowledgeEnrichmentRecord record) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("id", record.id());
        compact.put("enrichmentType", record.enrichmentType());
        compact.put("relativePath", record.relativePath());
        compact.put("className", record.className());
        compact.put("methodName", record.methodName());
        compact.put("codeSummary", record.codeSummary());
        compact.put("businessIntent", record.businessIntent());
        compact.put("businessMeaning", record.businessMeaning());
        compact.put("tags", record.tags());
        compact.put("dependencies", record.dependencies());
        compact.put("risks", record.risks());
        compact.put("stableLocators", record.stableLocators());
        compact.put("locatorStabilityScore", record.locatorStabilityScore());
        compact.put("preconditions", record.preconditions());
        compact.put("postconditions", record.postconditions());
        compact.put("testCoverageGaps", record.testCoverageGaps());
        compact.put("failureClassifications", record.failureClassifications());
        compact.put("requirementTraceability", record.requirementTraceability());
        return compact;
    }

    private List<KnowledgeEnrichmentRecord> parseResponse(
            String response,
            Map<String, KnowledgeEnrichmentRecord> baselineById
    ) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(stripFences(response));
        JsonNode records = root.path("records");
        if (!records.isArray()) {
            return List.of();
        }
        List<KnowledgeEnrichmentRecord> enriched = new ArrayList<>();
        for (JsonNode node : records) {
            String id = text(node, "id", "");
            KnowledgeEnrichmentRecord baseline = baselineById.get(id);
            if (baseline == null) {
                continue;
            }
            enriched.add(new KnowledgeEnrichmentRecord(
                    baseline.id(),
                    baseline.enrichmentType(),
                    baseline.relativePath(),
                    baseline.className(),
                    baseline.methodName(),
                    text(node, "codeSummary", baseline.codeSummary()),
                    text(node, "businessIntent", baseline.businessIntent()),
                    text(node, "businessMeaning", baseline.businessMeaning()),
                    list(node, "tags", baseline.tags()),
                    list(node, "dependencies", baseline.dependencies()),
                    list(node, "risks", baseline.risks()),
                    list(node, "stableLocators", baseline.stableLocators()),
                    number(node, "locatorStabilityScore", baseline.locatorStabilityScore()),
                    list(node, "preconditions", baseline.preconditions()),
                    list(node, "postconditions", baseline.postconditions()),
                    list(node, "testCoverageGaps", baseline.testCoverageGaps()),
                    list(node, "failureClassifications", baseline.failureClassifications()),
                    list(node, "requirementTraceability", baseline.requirementTraceability()),
                    "openai"
            ));
        }
        return enriched;
    }

    private int priority(KnowledgeEnrichmentRecord record) {
        String text = (record.relativePath() + " " + record.className() + " " + record.tags()).toLowerCase();
        if (text.contains("page") || text.contains("test")) {
            return 0;
        }
        if (text.contains("ui") || text.contains("selenium")) {
            return 1;
        }
        return 2;
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value.trim();
    }

    private double number(JsonNode node, String field, double fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : fallback;
    }

    private List<String> list(JsonNode node, String field, List<String> fallback) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            return fallback;
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : value) {
            String text = item.asText("");
            if (!text.isBlank()) {
                items.add(text.trim());
            }
        }
        return items.isEmpty() ? fallback : items;
    }

    private String stripFences(String response) {
        String value = response == null ? "" : response.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value.trim();
    }
}
