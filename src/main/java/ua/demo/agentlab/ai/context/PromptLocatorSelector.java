package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PromptLocatorSelector {

    public Optional<LocatorCandidate> select(
            List<LocatorCandidate> candidates,
            String elementName,
            String role,
            String visibleText
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        String context = normalize(String.join(" ", safe(elementName), safe(role), safe(visibleText)));
        return candidates.stream()
                .filter(candidate -> candidate != null && !candidate.value().isBlank())
                .filter(LocatorCandidate::sameOrigin)
                .filter(candidate -> !promptForbidden(candidate))
                .max(Comparator.comparingDouble(candidate -> promptScore(candidate, context)));
    }

    private double promptScore(LocatorCandidate candidate, String context) {
        double score = candidate.stabilityScore();
        score += strategyPreference(candidate.strategy());
        score += semanticBonus(candidate, context);
        score -= riskPenalty(candidate);
        if (!candidate.uniqueOnPage()) {
            score -= 0.08d;
        }
        if (!candidate.stableAcrossRuns()) {
            score -= 0.10d;
        }
        return score;
    }

    private double strategyPreference(LocatorStrategy strategy) {
        return switch (strategy) {
            case ID -> 0.05d;
            case NAME -> 0.06d;
            case CSS -> 0.03d;
            case XPATH -> -0.10d;
            case LINK_TEXT -> -0.04d;
            case PARTIAL_LINK_TEXT -> -0.04d;
            case TAG_NAME -> -0.12d;
            case CLASS_NAME -> -0.08d;
            case UNKNOWN -> -0.20d;
        };
    }

    private double semanticBonus(LocatorCandidate candidate, String context) {
        String value = normalize(candidate.value());
        String evidence = normalize(String.join(" ",
                context,
                candidate.elementRole(),
                candidate.accessibleName(),
                candidate.visibleText()
        ));
        if (containsAny(evidence, "password", "pass") && containsAny(value, "password", "pass")) {
            return 0.12d;
        }
        if (containsAny(evidence, "username", "user name", "user", "email")
                && containsAny(value, "username", "user", "email")) {
            return 0.12d;
        }
        if (containsAny(evidence, "submit", "button", "login")
                && containsAny(value, "submit", "button")) {
            return 0.08d;
        }
        return 0.0d;
    }

    private double riskPenalty(LocatorCandidate candidate) {
        double penalty = 0.0d;
        for (String risk : candidate.risks()) {
            String normalized = normalize(risk);
            if (normalized.contains("external") || normalized.contains("semantic-locator-conflict")) {
                penalty += 1.0d;
            } else if (normalized.contains("generated")) {
                penalty += 0.30d;
            } else if (normalized.contains("generic-id")) {
                penalty += 0.20d;
            } else if (normalized.contains("unstable") || normalized.contains("not-proven")) {
                penalty += 0.12d;
            } else {
                penalty += 0.04d;
            }
        }
        return penalty;
    }

    private boolean promptForbidden(LocatorCandidate candidate) {
        for (String risk : candidate.risks()) {
            String normalized = normalize(risk);
            if (normalized.contains("external")
                    || normalized.contains("semantic-locator-conflict")
                    || normalized.contains("hidden-or-invisible")
                    || normalized.contains("security-token")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
