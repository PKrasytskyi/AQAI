package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AiRunQualitySummaryService {

    private static final double LOW_CONFIDENCE_LOCATOR_THRESHOLD = 0.60d;

    public AiRunQualitySummary summarize(WorkflowState state) {
        if (state == null) {
            return emptySummary();
        }
        return summarize(new AiRunQualitySummaryInput(
                state.getKnowledgeRunMetadata(),
                state.getNormalizedRequirementBundle(),
                state.getCanonicalTestCaseBundle(),
                state.getMappedUiKnowledge(),
                state.getArtifacts()
        ));
    }

    public AiRunQualitySummary summarize(AiRunQualitySummaryInput input) {
        if (input == null) {
            return emptySummary();
        }
        MappedUiKnowledge mappedKnowledge = input.mappedUiKnowledge();
        List<LocatorCandidate> locatorCandidates = locatorCandidates(mappedKnowledge);
        int requirements = input.normalizedRequirementBundle() == null
                ? 0
                : input.normalizedRequirementBundle().requirements().size();
        int canonicalTestCases = input.canonicalTestCaseBundle() == null
                ? 0
                : input.canonicalTestCaseBundle().testCases().size();
        int expectedResultsResolved = intArtifact(input, "test.case.expectation.resolved.count", 0);
        int expectedResultsNeedsReview = Math.max(0, canonicalTestCases - expectedResultsResolved);
        int mappedPages = mappedKnowledge == null ? 0 : mappedKnowledge.pages().size();
        int pageObjectPrompts = pageObjectPromptCount(input);
        int promptPagesWithoutAllowedLocators = promptPagesWithoutAllowedLocators(input);
        int weakPromptLocatorCoverage = weakPromptLocatorCoverage(input);
        int routeCollisions = routeCollisions(mappedKnowledge);
        int externalEvidenceRejected = (int) locatorCandidates.stream()
                .filter(this::isExternalEvidence)
                .count();
        int lowConfidenceLocators = (int) locatorCandidates.stream()
                .filter(locator -> locator.stabilityScore() < LOW_CONFIDENCE_LOCATOR_THRESHOLD
                        || locator.risks().stream().anyMatch(this::isUnstableRisk))
                .count();
        int confirmedLocators = evidenceTypeCount(locatorCandidates, LocatorEvidenceType.CONFIRMED_LOCATOR);
        int candidateLocators = evidenceTypeCount(locatorCandidates, LocatorEvidenceType.CANDIDATE_LOCATOR);
        int fallbackLocators = evidenceTypeCount(locatorCandidates, LocatorEvidenceType.FALLBACK_LOCATOR);
        int promptBlockingIssues = promptBlockingIssues(input);
        int promptAllowedLocators = intArtifact(input, "prompt.ui.evidence.locator.count", 0);
        int runtimeFeedbackIssues = intArtifact(input, "ui.runtime.feedback.issue.count", 0);
        double runtimeLocatorPassRate = doubleArtifact(input, "ui.runtime.feedback.locator.pass.rate", 1.0d);
        double runtimeFlakyRiskScore = doubleArtifact(input, "ui.runtime.feedback.flaky.risk.score", 0.0d);
        int componentPageCount = intArtifact(input, "ui.component.page.count", 0);
        int componentScopedLocatorCount = intArtifact(input, "ui.component.scoped.locator.count", 0);
        boolean neo4jHit = booleanArtifact(input, "ui.knowledge.retrieval.neo4j.hit", false);
        boolean qdrantHit = booleanArtifact(input, "ui.knowledge.retrieval.qdrant.hit", false);
        String retrievalMode = stringArtifact(input, "ui.knowledge.retrieval.mode",
                normalizedRetrievalMode(stringArtifact(input, "page.knowledge.cache.retrieval.mode", "unknown")));
        boolean stableCacheUsed = booleanArtifact(input, "ui.knowledge.retrieval.stable.cache.used",
                retrievalMode.equals("stable-page-cache"));
        int staleEvidenceRejected = intArtifact(input, "ui.knowledge.retrieval.stale.evidence.rejected", 0);
        String vectorUnavailableReason = stringArtifact(input, "ui.knowledge.retrieval.vector.unavailable.reason", "");
        double averageLocatorScore = averageLocatorScore(locatorCandidates);
        int qualityScore = qualityScore(
                canonicalTestCases,
                expectedResultsNeedsReview,
                locatorCandidates.size(),
                promptAllowedLocators,
                lowConfidenceLocators,
                confirmedLocators,
                candidateLocators,
                fallbackLocators,
                routeCollisions,
                externalEvidenceRejected,
                promptBlockingIssues,
                pageObjectPrompts,
                weakPromptLocatorCoverage,
                mappedPages,
                componentPageCount,
                componentScopedLocatorCount,
                averageLocatorScore,
                runtimeFeedbackIssues,
                runtimeLocatorPassRate,
                runtimeFlakyRiskScore,
                qdrantHit,
                vectorUnavailableReason,
                staleEvidenceRejected
        );
        return new AiRunQualitySummary(
                runId(input),
                requirements,
                canonicalTestCases,
                expectedResultsResolved,
                expectedResultsNeedsReview,
                mappedPages,
                pageObjectPrompts,
                promptPagesWithoutAllowedLocators,
                routeCollisions,
                externalEvidenceRejected,
                lowConfidenceLocators,
                confirmedLocators,
                candidateLocators,
                fallbackLocators,
                promptBlockingIssues,
                round2(averageLocatorScore),
                neo4jHit,
                qdrantHit,
                retrievalMode,
                stableCacheUsed,
                staleEvidenceRejected,
                vectorUnavailableReason,
                qualityScore
        );
    }

    private AiRunQualitySummary emptySummary() {
        return new AiRunQualitySummary(
                Instant.now().toString(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0.0d,
                false,
                false,
                "unknown",
                false,
                0,
                "",
                0
        );
    }

    private String runId(AiRunQualitySummaryInput input) {
        if (input.knowledgeRunMetadata() != null && !input.knowledgeRunMetadata().runId().isBlank()) {
            return input.knowledgeRunMetadata().runId();
        }
        String artifactRunId = input.artifacts().get("ui.knowledge.run.id");
        if (artifactRunId != null && !artifactRunId.isBlank()) {
            return artifactRunId;
        }
        return Instant.now().toString();
    }

    private List<LocatorCandidate> locatorCandidates(MappedUiKnowledge knowledge) {
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return List.of();
        }
        List<LocatorCandidate> candidates = new ArrayList<>();
        for (MappedPage page : knowledge.pages()) {
            for (MappedElement element : page.elements()) {
                candidates.addAll(element.locatorCandidates());
            }
            for (MappedForm form : page.forms()) {
                for (MappedField field : form.fields()) {
                    candidates.addAll(field.locatorCandidates());
                }
            }
        }
        return candidates;
    }

    private int routeCollisions(MappedUiKnowledge knowledge) {
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return 0;
        }
        Map<String, Long> routeCounts = knowledge.pages().stream()
                .map(this::routeKey)
                .filter(route -> !route.isBlank())
                .collect(Collectors.groupingBy(
                        route -> route,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        return (int) routeCounts.values().stream()
                .filter(count -> count > 1)
                .count();
    }

    private String routeKey(MappedPage page) {
        if (page == null) {
            return "";
        }
        if (page.urlPattern() != null && !page.urlPattern().isBlank()) {
            return page.urlPattern();
        }
        return page.url() == null ? "" : page.url();
    }

    private boolean isExternalEvidence(LocatorCandidate locator) {
        if (locator == null) {
            return false;
        }
        if (!locator.href().isBlank() && !locator.sameOrigin()) {
            return true;
        }
        return locator.risks().stream()
                .map(risk -> risk.toLowerCase(Locale.ROOT))
                .anyMatch(risk -> risk.contains("external") || risk.contains("outbound"));
    }

    private boolean isUnstableRisk(String risk) {
        if (risk == null) {
            return false;
        }
        String normalized = risk.toLowerCase(Locale.ROOT);
        return normalized.contains("unstable")
                || normalized.contains("low-confidence")
                || normalized.contains("absolute-xpath")
                || normalized.contains("long-xpath");
    }

    private double averageLocatorScore(List<LocatorCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0.0d;
        }
        return candidates.stream()
                .mapToDouble(LocatorCandidate::stabilityScore)
                .filter(Double::isFinite)
                .average()
                .orElse(0.0d);
    }

    private int promptBlockingIssues(AiRunQualitySummaryInput input) {
        return input.artifacts().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("ai.page.object.prompt.quality.")
                        && entry.getKey().endsWith(".blocking"))
                .mapToInt(entry -> parseInt(entry.getValue(), 0))
                .sum();
    }

    private int evidenceTypeCount(List<LocatorCandidate> candidates, LocatorEvidenceType type) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }
        return (int) candidates.stream()
                .filter(locator -> locator != null && locator.evidenceType() == type)
                .count();
    }

    private int pageObjectPromptCount(AiRunQualitySummaryInput input) {
        int scopedRequests = intArtifact(input, "openai.page.object.scoped.requests", -1);
        if (scopedRequests >= 0) {
            return scopedRequests;
        }
        return (int) input.artifacts().keySet().stream()
                .filter(key -> key.startsWith("ai.page.object.prompt.")
                        && key.endsWith(".allowedLocators"))
                .count();
    }

    private int promptPagesWithoutAllowedLocators(AiRunQualitySummaryInput input) {
        return (int) input.artifacts().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("ai.page.object.prompt.")
                        && entry.getKey().endsWith(".allowedLocators"))
                .filter(entry -> parseInt(entry.getValue(), 0) == 0)
                .count();
    }

    private int weakPromptLocatorCoverage(AiRunQualitySummaryInput input) {
        return (int) input.artifacts().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("ai.page.object.prompt.")
                        && entry.getKey().endsWith(".allowedLocators"))
                .filter(entry -> parseInt(entry.getValue(), 0) > 0)
                .filter(entry -> parseInt(entry.getValue(), 0) < 3)
                .count();
    }

    private int intArtifact(AiRunQualitySummaryInput input, String key, int defaultValue) {
        return parseInt(input.artifacts().get(key), defaultValue);
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private double doubleArtifact(AiRunQualitySummaryInput input, String key, double defaultValue) {
        String value = input.artifacts().get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private boolean booleanArtifact(AiRunQualitySummaryInput input, String key, boolean defaultValue) {
        String value = input.artifacts().get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private String stringArtifact(AiRunQualitySummaryInput input, String key, String defaultValue) {
        String value = input.artifacts().get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String normalizedRetrievalMode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private int qualityScore(
            int canonicalTestCases,
            int expectedResultsNeedsReview,
            int locatorCount,
            int promptAllowedLocators,
            int lowConfidenceLocators,
            int confirmedLocators,
            int candidateLocators,
            int fallbackLocators,
            int routeCollisions,
            int externalEvidenceRejected,
            int promptBlockingIssues,
            int pageObjectPrompts,
            int weakPromptLocatorCoverage,
            int mappedPages,
            int componentPageCount,
            int componentScopedLocatorCount,
            double averageLocatorScore,
            int runtimeFeedbackIssues,
            double runtimeLocatorPassRate,
            double runtimeFlakyRiskScore,
            boolean qdrantHit,
            String vectorUnavailableReason,
            int staleEvidenceRejected
    ) {
        double score = 100.0d;
        if (canonicalTestCases > 0) {
            score -= 20.0d * expectedResultsNeedsReview / canonicalTestCases;
        }
        if (locatorCount > 0) {
            score -= 15.0d * lowConfidenceLocators / locatorCount;
        } else {
            score -= 20.0d;
        }
        if (promptAllowedLocators <= 0 && canonicalTestCases > 0) {
            score -= 25.0d;
        }
        if (locatorCount > 0 && confirmedLocators <= 0) {
            score -= 20.0d;
        }
        if (locatorCount > 0) {
            score -= Math.min(12.0d, 12.0d * fallbackLocators / locatorCount);
            score -= Math.min(6.0d, 6.0d * candidateLocators / locatorCount);
        }
        if (pageObjectPrompts > 0) {
            score -= Math.min(20.0d, weakPromptLocatorCoverage * 10.0d);
        }
        if (mappedPages > 0 && componentPageCount < mappedPages) {
            score -= 10.0d;
        }
        if (mappedPages > 0 && componentScopedLocatorCount <= 0) {
            score -= 10.0d;
        }
        score -= Math.min(20.0d, promptBlockingIssues * 25.0d);
        score -= Math.min(15.0d, routeCollisions * 10.0d);
        score -= Math.min(10.0d, externalEvidenceRejected * 2.0d);
        if (averageLocatorScore < 0.80d) {
            score -= (0.80d - averageLocatorScore) * 25.0d;
        }
        if (runtimeLocatorPassRate < 0.75d) {
            score -= (0.75d - runtimeLocatorPassRate) * 20.0d;
        }
        score -= Math.min(10.0d, runtimeFlakyRiskScore * 10.0d);
        score -= Math.min(10.0d, runtimeFeedbackIssues * 2.0d);
        if (!qdrantHit && vectorUnavailableReason != null && !vectorUnavailableReason.isBlank()
                && !vectorUnavailableReason.equalsIgnoreCase("vector retrieval disabled")) {
            score -= 5.0d;
        }
        score -= Math.min(10.0d, staleEvidenceRejected * 2.0d);
        return (int) Math.round(Math.max(0.0d, Math.min(100.0d, score)));
    }

    private double round2(double value) {
        if (!Double.isFinite(value)) {
            return 0.0d;
        }
        return Math.round(value * 100.0d) / 100.0d;
    }
}
