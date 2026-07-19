package ua.demo.agentlab.validation.feedback;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeneratedUiRuntimeFeedbackWriter {

    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient httpClient;

    public GeneratedUiRuntimeFeedbackWriter(Neo4jRuntimeConfig config) {
        this(config, new JsonHttpClient());
    }

    public GeneratedUiRuntimeFeedbackWriter(Neo4jRuntimeConfig config, JsonHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient == null ? new JsonHttpClient() : httpClient;
    }

    public RuntimeFeedbackDbUpdateResult write(
            GeneratedUiSmokeResult smokeResult,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport,
            GeneratedTestExecutionResult testExecution,
            String runId
    ) {
        if (config == null || !config.enabled()) {
            return new RuntimeFeedbackDbUpdateResult(false, "neo4j", 0, "Neo4j feedback update is disabled");
        }
        if (config.password() == null || config.password().isBlank()) {
            return new RuntimeFeedbackDbUpdateResult(false, "neo4j", 0, "Neo4j password is not configured");
        }
        try {
            Map<String, Object> payload = payload(smokeResult, compileResult, reviewReport, testExecution, runId);
            httpClient.post(
                    commitUrl(),
                    Map.of("statements", List.of(Map.of("statement", statement(), "parameters", payload))),
                    headers()
            );
            return new RuntimeFeedbackDbUpdateResult(true, "neo4j", 1,
                    "Generated UI runtime feedback was written for runId=" + payload.get("runId"));
        } catch (Exception exception) {
            return new RuntimeFeedbackDbUpdateResult(false, "neo4j", 0,
                    "Neo4j feedback update failed: " + safe(exception.getMessage()));
        }
    }

    private Map<String, Object> payload(
            GeneratedUiSmokeResult smokeResult,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport,
            GeneratedTestExecutionResult testExecution,
            String runId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("feedbackId", "generated-ui-feedback-" + safe(runId, "unknown-run"));
        payload.put("runId", safe(runId, "unknown-run"));
        payload.put("createdAt", Instant.now().toString());
        payload.put("sourceAgent", "runtime-feedback-db-update-agent");
        payload.put("schemaVersion", "runtime-feedback-v1");
        payload.put("smokeStatus", smokeResult == null ? "UNKNOWN" : smokeResult.status().name());
        payload.put("smokeIssues", smokeResult == null ? 0 : smokeResult.issues().size());
        payload.put("filesChecked", smokeResult == null ? 0 : smokeResult.filesChecked());
        payload.put("compileStatus", compileResult == null ? "UNKNOWN" : compileResult.status().name());
        payload.put("reviewFindings", reviewReport == null ? 0 : reviewReport.totalFindings());
        payload.put("generatedTestExecutionStatus", testExecution == null ? "MISSING" : testExecution.status().name());
        payload.put("generatedTestsExecuted", testExecution == null ? 0 : testExecution.total());
        payload.put("generatedTestsPassed", testExecution == null ? 0 : testExecution.passed());
        boolean executionAccepted = testExecution != null
                && (testExecution.status() == GeneratedTestExecutionStatus.PASSED
                || testExecution.status() == GeneratedTestExecutionStatus.SKIPPED);
        boolean passed = smokeResult != null && smokeResult.passed() && executionAccepted;
        payload.put("qualitySignal", passed ? "PASSED" : "NEEDS_REVIEW");
        payload.put("locatorStatus", passed ? "ACTIVE" : "DEMOTED");
        payload.put("locatorValidationStatus", passed ? "PASSED" : "FAILED");
        payload.put("locatorRuntimePassRate", passed ? 1.0d : 0.0d);
        payload.put("locatorFlakyRate", passed ? 0.0d : 1.0d);
        payload.put("demotionReason", passed
                ? ""
                : "generated-ui-smoke-or-generated-test-execution-failed");
        return payload;
    }

    private String statement() {
        return """
                MERGE (n:UiKnowledgeNode:GeneratedUiRuntimeFeedback {feedbackId: $feedbackId})
                SET n.runId = $runId,
                    n.createdAt = $createdAt,
                    n.sourceAgent = $sourceAgent,
                    n.schemaVersion = $schemaVersion,
                    n.smokeStatus = $smokeStatus,
                    n.smokeIssues = $smokeIssues,
                    n.filesChecked = $filesChecked,
                    n.compileStatus = $compileStatus,
                    n.reviewFindings = $reviewFindings,
                    n.generatedTestExecutionStatus = $generatedTestExecutionStatus,
                    n.generatedTestsExecuted = $generatedTestsExecuted,
                    n.generatedTestsPassed = $generatedTestsPassed,
                    n.qualitySignal = $qualitySignal
                RETURN n.feedbackId
                """;
    }

    private String commitUrl() {
        String base = config.httpUrl().endsWith("/")
                ? config.httpUrl().substring(0, config.httpUrl().length() - 1)
                : config.httpUrl();
        return base + "/db/" + config.database() + "/tx/commit";
    }

    private Map<String, String> headers() {
        return Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes(StandardCharsets.UTF_8)
        ));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value, String fallback) {
        String safe = safe(value);
        return safe.isBlank() ? fallback : safe;
    }
}
