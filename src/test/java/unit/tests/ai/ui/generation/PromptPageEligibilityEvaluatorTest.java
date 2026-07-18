package unit.tests.ai.ui.generation;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.assertions.model.AssertionSource;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectPromptScope;
import ua.demo.agentlab.ai.ui.generation.PromptPage;
import ua.demo.agentlab.ai.ui.generation.PromptPageEligibilityEvaluator;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedCatalogPage;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedUiCatalog;

import java.util.List;
import java.util.Map;

public class PromptPageEligibilityEvaluatorTest {

    @Test
    public void pageWithoutConfirmedCatalogActionsOrLocatorsIsReducedToRouteOnlyScope() {
        AiPageObjectPromptScope scope = scope(new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                List.of("REQ-001"),
                List.of(new PromptActionEvidence("Sign out from current session", "LOGOUT", "DashboardPage", "REQ-001")),
                List.of(new PromptAssertionEvidence(
                        "AUTHENTICATED_AREA_VISIBLE",
                        "Protected landing page is visible",
                        "DashboardPage",
                        "REQ-001",
                        0.80d
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.80d
        ));

        PromptPage promptPage = new PromptPageEligibilityEvaluator().evaluate(scope);

        Assert.assertFalse(promptPage.eligible());
        Assert.assertTrue(promptPage.routeOnlyContract());
        Assert.assertTrue(promptPage.reasons().stream()
                .anyMatch(reason -> reason.contains("does not require a generated POM")));
    }

    @Test
    public void routeOnlyContractIsSkippedBecauseBasePageAlreadyOwnsRouteNavigation() {
        AiPageObjectPromptScope scope = scope(new PromptUiEvidence(
                "ProtectedRoutePage",
                "/dashboard/index",
                List.of("REQ-ROUTE"),
                List.of(new PromptActionEvidence("Open the target page", "OPEN_PAGE", "ProtectedRoutePage", "REQ-ROUTE")),
                List.of(new PromptAssertionEvidence(
                        "URL_CONTAINS",
                        "/dashboard/index",
                        "ProtectedRoutePage",
                        "REQ-ROUTE",
                        0.90d
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.90d
        ));

        PromptPage promptPage = new PromptPageEligibilityEvaluator().evaluate(scope);

        Assert.assertFalse(promptPage.eligible());
        Assert.assertTrue(promptPage.routeOnlyContract());
    }

    @Test
    public void routeBackedContractWithMissingLocatorAssertionsIsSkippedUntilPageEvidenceExists() {
        AiPageObjectPromptScope scope = scope(new PromptUiEvidence(
                "DashboardPage",
                "/dashboard/index",
                List.of("REQ-ROUTE", "REQ-LOGOUT"),
                List.of(),
                List.of(
                        new PromptAssertionEvidence(
                                "URL_CONTAINS",
                                "/dashboard/index",
                                "DashboardPage",
                                "REQ-ROUTE",
                                1.0d
                        ),
                        new PromptAssertionEvidence(
                                "ELEMENT_VISIBLE",
                                "logoutLink",
                                "DashboardPage",
                                "REQ-LOGOUT",
                                0.88d
                        )
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.80d
        ));

        PromptPage promptPage = new PromptPageEligibilityEvaluator().evaluate(scope);

        Assert.assertFalse(promptPage.eligible());
        Assert.assertTrue(promptPage.routeOnlyContract());
        Assert.assertTrue(promptPage.reasons().stream().anyMatch(reason -> reason.contains("does not require a generated POM")));
    }

    private AiPageObjectPromptScope scope(PromptUiEvidence evidence) {
        AiContextPackage context = new AiContextPackage(
                "test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                evidence
        );
        ConfirmedUiCatalog catalog = new ConfirmedUiCatalog(
                ConfirmedUiCatalog.SCHEMA_VERSION,
                "test-run",
                true,
                List.of(new ConfirmedCatalogPage(
                        "AUTHENTICATED_AREA",
                        evidence.targetPage().toLowerCase(),
                        evidence.targetPage(),
                        evidence.targetRoute(),
                        evidence.targetPage().toLowerCase() + ":state",
                        List.of(),
                        evidence.requiredAssertions().stream().map(assertion -> new AssertionContract(
                                "REQ", "REQ", AssertionType.valueOf(assertion.type()), assertion.expectedValue(),
                                assertion.ownerPage(), evidence.targetRoute(), assertion.sourceTrace(),
                                assertion.confidence(), AssertionSource.REQUIREMENT)).toList(),
                        List.of()
                )),
                List.of()
        );
        return new AiPageObjectPromptScope(
                evidence.targetPage(),
                evidence.targetPage(),
                null,
                context,
                context,
                List.of(),
                null,
                Map.of(),
                catalog
        );
    }
}
