package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

public class PromptEvidenceQualityGate {

    public void validate(PromptUiEvidence evidence) {
        if (evidence == null) {
            throw new IllegalStateException("Prompt evidence cannot be null");
        }
        for (PromptLocatorEvidence locator : evidence.requiredLocators()) {
            if (locator.evidenceType() != LocatorEvidenceType.CONFIRMED_LOCATOR) {
                throw new IllegalStateException("Prompt required locators must contain only CONFIRMED_LOCATOR evidence: "
                        + locator.fieldHint() + "=" + locator.value());
            }
            if (!locator.sameOrigin()) {
                throw new IllegalStateException("Prompt required locators must be same-origin: "
                        + locator.fieldHint() + "=" + locator.value());
            }
        }
        if (evidence.targetPage().isBlank() || evidence.targetRoute().isBlank()) {
            throw new IllegalStateException("Prompt evidence must include target page and target route");
        }
    }
}
