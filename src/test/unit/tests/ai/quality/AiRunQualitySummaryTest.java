package unit.tests.ai.quality;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.quality.AiRunQualitySummary;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryInput;
import ua.demo.agentlab.ai.quality.AiRunQualitySummaryService;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;
import java.util.Map;

public class AiRunQualitySummaryTest {

    @Test
    public void summarySanitizesNonFiniteAverageLocatorScore() {
        AiRunQualitySummary summary = new AiRunQualitySummary(
                "run",
                1,
                1,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                Double.NaN,
                100
        );

        Assert.assertEquals(summary.averageLocatorScore(), 0.0d);
    }

    @Test
    public void locatorCandidateSanitizesNonFiniteStabilityScore() {
        LocatorCandidate candidate = new LocatorCandidate(
                LocatorStrategy.CSS,
                "#login",
                Double.NaN,
                "test",
                "button",
                "Login",
                "Login",
                "",
                "example.test",
                true,
                true,
                true,
                List.of()
        );

        Assert.assertEquals(candidate.stabilityScore(), 0.0d);
    }

    @Test
    public void qualityScorePenalizesMissingPromptAllowedLocators() {
        CanonicalTestCaseBundle bundle = new CanonicalTestCaseBundle(
                "test",
                "LoginPage",
                List.of("LoginPage"),
                List.of(new CanonicalTestCase(
                        "REQ-001",
                        "Login page opens",
                        List.of("REQ-001"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("LoginPage"),
                        null,
                        "",
                        "",
                        "HomePage",
                        "LoginPage",
                        "/",
                        "/login",
                        "",
                        null,
                        List.of("Open login page"),
                        List.of("Login form is visible"),
                        List.of(),
                        "requirements/login.md [L1]"
                ))
        );
        AiRunQualitySummary summary = new AiRunQualitySummaryService().summarize(new AiRunQualitySummaryInput(
                null,
                null,
                bundle,
                MappedUiKnowledge.empty(),
                Map.of(
                        "test.case.expectation.resolved.count", "1",
                        "prompt.ui.evidence.locator.count", "0"
                )
        ));

        Assert.assertTrue(
                summary.qualityScore() <= 55,
                "Missing prompt allowed locators must materially reduce the run quality score"
        );
    }

    @Test
    public void summarySeparatesMappedPagesFromGeneratedPomPrompts() {
        AiRunQualitySummary summary = new AiRunQualitySummaryService().summarize(new AiRunQualitySummaryInput(
                null,
                null,
                null,
                new MappedUiKnowledge(
                        List.of(
                                new ua.demo.agentlab.ui.discovery.mapping.model.MappedPage(
                                        "login", "LoginPage", "authentication", "/login", "/login", "Login",
                                        List.of(), List.of(), List.of(), List.of(), List.of(), null, "", ""
                                ),
                                new ua.demo.agentlab.ui.discovery.mapping.model.MappedPage(
                                        "dashboard", "DashboardPage", "dashboard", "/dashboard", "/dashboard", "Dashboard",
                                        List.of(), List.of(), List.of(), List.of(), List.of(), null, "", ""
                                ),
                                new ua.demo.agentlab.ui.discovery.mapping.model.MappedPage(
                                        "profile", "ProfilePage", "details", "/profile", "/profile", "Profile",
                                        List.of(), List.of(), List.of(), List.of(), List.of(), null, "", ""
                                )
                        ),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                Map.of(
                        "openai.page.object.scoped.requests", "2",
                        "ai.page.object.prompt.LoginPage.allowedLocators", "3",
                        "ai.page.object.prompt.DashboardPage.allowedLocators", "0"
                )
        ));

        Assert.assertEquals(summary.mappedPages(), 3);
        Assert.assertEquals(summary.pageObjectPrompts(), 2);
        Assert.assertEquals(summary.promptPagesWithoutAllowedLocators(), 1);
    }
}
