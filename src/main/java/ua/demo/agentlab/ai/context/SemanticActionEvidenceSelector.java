package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.BusinessIntentCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticElementModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SemanticActionEvidenceSelector {

    private final SemanticActionModelBuilder semanticActionModelBuilder;
    private final PageOwnershipSlicer ownershipSlicer;

    public SemanticActionEvidenceSelector() {
        this(new SemanticActionModelBuilder(), new PageOwnershipSlicer());
    }

    public SemanticActionEvidenceSelector(
            SemanticActionModelBuilder semanticActionModelBuilder,
            PageOwnershipSlicer ownershipSlicer
    ) {
        this.semanticActionModelBuilder = semanticActionModelBuilder == null
                ? new SemanticActionModelBuilder()
                : semanticActionModelBuilder;
        this.ownershipSlicer = ownershipSlicer == null ? new PageOwnershipSlicer() : ownershipSlicer;
    }

    public List<PromptActionEvidence> select(AiContextPackage context, PromptPageScope scope) {
        if (context == null || scope == null || scope.targetPage() == null) {
            return List.of();
        }
        MappedPage targetPage = scope.targetPage();
        if (!hasPageOwnedActionScenarios(context, targetPage)) {
            return List.of();
        }
        List<PromptActionEvidence> actions = new ArrayList<>();
        actions.addAll(semanticActionEvidence(context, targetPage).stream()
                .filter(action -> actionRelevantToScope(context, targetPage, action))
                .toList());
        if (context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                if (!ownershipSlicer.canonicalTestCaseSourceBelongsToTarget(testCase, targetPage)) {
                    continue;
                }
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
            PromptActionEvidence evidence = new PromptActionEvidence(
                    action.actionName(),
                    action.actionType(),
                    targetPage.pageName(),
                    "mapped-action:" + action.actionId()
            );
            if (actionRelevantToScope(context, targetPage, evidence)) {
                actions.add(evidence);
            }
        }
        return deduplicate(actions).stream().limit(12).toList();
    }

    private boolean hasPageOwnedActionScenarios(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.canonicalTestCaseBundle() == null) {
            return ownershipSlicer.isAuthenticationPage(targetPage);
        }
        return context.canonicalTestCaseBundle().testCases().stream()
                .anyMatch(testCase -> ownershipSlicer.canonicalTestCaseSourceBelongsToTarget(testCase, targetPage)
                        && (!testCase.actions().isEmpty() || !testCase.operationIntents().isEmpty())
                        || targetOwnedMenuActionScenario(testCase, targetPage));
    }

    private boolean targetOwnedMenuActionScenario(CanonicalTestCase testCase, MappedPage targetPage) {
        if (!ownershipSlicer.canonicalTestCaseBelongsToTarget(testCase, targetPage)) {
            return false;
        }
        String text = canonicalRequirementText(testCase);
        return containsAny(text, "logout", "sign out")
                && containsAny(text, "user menu", "menu", "open_menu", "open user menu");
    }

    private List<PromptActionEvidence> semanticActionEvidence(AiContextPackage context, MappedPage targetPage) {
        SemanticPageModel semanticPage = semanticActionModelBuilder.buildForTarget(
                context.pageModelBundle(),
                context.mappedUiKnowledge(),
                targetPage
        );
        if (semanticPage == null) {
            return List.of();
        }
        List<PromptActionEvidence> actions = new ArrayList<>();
        for (BusinessIntentCandidate intent : semanticPage.pageBusinessIntentCandidates()) {
            if (intent.confidence() >= 0.70d) {
                actions.add(new PromptActionEvidence(
                        intent.intent(),
                        "semantic-business-intent",
                        targetPage.pageName(),
                        "semantic-page:" + semanticPage.pageId()
                ));
            }
        }
        Map<String, SemanticElementModel> elementsById = semanticPage.elements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SemanticElementModel::elementId,
                        element -> element,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (ActionCandidate action : semanticPage.pageActionCandidates()) {
            if (action.confidence() >= 0.76d) {
                SemanticElementModel element = elementsById.get(action.targetElementId());
                actions.add(new PromptActionEvidence(
                        element == null || element.name().isBlank()
                                ? action.action()
                                : element.name() + ":" + action.action(),
                        "semantic-action",
                        targetPage.pageName(),
                        "semantic-action:" + action.targetElementId()
                ));
            }
        }
        for (SemanticElementModel element : semanticPage.elements()) {
            for (BusinessIntentCandidate intent : element.businessIntentCandidates()) {
                if (intent.confidence() >= 0.78d) {
                    actions.add(new PromptActionEvidence(
                            element.name() + ":" + intent.intent(),
                            "semantic-element-intent",
                            targetPage.pageName(),
                            "semantic-element:" + element.elementId()
                    ));
                }
            }
        }
        return actions;
    }

    private boolean actionRelevantToScope(
            AiContextPackage context,
            MappedPage targetPage,
            PromptActionEvidence action
    ) {
        String actionText = normalize(action.name() + " " + action.type() + " " + action.sourceTrace());
        String scopeText = scopedRequirementText(context, targetPage);
        if (scopeText.isBlank()) {
            return true;
        }
        boolean logoutScope = containsAny(scopeText, "logout", "sign out")
                || containsAny(scopeText, "user menu", "open menu", "drop-down", "dropdown");
        if (logoutScope) {
            return containsAny(actionText,
                    "logout",
                    "sign out",
                    "open_menu",
                    "open user menu",
                    "openusermenu",
                    "user menu",
                    "user-menu",
                    "userdropdown",
                    "user-menu-trigger");
        }
        if (!containsAny(scopeText, "search", "filter")
                && containsAny(actionText, "search", ":type", "type", "clear")) {
            return false;
        }
        return true;
    }

    private String scopedRequirementText(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.canonicalTestCaseBundle() == null) {
            return "";
        }
        return context.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownershipSlicer.canonicalTestCaseBelongsToTarget(testCase, targetPage))
                .map(this::canonicalRequirementText)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private String canonicalRequirementText(CanonicalTestCase testCase) {
        return normalize(String.join(" ",
                safe(testCase.title()),
                String.join(" ", testCase.actions()),
                String.join(" ", testCase.assertions()),
                testCase.operationIntents().stream()
                        .map(intent -> intent.kind() == null ? "" : intent.kind().name())
                        .toList()
                        .toString()
        ));
    }

    private List<PromptActionEvidence> deduplicate(List<PromptActionEvidence> actions) {
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

    private String ownerPage(CanonicalTestCase testCase) {
        return testCase.sourcePageName().isBlank() ? testCase.pageName() : testCase.sourcePageName();
    }

    private boolean containsAny(String value, String... fragments) {
        String normalized = normalize(value);
        for (String fragment : fragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
