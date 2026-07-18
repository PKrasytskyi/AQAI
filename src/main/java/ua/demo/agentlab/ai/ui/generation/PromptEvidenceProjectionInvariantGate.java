package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

/** Stops confirmed catalog evidence from disappearing behind a route-only POM decision. */
public final class PromptEvidenceProjectionInvariantGate {

    public void validate(AiPageObjectPromptScope source, PromptReadyPomScope projected) {
        if (source == null || source.scopedContext() == null || projected == null) return;
        List<PromptLocatorEvidence> catalogLocators = source.scopedContext().confirmedCatalogLocatorEvidence();
        long confirmed = catalogLocators == null ? 0L : catalogLocators.stream()
                .filter(locator -> locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR)
                .filter(PromptLocatorEvidence::sameOrigin)
                .filter(locator -> locator.sourceTrace().stream().anyMatch(trace -> trace.startsWith("requirement-id:")))
                .count();
        if (confirmed > 0 && projected.allowedLocators().isEmpty()) {
            throw new IllegalStateException("EVIDENCE_PROJECTION_MISMATCH: page " + source.pageName()
                    + " has " + confirmed + " confirmed requirement-owned catalog locator(s),"
                    + " but PromptReadyPomScope contains none");
        }
    }
}
