package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskClassifier {

    public TaskClassification classify(String request) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("request cannot be blank");
        }
        String normalized = request.toLowerCase(Locale.ROOT);
        Set<String> signals = new LinkedHashSet<>();

        if (containsAny(normalized, "page object", "pom", "page class")) {
            signals.add("page-object");
            return new TaskClassification(
                    RagTaskType.PAGE_OBJECT_GENERATION,
                    0.94d,
                    signals,
                    List.of(ArtifactType.PAGE_OBJECT, ArtifactType.BASE_CLASS, ArtifactType.POLICY)
            );
        }
        if (containsAny(normalized, "failure", "error", "stacktrace", "screenshot", "allure", "broken", "flaky")) {
            signals.add("failure-analysis");
            return new TaskClassification(
                    RagTaskType.FAILURE_ANALYSIS,
                    0.92d,
                    signals,
                    List.of(ArtifactType.TEST_CLASS, ArtifactType.PAGE_OBJECT, ArtifactType.POLICY, ArtifactType.DOCUMENTATION)
            );
        }
        if (containsAny(normalized, "review", "audit", "check code", "inspect")) {
            signals.add("code-review");
            return new TaskClassification(
                    RagTaskType.CODE_REVIEW,
                    0.88d,
                    signals,
                    List.of(ArtifactType.TEST_CLASS, ArtifactType.PAGE_OBJECT, ArtifactType.BASE_CLASS, ArtifactType.POLICY)
            );
        }
        if (containsAny(normalized, "fix", "repair", "correct")) {
            signals.add("bug-fix");
            return new TaskClassification(
                    RagTaskType.BUG_FIX,
                    0.86d,
                    signals,
                    List.of(ArtifactType.TEST_CLASS, ArtifactType.PAGE_OBJECT, ArtifactType.BASE_CLASS, ArtifactType.TEST_DATA)
            );
        }

        boolean mentionsApi = containsAny(normalized, "api", "endpoint", "rest", "contract", "swagger", "openapi");
        boolean mentionsUi = containsAny(normalized, "ui", "selenium", "page", "screen", "browser", "locator");
        boolean negative = containsAny(normalized, "negative", "invalid", "error case", "restricted");
        boolean smoke = normalized.contains("smoke");
        boolean regression = normalized.contains("regression");

        if (negative) {
            signals.add("negative");
        }
        if (smoke) {
            signals.add("smoke");
        }
        if (regression) {
            signals.add("regression");
        }
        if (mentionsApi) {
            signals.add("api");
        }
        if (mentionsUi) {
            signals.add("ui");
        }

        if (mentionsApi) {
            return new TaskClassification(
                    negative ? RagTaskType.NEGATIVE_TEST_GENERATION
                            : smoke ? RagTaskType.SMOKE_TEST_GENERATION
                            : regression ? RagTaskType.REGRESSION_TEST_GENERATION
                            : RagTaskType.API_TEST_GENERATION,
                    negative || smoke || regression ? 0.87d : 0.90d,
                    signals,
                    apiPreferredArtifacts(negative, regression)
            );
        }
        if (mentionsUi || containsAny(normalized, "test", "scenario", "automation", "generate")) {
            return new TaskClassification(
                    negative ? RagTaskType.NEGATIVE_TEST_GENERATION
                            : smoke ? RagTaskType.SMOKE_TEST_GENERATION
                            : regression ? RagTaskType.REGRESSION_TEST_GENERATION
                            : RagTaskType.UI_TEST_GENERATION,
                    negative || smoke || regression ? 0.85d : 0.89d,
                    signals,
                    uiPreferredArtifacts(negative, regression)
            );
        }

        return new TaskClassification(
                RagTaskType.GENERAL_AUTOMATION,
                0.60d,
                signals,
                List.of(
                        ArtifactType.PAGE_OBJECT,
                        ArtifactType.TEST_CLASS,
                        ArtifactType.BASE_CLASS,
                        ArtifactType.POLICY,
                        ArtifactType.DOCUMENTATION
                )
        );
    }

    private List<ArtifactType> apiPreferredArtifacts(boolean negative, boolean regression) {
        List<ArtifactType> types = new ArrayList<>();
        types.add(ArtifactType.API_CLIENT);
        types.add(ArtifactType.TEST_CLASS);
        types.add(ArtifactType.TEST_DATA);
        types.add(ArtifactType.POLICY);
        if (negative || regression) {
            types.add(ArtifactType.DOCUMENTATION);
        }
        return List.copyOf(types);
    }

    private List<ArtifactType> uiPreferredArtifacts(boolean negative, boolean regression) {
        List<ArtifactType> types = new ArrayList<>();
        types.add(ArtifactType.PAGE_OBJECT);
        types.add(ArtifactType.TEST_CLASS);
        types.add(ArtifactType.BASE_CLASS);
        types.add(ArtifactType.TEST_DATA);
        types.add(ArtifactType.POLICY);
        if (negative || regression) {
            types.add(ArtifactType.DOCUMENTATION);
        }
        return List.copyOf(types);
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
