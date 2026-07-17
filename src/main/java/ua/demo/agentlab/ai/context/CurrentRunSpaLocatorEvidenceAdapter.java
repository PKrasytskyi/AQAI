package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.util.List;
import java.util.Locale;

/** Converts only current-run browser-verified SPA locators into prompt-safe evidence. */
public final class CurrentRunSpaLocatorEvidenceAdapter {
    public List<PromptLocatorEvidence> adapt(SpaTargetedVerificationResult verification) {
        if (verification == null) return List.of();
        return verification.locatorVerifications().stream()
                .filter(TargetedLocatorVerification::verified)
                .map(this::toPromptLocator)
                .toList();
    }

    public List<PromptLocatorEvidence> adapt(SpaLiveTargetedVerificationResult verification) {
        if (verification == null || !verification.executed()) return List.of();
        return verification.locatorVerifications().stream()
                .filter(TargetedLocatorVerification::verified)
                .map(this::toPromptLocator)
                .toList();
    }

    private PromptLocatorEvidence toPromptLocator(TargetedLocatorVerification locator) {
        String element = nonBlank(locator.elementId(), locator.locatorId());
        java.util.ArrayList<String> trace = new java.util.ArrayList<>(List.of(
                "spa-current-run-verified",
                "spa-page-id:" + locator.pageId(),
                "spa-route:" + locator.route(),
                "spa-component-id:" + locator.componentId(),
                "spa-locator-id:" + locator.locatorId()
        ));
        locator.requirementIds().stream()
                .filter(requirementId -> requirementId != null && !requirementId.isBlank())
                .map(requirementId -> "requirement-id:" + requirementId.trim())
                .forEach(trace::add);
        return new PromptLocatorEvidence(fieldHint(element), element, locator.strategy(), locator.value(), "", element, "",
                true, locator.qualityScore(), locator.componentId(), "", 1, 1, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                trace);
    }

    private String fieldHint(String value) {
        String[] tokens = safe(value).replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ").trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) return "element";
        StringBuilder result = new StringBuilder(tokens[0]);
        for (int index = 1; index < tokens.length; index++) {
            if (!tokens[index].isBlank()) result.append(Character.toUpperCase(tokens[index].charAt(0))).append(tokens[index].substring(1));
        }
        return result.toString();
    }
    private String nonBlank(String first, String second) { return !safe(first).isBlank() ? first.trim() : safe(second); }
    private String safe(String value) { return value == null ? "" : value.trim(); }
}
