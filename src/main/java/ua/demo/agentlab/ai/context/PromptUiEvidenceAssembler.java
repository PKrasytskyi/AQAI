package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;

public class PromptUiEvidenceAssembler {

    public PromptUiEvidence assemble(
            AiContextPackage context,
            PromptPageScope scope,
            List<PromptActionEvidence> actions,
            List<PromptAssertionEvidence> assertions,
            List<PromptLocatorEvidence> locatorCandidates
    ) {
        List<PromptLocatorEvidence> locators = locatorCandidates.stream()
                .filter(locator -> locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR)
                .toList();
        List<PromptLocatorEvidence> candidateLocators = locatorCandidates.stream()
                .filter(locator -> locator.evidenceType() == LocatorEvidenceType.CANDIDATE_LOCATOR)
                .toList();
        List<PromptLocatorEvidence> fallbackLocators = locatorCandidates.stream()
                .filter(locator -> locator.evidenceType() == LocatorEvidenceType.FALLBACK_LOCATOR)
                .toList();
        return new PromptUiEvidence(
                scope.targetPageName(),
                scope.targetRoute(),
                scope.requiresAuthentication(),
                scope.prerequisitePages(),
                scope.requirementIds(),
                actions,
                assertions,
                locators,
                candidateLocators,
                fallbackLocators,
                List.of(),
                scope.excludedEvidence(),
                scope.sourceTrace(),
                confidence(context, locators, assertions)
        );
    }

    private double confidence(
            AiContextPackage context,
            List<PromptLocatorEvidence> locators,
            List<PromptAssertionEvidence> assertions
    ) {
        double locatorConfidence = locators.stream()
                .mapToDouble(PromptLocatorEvidence::stabilityScore)
                .average()
                .orElse(0.0d);
        double assertionConfidence = assertions.stream()
                .mapToDouble(PromptAssertionEvidence::confidence)
                .average()
                .orElse(0.0d);
        double curatedConfidence = context.mappedUiKnowledgeCurated() == null
                ? 0.0d
                : context.mappedUiKnowledgeCurated().confidence();
        return Math.max(locatorConfidence, Math.max(assertionConfidence, curatedConfidence));
    }
}
