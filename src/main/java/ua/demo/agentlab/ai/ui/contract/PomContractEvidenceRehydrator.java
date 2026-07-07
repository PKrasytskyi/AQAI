package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PomContractEvidenceRehydrator {

    public PomContractSpec rehydrate(PomContractSpec spec, AiContextPackage contextPackage) {
        if (spec == null || contextPackage == null || contextPackage.promptUiEvidence() == null) {
            return spec;
        }
        Map<String, PromptLocatorEvidence> evidenceById = evidenceById(contextPackage.promptUiEvidence());
        return new PomContractSpec(
                spec.schemaVersion(),
                spec.page(),
                rehydrateLocators(spec.locators(), evidenceById),
                spec.components().stream()
                        .map(component -> rehydrateComponent(component, evidenceById))
                        .toList(),
                spec.actions(),
                spec.assertions(),
                spec.coverageGaps(),
                spec.rejectedSuggestions()
        );
    }

    public PomContractSpec rehydrate(PomContractSpec spec, PromptReadyPomScope promptScope, AiContextPackage fallbackContext) {
        if (spec == null) {
            return null;
        }
        Map<String, PromptReadyLocator> readyById = readyEvidenceById(promptScope);
        PomContractSpec readyContract = new PomContractSpec(
                spec.schemaVersion(),
                spec.page(),
                rehydrateReadyLocators(spec.locators(), readyById),
                spec.components().stream()
                        .map(component -> rehydrateReadyComponent(component, readyById))
                        .toList(),
                spec.actions(),
                spec.assertions(),
                spec.coverageGaps(),
                spec.rejectedSuggestions()
        );
        return hasUnresolvedEvidence(readyContract) && fallbackContext != null
                ? rehydrate(readyContract, fallbackContext)
                : readyContract;
    }

    private PomComponentSpec rehydrateComponent(
            PomComponentSpec component,
            Map<String, PromptLocatorEvidence> evidenceById
    ) {
        return new PomComponentSpec(
                component.name(),
                component.type(),
                component.rootLocatorId(),
                rehydrateLocators(component.locators(), evidenceById),
                component.actions(),
                component.assertions(),
                component.reusable()
        );
    }

    private PomComponentSpec rehydrateReadyComponent(
            PomComponentSpec component,
            Map<String, PromptReadyLocator> evidenceById
    ) {
        return new PomComponentSpec(
                component.name(),
                component.type(),
                component.rootLocatorId(),
                rehydrateReadyLocators(component.locators(), evidenceById),
                component.actions(),
                component.assertions(),
                component.reusable()
        );
    }

    private List<PomLocatorSpec> rehydrateLocators(
            List<PomLocatorSpec> locators,
            Map<String, PromptLocatorEvidence> evidenceById
    ) {
        return locators.stream()
                .map(locator -> {
                    PromptLocatorEvidence evidence = evidenceById.get(normalize(locator.id()));
                    if (evidence == null) {
                        return locator;
                    }
                    return new PomLocatorSpec(
                            locator.id(),
                            firstNonBlank(locator.elementName(), evidence.elementName()),
                            firstNonBlank(locator.strategy(), evidence.strategy()),
                            firstNonBlank(locator.value(), evidence.value()),
                            firstNonBlank(locator.role(), evidence.role()),
                            locator.stabilityScore() > 0.0d ? locator.stabilityScore() : evidence.stabilityScore(),
                            evidence.evidenceType().name(),
                            evidence.sameOrigin(),
                            evidence.uniqueWithinComponent(),
                            evidence.sourceTrace()
                    );
                })
                .toList();
    }

    private List<PomLocatorSpec> rehydrateReadyLocators(
            List<PomLocatorSpec> locators,
            Map<String, PromptReadyLocator> evidenceById
    ) {
        return locators.stream()
                .map(locator -> {
                    PromptReadyLocator evidence = evidenceById.get(normalize(locator.id()));
                    if (evidence == null) {
                        return locator;
                    }
                    return new PomLocatorSpec(
                            locator.id(),
                            firstNonBlank(locator.elementName(), evidence.elementName()),
                            firstNonBlank(locator.strategy(), evidence.strategy()),
                            firstNonBlank(locator.value(), evidence.value()),
                            firstNonBlank(locator.role(), evidence.role()),
                            locator.stabilityScore() > 0.0d ? locator.stabilityScore() : evidence.score(),
                            evidence.evidenceType().name(),
                            evidence.sameOrigin(),
                            evidence.uniqueWithinComponent(),
                            evidence.sourceTrace()
                    );
                })
                .toList();
    }

    private Map<String, PromptReadyLocator> readyEvidenceById(PromptReadyPomScope scope) {
        Map<String, PromptReadyLocator> values = new LinkedHashMap<>();
        if (scope == null) {
            return values;
        }
        scope.allowedLocators().forEach(locator -> {
            put(values, locator.id(), locator);
            put(values, locator.elementName(), locator);
        });
        return values;
    }

    private Map<String, PromptLocatorEvidence> evidenceById(PromptUiEvidence evidence) {
        Map<String, PromptLocatorEvidence> values = new LinkedHashMap<>();
        List.of(evidence.requiredLocators(), evidence.candidateLocators(), evidence.fallbackLocators())
                .forEach(list -> list.forEach(locator -> {
                    put(values, locator.fieldHint(), locator);
                    put(values, locator.elementName(), locator);
                }));
        return values;
    }

    private void put(Map<String, PromptReadyLocator> values, String key, PromptReadyLocator evidence) {
        String normalized = normalize(key);
        if (!normalized.isBlank()) {
            values.putIfAbsent(normalized, evidence);
        }
    }

    private void put(Map<String, PromptLocatorEvidence> values, String key, PromptLocatorEvidence evidence) {
        String normalized = normalize(key);
        if (!normalized.isBlank()) {
            values.putIfAbsent(normalized, evidence);
        }
    }

    private boolean hasUnresolvedEvidence(PomContractSpec contract) {
        return contract.locators().stream().anyMatch(this::isUnresolved)
                || contract.components().stream()
                .flatMap(component -> component.locators().stream())
                .anyMatch(this::isUnresolved);
    }

    private boolean isUnresolved(PomLocatorSpec locator) {
        return locator.evidenceType().isBlank() || !locator.sameOrigin();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
