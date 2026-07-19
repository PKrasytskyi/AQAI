package ua.demo.agentlab.ui.discovery.interaction.selection;

import ua.demo.agentlab.ui.discovery.component.DynamicCssClassRiskClassifier;
import ua.demo.agentlab.ui.discovery.interaction.model.InteractionCandidate;
import ua.demo.agentlab.ui.discovery.interaction.model.RequirementScopedInteraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rejects unsafe selector evidence before ranking or browser execution. */
public final class InteractionSafetyGate {

    private final DynamicCssClassRiskClassifier dynamicCssClassRiskClassifier;

    public InteractionSafetyGate() {
        this(new DynamicCssClassRiskClassifier());
    }

    InteractionSafetyGate(DynamicCssClassRiskClassifier dynamicCssClassRiskClassifier) {
        this.dynamicCssClassRiskClassifier = java.util.Objects.requireNonNull(dynamicCssClassRiskClassifier);
    }

    public InteractionSafetyDecision evaluate(RequirementScopedInteraction interaction) {
        InteractionCandidate candidate = interaction.candidate();
        List<String> codes = new ArrayList<>();
        String value = normalize(candidate.value());
        String evidence = normalize(candidate.value() + " " + candidate.elementKey().semanticRole() + " "
                + String.join(" ", candidate.risks()));
        if (!candidate.visible()) codes.add("HIDDEN_ELEMENT");
        if (!candidate.enabled()) codes.add("DISABLED_ELEMENT");
        if (!candidate.sameOrigin() || value.startsWith("http://") || value.startsWith("https://")) {
            codes.add("EXTERNAL_ORIGIN");
        }
        if (containsAny(evidence, "_token", "csrf", "xsrf", "authenticity_token")) codes.add("SECURITY_TOKEN");
        if (value.startsWith("/html") || value.startsWith("//html") || value.contains("/body/")) codes.add("ABSOLUTE_XPATH");
        if (value.contains("nth-child") || value.contains("nth-of-type")) codes.add("POSITIONAL_SELECTOR");
        if (("css".equals(normalize(candidate.strategy())) && dynamicCssClassRiskClassifier.isDynamic(value))
                || evidence.contains("dynamic-hash") || evidence.contains("dynamic-css-hash")) {
            codes.add("DYNAMIC_HASH_SELECTOR");
        }
        if (value.isBlank()) codes.add("EMPTY_SELECTOR");
        if (!candidate.uniqueOnPage() && !candidate.uniqueWithinComponent()) codes.add("NON_UNIQUE_SELECTOR");
        return new InteractionSafetyDecision(interaction, codes.isEmpty(), codes.stream().distinct().toList());
    }

    public String selectorFamily(RequirementScopedInteraction interaction) {
        InteractionCandidate candidate = interaction.candidate();
        String strategy = normalize(candidate.strategy());
        String value = normalize(candidate.value());
        if (containsAny(value, "data-testid", "data-test", "data-qa", "data-cy")) return "test-attribute";
        if (strategy.equals("id")) return "id";
        if (strategy.equals("name")) return "name";
        if (containsAny(value, "aria-label", "role=")) return "accessibility";
        if (strategy.equals("xpath")) return "xpath";
        if (strategy.equals("css")) return "css";
        return strategy;
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
