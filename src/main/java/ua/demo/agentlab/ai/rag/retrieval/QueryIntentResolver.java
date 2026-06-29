package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class QueryIntentResolver {

    private static final Set<String> STOP_WORDS = Set.of(
            "generate", "create", "write", "build", "new", "for", "the", "a", "an",
            "and", "with", "that", "this", "from", "into", "using", "test", "tests",
            "case", "cases", "scenario", "scenarios", "automation", "ui", "api"
    );

    private final TaskClassifier taskClassifier;

    public QueryIntentResolver() {
        this(new TaskClassifier());
    }

    public QueryIntentResolver(TaskClassifier taskClassifier) {
        this.taskClassifier = taskClassifier;
    }

    public QueryIntent resolve(String request) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("request cannot be blank");
        }

        String normalized = request.toLowerCase(Locale.ROOT);
        TaskClassification taskClassification = taskClassifier.classify(request);
        Set<String> qualifiers = new LinkedHashSet<>();
        if (normalized.contains("negative")) {
            qualifiers.add("negative");
        }
        if (normalized.contains("positive")) {
            qualifiers.add("positive");
        }
        if (normalized.contains("smoke")) {
            qualifiers.add("smoke");
        }
        if (normalized.contains("regression")) {
            qualifiers.add("regression");
        }
        if (normalized.contains("login") || normalized.contains("sign in") || normalized.contains("signin")) {
            qualifiers.add("authentication");
        }

        Set<String> domainTerms = new LinkedHashSet<>();
        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.isBlank() || token.length() < 3 || STOP_WORDS.contains(token)) {
                continue;
            }
            domainTerms.add(token);
        }

        List<ArtifactType> requestedArtifactTypes = new ArrayList<>(taskClassification.preferredArtifactTypes());
        if (normalized.contains("page object")) {
            requestedArtifactTypes.add(ArtifactType.PAGE_OBJECT);
        }
        if (normalized.contains("policy") || normalized.contains("locator")) {
            requestedArtifactTypes.add(ArtifactType.POLICY);
        }
        if (normalized.contains("feature") || normalized.contains("gherkin")) {
            requestedArtifactTypes.add(ArtifactType.FEATURE_FILE);
        }
        if (normalized.contains("test data") || normalized.contains("credentials") || normalized.contains("factory")) {
            requestedArtifactTypes.add(ArtifactType.TEST_DATA);
        }
        if (normalized.contains("base")) {
            requestedArtifactTypes.add(ArtifactType.BASE_CLASS);
        }
        if (normalized.contains("test") || normalized.contains("scenario")) {
            requestedArtifactTypes.add(ArtifactType.TEST_CLASS);
            requestedArtifactTypes.add(ArtifactType.PAGE_OBJECT);
            requestedArtifactTypes.add(ArtifactType.BASE_CLASS);
            requestedArtifactTypes.add(ArtifactType.TEST_DATA);
            requestedArtifactTypes.add(ArtifactType.POLICY);
        }
        if (requestedArtifactTypes.isEmpty()) {
            requestedArtifactTypes = List.of(
                    ArtifactType.PAGE_OBJECT,
                    ArtifactType.TEST_CLASS,
                    ArtifactType.BASE_CLASS,
                    ArtifactType.POLICY,
                    ArtifactType.DOCUMENTATION
            );
        }

        return new QueryIntent(request.trim(), domainTerms, qualifiers, requestedArtifactTypes, taskClassification);
    }
}
