package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PromptEvidencePrioritizer {

    private final PageOwnershipSlicer ownershipSlicer;

    public PromptEvidencePrioritizer() {
        this(new PageOwnershipSlicer());
    }

    public PromptEvidencePrioritizer(PageOwnershipSlicer ownershipSlicer) {
        this.ownershipSlicer = ownershipSlicer == null ? new PageOwnershipSlicer() : ownershipSlicer;
    }

    public List<PromptLocatorEvidence> prioritize(
            List<PromptLocatorEvidence> locators,
            AiContextPackage context,
            PromptPageScope scope
    ) {
        if (locators == null || locators.isEmpty() || scope == null || scope.targetPage() == null) {
            return List.of();
        }
        boolean loginPage = ownershipSlicer.isLoginPage(scope.targetPage());
        boolean logoutMenuScope = hasLogoutMenuRequirement(context, scope.targetPage());
        return locators.stream()
                .sorted(Comparator
                        .comparingDouble((PromptLocatorEvidence locator) -> promptLocatorPriority(locator, loginPage, logoutMenuScope))
                        .reversed()
                        .thenComparing(Comparator.comparingDouble(PromptLocatorEvidence::stabilityScore).reversed())
                        .thenComparing(PromptLocatorEvidence::fieldHint))
                .toList();
    }

    private double promptLocatorPriority(PromptLocatorEvidence locator, boolean loginPage, boolean logoutMenuScope) {
        String evidence = locatorEvidenceText(locator);
        double priority = 0.0d;
        if (loginPage && containsAny(evidence, "username")) {
            priority += 100.0d;
        }
        if (loginPage && containsAny(evidence, "password") && containsAny(evidence, "input", "name=password", "placeholder='password'")) {
            priority += 100.0d;
        }
        if (loginPage && containsAny(evidence, "login", "submit")) {
            priority += 95.0d;
        }
        if (logoutMenuScope && containsAny(evidence,
                "userdropdown",
                "user dropdown",
                "user menu",
                "user-menu-trigger",
                "open_menu",
                "open user menu",
                "dropdown tab",
                "dropdown-tab")) {
            priority += 110.0d;
        }
        if (logoutMenuScope && containsAny(evidence, "logout", "log out", "signout", "sign out")) {
            priority += 105.0d;
        }
        if (containsAny(evidence, "dashboard") && containsAny(evidence, "heading", "breadcrumb", "title", "h6")) {
            priority += 90.0d;
        }
        if (containsAny(evidence, "change password", "changepassword", "updatepassword")) {
            priority -= 10.0d;
        }
        return priority;
    }

    private boolean hasLogoutMenuRequirement(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.canonicalTestCaseBundle() == null || targetPage == null) {
            return false;
        }
        return context.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownershipSlicer.canonicalTestCaseBelongsToTarget(testCase, targetPage))
                .map(this::canonicalRequirementText)
                .anyMatch(text -> containsAny(text, "logout", "sign out") && containsAny(text, "user menu", "menu"));
    }

    private String canonicalRequirementText(CanonicalTestCase testCase) {
        return String.join(" ",
                        safe(testCase.title()),
                        String.join(" ", testCase.actions()),
                        String.join(" ", testCase.assertions()),
                        testCase.operationIntents().stream()
                                .map(intent -> intent.kind() == null ? "" : intent.kind().name())
                                .toList()
                                .toString())
                .toLowerCase(Locale.ROOT);
    }

    private String locatorEvidenceText(PromptLocatorEvidence locator) {
        return String.join(" ",
                        locator.fieldHint(),
                        locator.elementName(),
                        locator.role(),
                        locator.visibleText(),
                        locator.href(),
                        locator.value(),
                        String.join(" ", locator.sourceTrace()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... fragments) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
