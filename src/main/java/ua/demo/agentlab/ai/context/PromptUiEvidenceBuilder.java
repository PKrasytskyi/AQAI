package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PromptUiEvidenceBuilder {

    public PromptUiEvidence build(AiContextPackage context) {
        if (context == null || context.mappedUiKnowledge() == null || context.mappedUiKnowledge().pages().isEmpty()) {
            return PromptUiEvidence.empty("prompt-evidence:no-mapped-page-scope");
        }
        MappedPage targetPage = context.mappedUiKnowledge().pages().get(0);
        Set<String> requirementIds = requirementIds(context);
        List<PromptActionEvidence> actions = actionEvidence(context, targetPage);
        List<PromptAssertionEvidence> assertions = assertionEvidence(context, targetPage);
        List<PromptLocatorEvidence> locators = locatorEvidence(context, targetPage);
        List<ExcludedEvidence> excluded = context.mappedUiKnowledgeCurated() == null
                ? List.of()
                : context.mappedUiKnowledgeCurated().excludedEvidence().stream()
                .filter(evidence -> evidence.pageId().isBlank() || evidence.pageId().equals(targetPage.pageId()))
                .limit(20)
                .toList();
        List<String> sourceTrace = new ArrayList<>();
        sourceTrace.add("prompt-evidence:scoped-context");
        sourceTrace.add("prompt-evidence:targetPage=" + targetPage.pageName());
        sourceTrace.add("prompt-evidence:requirementIds=" + requirementIds);
        return new PromptUiEvidence(
                targetPage.pageName(),
                route(targetPage),
                new ArrayList<>(requirementIds),
                actions,
                assertions,
                locators,
                List.of(),
                excluded,
                sourceTrace,
                confidence(context, locators, assertions)
        );
    }

    private Set<String> requirementIds(AiContextPackage context) {
        Set<String> ids = new LinkedHashSet<>();
        if (context.canonicalTestCaseBundle() != null) {
            context.canonicalTestCaseBundle().testCases().forEach(testCase -> ids.addAll(testCase.requirementRefs()));
        }
        context.assertionContracts().forEach(contract -> addIfPresent(ids, contract.requirementId()));
        return ids;
    }

    private List<PromptActionEvidence> actionEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptActionEvidence> actions = new ArrayList<>();
        if (context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                testCase.actions().forEach(action -> actions.add(new PromptActionEvidence(
                        action,
                        "canonical-test-action",
                        ownerPage(testCase),
                        testCase.id()
                )));
                testCase.operationIntents().forEach(intent -> actions.add(new PromptActionEvidence(
                        intent.kind() == null ? "" : intent.kind().name(),
                        "operation-intent",
                        ownerPage(testCase),
                        testCase.id()
                )));
            }
        }
        for (MappedAction action : targetPage.actions()) {
            actions.add(new PromptActionEvidence(
                    action.actionName(),
                    action.actionType(),
                    targetPage.pageName(),
                    "mapped-action:" + action.actionId()
            ));
        }
        return deduplicateActions(actions).stream().limit(12).toList();
    }

    private List<PromptAssertionEvidence> assertionEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptAssertionEvidence> assertions = new ArrayList<>();
        for (AssertionContract contract : context.assertionContracts()) {
            assertions.add(new PromptAssertionEvidence(
                    contract.type().name(),
                    contract.expectedValue(),
                    contract.ownerPage(),
                    contract.sourceLine().isBlank() ? contract.testCaseId() : contract.sourceLine(),
                    contract.confidence()
            ));
        }
        if (context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                testCase.assertions().forEach(assertion -> assertions.add(new PromptAssertionEvidence(
                        "CANONICAL_ASSERTION",
                        assertion,
                        testCase.pageName(),
                        testCase.id(),
                        0.70d
                )));
                testCase.assertionIntents().forEach(intent -> assertions.add(new PromptAssertionEvidence(
                        intent.kind() == null ? "" : intent.kind().name(),
                        intent.expectedValue(),
                        testCase.pageName(),
                        testCase.id(),
                        0.75d
                )));
            }
        }
        targetPage.assertionHints().forEach(hint -> assertions.add(new PromptAssertionEvidence(
                hint.hintType(),
                hint.target().isBlank() ? hint.description() : hint.target(),
                targetPage.pageName(),
                "mapped-assertion-hint",
                hint.confidenceScore()
        )));
        return assertions.stream()
                .filter(assertion -> !assertion.type().isBlank() || !assertion.expectedValue().isBlank())
                .limit(16)
                .toList();
    }

    private List<PromptLocatorEvidence> locatorEvidence(AiContextPackage context, MappedPage targetPage) {
        List<PromptLocatorEvidence> locators = new ArrayList<>();
        for (MappedElement element : targetPage.elements()) {
            element.locatorCandidates().stream().findFirst().ifPresent(locator ->
                    locators.add(toLocatorEvidence(element.semanticName(), element.role(), element.text(), locator)));
        }
        targetPage.forms().forEach(form -> form.fields().forEach(field ->
                field.locatorCandidates().stream().findFirst().ifPresent(locator ->
                        locators.add(toLocatorEvidence(field.fieldName(), field.fieldType(), field.label(), locator)))));
        return locators.stream().limit(16).toList();
    }

    private PromptLocatorEvidence toLocatorEvidence(
            String elementName,
            String role,
            String visibleText,
            LocatorCandidate locator
    ) {
        return new PromptLocatorEvidence(
                fieldHint(elementName),
                elementName,
                locator.strategy().wireName(),
                locator.value(),
                role.isBlank() ? locator.elementRole() : role,
                visibleText.isBlank() ? locator.visibleText() : visibleText,
                locator.href(),
                locator.sameOrigin(),
                locator.stabilityScore(),
                List.of("curated-locator:" + locator.evidenceSource())
        );
    }

    private String route(MappedPage page) {
        return page.urlPattern().isBlank() ? page.url() : page.urlPattern();
    }

    private String ownerPage(CanonicalTestCase testCase) {
        return testCase.sourcePageName().isBlank() ? testCase.pageName() : testCase.sourcePageName();
    }

    private List<PromptActionEvidence> deduplicateActions(List<PromptActionEvidence> actions) {
        Set<String> keys = new LinkedHashSet<>();
        List<PromptActionEvidence> deduped = new ArrayList<>();
        for (PromptActionEvidence action : actions) {
            String key = action.name() + "|" + action.type() + "|" + action.ownerPage();
            if (keys.add(key)) {
                deduped.add(action);
            }
        }
        return deduped;
    }

    private double confidence(AiContextPackage context, List<PromptLocatorEvidence> locators, List<PromptAssertionEvidence> assertions) {
        double locatorConfidence = locators.stream()
                .mapToDouble(PromptLocatorEvidence::stabilityScore)
                .average()
                .orElse(0.0d);
        double assertionConfidence = assertions.stream()
                .mapToDouble(PromptAssertionEvidence::confidence)
                .average()
                .orElse(0.0d);
        double curatedConfidence = context.mappedUiKnowledgeCurated() == null ? 0.0d : context.mappedUiKnowledgeCurated().confidence();
        return Math.max(locatorConfidence, Math.max(assertionConfidence, curatedConfidence));
    }

    private String fieldHint(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (normalized.isBlank()) {
            return "element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0].substring(0, 1).toLowerCase() + parts[0].substring(1));
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase()).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }
}
