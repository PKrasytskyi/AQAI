package ua.demo.agentlab.ai.quality;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

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
        int routeCollisions = routeCollisions(mappedKnowledge);
        int externalEvidenceRejected = (int) locatorCandidates.stream()
                .filter(this::isExternalEvidence)
                .count();
        int lowConfidenceLocators = (int) locatorCandidates.stream()
                .filter(locator -> locator.stabilityScore() < LOW_CONFIDENCE_LOCATOR_THRESHOLD
                        || locator.risks().stream().anyMatch(this::isUnstableRisk))
                .count();
        int promptBlockingIssues = promptBlockingIssues(input);
        double averageLocatorScore = averageLocatorScore(locatorCandidates);
        int qualityScore = qualityScore(
                canonicalTestCases,
                expectedResultsNeedsReview,
                locatorCandidates.size(),
                lowConfidenceLocators,
                routeCollisions,
                externalEvidenceRejected,
                promptBlockingIssues,
                averageLocatorScore
        );
        return new AiRunQualitySummary(
                runId(input),
                requirements,
                canonicalTestCases,
                expectedResultsResolved,
                expectedResultsNeedsReview,
                mappedPages,
                routeCollisions,
                externalEvidenceRejected,
                lowConfidenceLocators,
                promptBlockingIssues,
                round2(averageLocatorScore),
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
                0.0d,
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

    private int qualityScore(
            int canonicalTestCases,
            int expectedResultsNeedsReview,
            int locatorCount,
            int lowConfidenceLocators,
            int routeCollisions,
            int externalEvidenceRejected,
            int promptBlockingIssues,
            double averageLocatorScore
    ) {
        double score = 100.0d;
        if (canonicalTestCases > 0) {
            score -= 20.0d * expectedResultsNeedsReview / canonicalTestCases;
        }
        if (locatorCount > 0) {
            score -= 15.0d * lowConfidenceLocators / locatorCount;
        } else {
            score -= 10.0d;
        }
        score -= Math.min(20.0d, promptBlockingIssues * 25.0d);
        score -= Math.min(15.0d, routeCollisions * 10.0d);
        score -= Math.min(10.0d, externalEvidenceRejected * 2.0d);
        if (averageLocatorScore < 0.80d) {
            score -= (0.80d - averageLocatorScore) * 25.0d;
        }
        return (int) Math.round(Math.max(0.0d, Math.min(100.0d, score)));
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
