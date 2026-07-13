package ua.demo.agentlab.artifactreuse.fingerprint;

import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyAssertion;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyLocator;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PomContractFingerprintBuilder implements ArtifactFingerprintBuilder<PomContractFingerprintInput> {

    private final CanonicalArtifactHasher hasher;

    public PomContractFingerprintBuilder() {
        this(new CanonicalArtifactHasher());
    }

    PomContractFingerprintBuilder(CanonicalArtifactHasher hasher) {
        if (hasher == null) {
            throw new IllegalArgumentException("hasher cannot be null");
        }
        this.hasher = hasher;
    }

    @Override
    public ArtifactFingerprint build(PomContractFingerprintInput input) {
        if (input == null) {
            throw new IllegalArgumentException("fingerprint input cannot be null");
        }
        PromptUiEvidence evidence = input.promptUiEvidence();
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("artifactType", "POM_CONTRACT");
        canonical.put("targetPageId", firstNonBlank(input.targetPageId(), input.targetPageName()));
        canonical.put("targetPageName", input.targetPageName());
        canonical.put("route", firstNonBlank(input.route(), evidence == null ? "" : evidence.targetRoute()));
        canonical.put("capability", input.capability());
        canonical.put("appId", input.appId());
        canonical.put("baseUrlHash", input.baseUrlHash());
        PromptReadyPomScope scope = input.promptScope();
        canonical.put("approvedLocators", scope == null ? evidence == null ? List.of() : locators(evidence.requiredLocators())
                : readyLocators(scope.allowedLocators()));
        canonical.put("requiredActions", scope == null ? evidence == null ? List.of() : actions(evidence.requiredActions())
                : scope.ownedActions().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        canonical.put("requiredAssertions", scope == null ? evidence == null ? List.of() : assertions(evidence.requiredAssertions())
                : readyAssertions(scope.ownedAssertions()));
        canonical.put("promptTemplateVersion", input.promptTemplateVersion());
        canonical.put("pomContractSchemaVersion", input.pomContractSchemaVersion());
        canonical.put("modelName", input.modelName());
        canonical.put("temperature", input.temperature());
        canonical.put("generationMode", input.generationMode());
        canonical.put("writerVersion", input.writerVersion());
        canonical.put("promptMode", input.promptMetadata().getOrDefault("promptMode", ""));
        return hasher.hash(canonical);
    }

    private List<Map<String, Object>> locators(List<PromptLocatorEvidence> locators) {
        return locators.stream().map(locator -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("fieldHint", locator.fieldHint());
            value.put("elementName", locator.elementName());
            value.put("strategy", locator.strategy());
            value.put("value", locator.value());
            value.put("role", locator.role());
            value.put("componentName", locator.componentName());
            value.put("sameOrigin", locator.sameOrigin());
            value.put("stabilityScore", locator.stabilityScore());
            value.put("evidenceType", locator.evidenceType().name());
            value.put("uniqueWithinComponent", locator.uniqueWithinComponent());
            return value;
        }).toList();
    }

    private List<Map<String, Object>> readyLocators(List<PromptReadyLocator> locators) {
        return locators.stream().map(locator -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", locator.id());
            value.put("strategy", locator.strategy());
            value.put("value", locator.value());
            value.put("role", locator.role());
            value.put("componentName", locator.componentName());
            value.put("sameOrigin", locator.sameOrigin());
            value.put("score", locator.score());
            value.put("evidenceType", locator.evidenceType().name());
            value.put("uniqueWithinComponent", locator.uniqueWithinComponent());
            return value;
        }).toList();
    }

    private List<Map<String, Object>> actions(List<PromptActionEvidence> actions) {
        return actions.stream().map(action -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", action.name());
            value.put("type", action.type());
            value.put("ownerPage", action.ownerPage());
            return value;
        }).toList();
    }

    private List<Map<String, Object>> assertions(List<PromptAssertionEvidence> assertions) {
        return assertions.stream().map(assertion -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", assertion.type());
            value.put("expectedValue", assertion.expectedValue());
            value.put("ownerPage", assertion.ownerPage());
            value.put("confidence", assertion.confidence());
            return value;
        }).toList();
    }

    private List<Map<String, Object>> readyAssertions(List<PromptReadyAssertion> assertions) {
        return assertions.stream().map(assertion -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", assertion.type());
            value.put("expectedValue", assertion.expectedValue());
            value.put("ownerPage", assertion.ownerPage());
            value.put("confidence", assertion.confidence());
            return value;
        }).toList();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }
}
