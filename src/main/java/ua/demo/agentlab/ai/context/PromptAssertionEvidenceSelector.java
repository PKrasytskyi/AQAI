package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromptAssertionEvidenceSelector {

    private final PageOwnershipSlicer ownershipSlicer;

    public PromptAssertionEvidenceSelector() {
        this(new PageOwnershipSlicer());
    }

    public PromptAssertionEvidenceSelector(PageOwnershipSlicer ownershipSlicer) {
        this.ownershipSlicer = ownershipSlicer == null ? new PageOwnershipSlicer() : ownershipSlicer;
    }

    public List<PromptAssertionEvidence> select(AiContextPackage context, PromptPageScope scope) {
        if (context == null || scope == null || scope.targetPage() == null) {
            return List.of();
        }
        MappedPage targetPage = scope.targetPage();
        List<PromptAssertionEvidence> assertions = new ArrayList<>();
        for (AssertionContract contract : context.assertionContracts()) {
            if (assertionContractBelongsToTarget(contract, targetPage)) {
                assertions.add(new PromptAssertionEvidence(
                        contract.type().name(),
                        contract.expectedValue(),
                        contract.ownerPage(),
                        contract.sourceLine().isBlank() ? contract.testCaseId() : contract.sourceLine(),
                        contract.confidence()
                ));
            }
        }
        if (context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                if (!ownershipSlicer.canonicalTestCaseBelongsToTarget(testCase, targetPage)) {
                    continue;
                }
                testCase.assertions().forEach(assertion -> {
                    if (assertionTextFitsTarget("CANONICAL_ASSERTION", assertion, targetPage)) {
                        assertions.add(new PromptAssertionEvidence(
                                "CANONICAL_ASSERTION",
                                assertion,
                                testCase.pageName(),
                                testCase.id(),
                                0.70d
                        ));
                    }
                });
                testCase.assertionIntents().forEach(intent -> {
                    String type = intent.kind() == null ? "" : intent.kind().name();
                    if (assertionTextFitsTarget(type, intent.expectedValue(), targetPage)) {
                        assertions.add(new PromptAssertionEvidence(
                                type,
                                intent.expectedValue(),
                                testCase.pageName(),
                                testCase.id(),
                                0.75d
                        ));
                    }
                });
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

    private boolean assertionContractBelongsToTarget(AssertionContract contract, MappedPage targetPage) {
        if (contract == null) {
            return false;
        }
        boolean ownerMatches = ownershipSlicer.pageNameMatches(contract.ownerPage(), targetPage)
                || RouteCanonicalizer.routeEqualsOrSuffix(contract.route(), ownershipSlicer.route(targetPage));
        return ownerMatches && assertionTextFitsTarget(contract.type().name(), contract.expectedValue(), targetPage);
    }

    private boolean assertionTextFitsTarget(String type, String expectedValue, MappedPage targetPage) {
        String expected = expectedValue == null ? "" : expectedValue.trim();
        String normalized = expected.toLowerCase(Locale.ROOT);
        if ("URL_CONTAINS".equalsIgnoreCase(type) || "ROUTE_EQUALS".equalsIgnoreCase(type)) {
            return RouteCanonicalizer.routeEqualsOrSuffix(expected, ownershipSlicer.route(targetPage));
        }
        if (containsAny(normalized, "login form", "username", "password input", "login button")
                && !ownershipSlicer.isLoginPage(targetPage)) {
            return false;
        }
        if (containsAny(normalized, "registration", "forgot password", "password recovery")
                && !ownershipSlicer.isLoginPage(targetPage)) {
            return false;
        }
        if (containsAny(normalized, "/profile", "account/profile")
                && !RouteCanonicalizer.routeEqualsOrSuffix("/profile", ownershipSlicer.route(targetPage))) {
            return false;
        }
        return true;
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
}
