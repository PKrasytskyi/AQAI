package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.List;

/** Projects counts from existing evidence decisions; it never promotes or reclassifies evidence. */
public final class LocatorFunnelMetricsProjection {
    public UiEvidenceFunnelMetrics project(UiEvidenceFunnelInput input, List<UiEvidenceRequirementResult> results) {
        var trace = input.projectionTrace();
        int inventoryLocators = input.inventory() == null ? 0 : (int) input.inventory().pages().stream()
                .flatMap(page -> page.components().stream())
                .flatMap(component -> component.locators().stream())
                .map(locator -> locator.locatorId()).filter(value -> !value.isBlank()).distinct().count();
        int raw = trace == null || trace.rawCandidates() == 0
                ? inventoryLocators
                : trace.rawCandidates();
        int live = trace == null || trace.liveVerified() == 0
                ? input.liveVerification() == null ? 0 : (int) input.liveVerification().locatorVerifications().stream()
                .filter(locator -> locator.verified()).map(locator -> locator.locatorId()).distinct().count()
                : trace.liveVerified();
        int documentTransitions = input.liveVerification() == null || input.liveVerification().stateGraph() == null
                ? 0 : (int) input.liveVerification().stateGraph().transitions().stream()
                .filter(transition -> transition.routeChanged() && transition.confidence() > 0.0d)
                .count();
        int spaTransitions = input.liveVerification() == null || input.liveVerification().stateGraph() == null
                ? 0 : (int) input.liveVerification().stateGraph().transitions().stream()
                .filter(transition -> transition.sameRouteStateChange() && transition.confidence() > 0.0d)
                .count();
        int db = input.aiContext() == null ? 0 : input.aiContext().dbStableLocatorEvidence().size();
        int contextAllowed = trace == null ? promptAllowed(input) : trace.catalogPrimary();
        int requirementAllowed = trace == null
                ? results.stream().mapToInt(UiEvidenceRequirementResult::promptAllowedLocators).sum()
                : (int) trace.entries().stream()
                .filter(item -> item.promptAllowed() && !item.requirementIds().isEmpty())
                .map(item -> item.locatorEvidenceId()).filter(value -> !value.isBlank()).distinct().count();
        int boundPages = (int) results.stream().filter(UiEvidenceRequirementResult::requirementBound)
                .map(UiEvidenceRequirementResult::pageId).filter(value -> !value.isBlank()).distinct().count();
        int eligiblePages = (int) results.stream().filter(UiEvidenceRequirementResult::promptEligible)
                .map(UiEvidenceRequirementResult::pageId).filter(value -> !value.isBlank()).distinct().count();
        return new UiEvidenceFunnelMetrics(raw, inventoryLocators, live, documentTransitions, spaTransitions, db,
                contextAllowed, requirementAllowed, boundPages, eligiblePages);
    }

    private int promptAllowed(UiEvidenceFunnelInput input) {
        if (input.aiContext() == null || input.aiContext().promptUiEvidence() == null) return 0;
        return input.aiContext().promptUiEvidence().requiredLocators().size();
    }
}
