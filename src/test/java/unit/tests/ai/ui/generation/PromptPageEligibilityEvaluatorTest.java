package unit.tests.ai.ui.generation;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectPromptScope;
import ua.demo.agentlab.ai.ui.generation.PromptPage;
import ua.demo.agentlab.ai.ui.generation.PromptPageEligibilityEvaluator;

import java.util.List;
import java.util.Map;

public class PromptPageEligibilityEvaluatorTest {

    @Test
    public void pageWithoutRawEvidenceOrLocatorsIsNotPromptEligible() {
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
        Assert.assertTrue(promptPage.reasons().stream().anyMatch(reason -> reason.contains("no raw DOM evidence")));
    }

    @Test
    public void routeOnlyContractIsPromptEligibleWithoutLocators() {
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

        Assert.assertTrue(promptPage.eligible());
        Assert.assertTrue(promptPage.routeOnlyContract());
    }

    @Test
    public void routeBackedContractWithMissingLocatorAssertionsIsPromptEligibleForCoverageGaps() {
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

        Assert.assertTrue(promptPage.eligible());
        Assert.assertTrue(promptPage.routeOnlyContract());
        Assert.assertTrue(promptPage.reasons().stream().anyMatch(reason -> reason.contains("coverage gaps")));
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
        return new AiPageObjectPromptScope(
                evidence.targetPage(),
                evidence.targetPage(),
                null,
                context,
                context,
                List.of(),
                null,
                Map.of()
        );
    }
}
