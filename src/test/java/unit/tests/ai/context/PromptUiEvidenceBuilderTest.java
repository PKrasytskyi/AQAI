package unit.tests.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidenceBuilder;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.ui.prompt.AiPromptContextFormatter;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageEvidenceModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

import java.util.Map;
import java.util.List;

public class PromptUiEvidenceBuilderTest {

    @Test
    public void promptEvidenceUsesAllowedLocatorsAndCarriesExcludedEvidenceSeparately() {
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(loginPageWithPromotedLocator()),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        MappedUiKnowledgeCurated curated = new MappedUiKnowledgeCurated(
                knowledge,
                List.of(new ExcludedEvidence(
                        "raw",
                        "locator:element",
                        "login",
                        "external-origin",
                        "xpath=//a[@href='http://external.example/login']"
                )),
                List.of("test-curated"),
                0.90d
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                knowledge,
                curated,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptUiEvidence evidence = new PromptUiEvidenceBuilder().build(context);

        Assert.assertEquals(evidence.targetPage(), "LoginPage");
        Assert.assertEquals(evidence.targetRoute(), "/login");
        Assert.assertEquals(evidence.requiredLocators().size(), 1);
        Assert.assertEquals(evidence.requiredLocators().get(0).value(), "#username");
        Assert.assertTrue(evidence.excludedEvidence().stream()
                .anyMatch(excluded -> excluded.value().contains("external.example")));

        AiContextPackage promptContext = new AiContextPackage(
                context.objective(),
                context.normalizedRequirementBundle(),
                context.generationPolicy(),
                context.projectProfile(),
                context.testPlan(),
                context.canonicalTestCaseBundle(),
                context.uiTestPlan(),
                context.canonicalPageFlowModel(),
                context.mappedUiKnowledge(),
                context.mappedUiKnowledgeCurated(),
                context.pageModelBundle(),
                context.canonicalInteractionModel(),
                context.retrievalContext(),
                context.assertionContracts(),
                context.pageModelEnrichments(),
                context.templateCapabilities(),
                evidence
        );
        String promptEvidenceSummary = new AiPromptContextFormatter().summarizePromptUiEvidence(promptContext);
        Assert.assertFalse(promptEvidenceSummary.contains("external.example"));
        Assert.assertFalse(promptEvidenceSummary.contains("external-origin"));
        Assert.assertTrue(promptEvidenceSummary.contains("value=<redacted>"));
    }

    @Test
    public void promptEvidenceFallsBackToStableSameOriginPageModelLocators() {
        MappedPage dashboardPage = new MappedPage(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "authenticated-area",
                "https://example.test/web/index.php/dashboard/index",
                "/dashboard/index",
                "Dashboard",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(dashboardPage),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        PageModelBundle pageModelBundle = new PageModelBundle(List.of(new PageModel(
                "web-index-php-dashboard-index",
                "https://example.test/web/index.php/dashboard/index",
                "/dashboard/index",
                "Dashboard",
                "Dashboard",
                "authenticated-area",
                new PageEvidenceModel("", ""),
                List.of(
                        pageModelElement(
                                "dashboard-link",
                                "navigation-link",
                                "a",
                                "Dashboard",
                                "",
                                "",
                                "/web/index.php/dashboard/index",
                                new PageLocatorModel(
                                        "css",
                                        "a[href='/web/index.php/dashboard/index']",
                                        0.78d,
                                        "stable same-origin navigation link",
                                        false,
                                        3,
                                        3,
                                        true,
                                        1,
                                        1,
                                        "DashboardNavigation"
                                )
                        ),
                        pageModelElement(
                                "external-link",
                                "navigation-link",
                                "a",
                                "External",
                                "",
                                "",
                                "https://external.example/",
                                new PageLocatorModel(
                                        "xpath",
                                        "//a[normalize-space()='External']",
                                        0.80d,
                                        "external link",
                                        true,
                                        3,
                                        3,
                                        true,
                                        1,
                                        1,
                                        "DashboardNavigation"
                                )
                        )
                ),
                List.of(),
                List.of(),
                List.of()
        )));
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                knowledge,
                null,
                pageModelBundle,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptUiEvidence evidence = new PromptUiEvidenceBuilder().build(context);

        Assert.assertEquals(evidence.requiredLocators().size(), 1);
        Assert.assertEquals(evidence.requiredLocators().get(0).value(), "a[href='/web/index.php/dashboard/index']");
        Assert.assertEquals(evidence.requiredLocators().get(0).strategy(), "css");
        Assert.assertTrue(evidence.requiredLocators().get(0).sameOrigin());
    }

    @Test
    public void promptEvidenceUsesDbStableLocatorsAsConfirmedAllowedEvidence() {
        MappedPage dashboardPage = new MappedPage(
                "dashboard",
                "DashboardPage",
                "authenticated-area",
                "https://example.test/dashboard",
                "/dashboard",
                "Dashboard",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(dashboardPage),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        PromptLocatorEvidence dbLocator = new PromptLocatorEvidence(
                "dashboardHeading",
                "Dashboard heading",
                "css",
                "h6.oxd-text",
                "heading",
                "Dashboard",
                "",
                true,
                0.91d,
                "",
                "",
                1,
                1,
                true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of("db-page-id:dashboard", "db-route:/dashboard", "db-stable-locator:dashboard-heading")
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                knowledge,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(dbLocator),
                null
        );

        PromptUiEvidence evidence = new PromptUiEvidenceBuilder().build(context);

        Assert.assertEquals(evidence.requiredLocators().size(), 1);
        Assert.assertEquals(evidence.requiredLocators().get(0).value(), "h6.oxd-text");
        Assert.assertEquals(evidence.requiredLocators().get(0).evidenceType(), LocatorEvidenceType.CONFIRMED_LOCATOR);
        Assert.assertTrue(evidence.requiredLocators().get(0).sourceTrace().stream()
                .anyMatch(trace -> trace.startsWith("db-stable-locator:")));
    }

    @Test
    public void promptEvidenceIncludesSemanticBusinessIntentActions() {
        MappedPage loginPage = new MappedPage(
                "login",
                "LoginPage",
                "authentication",
                "https://example.test/login",
                "/login",
                "Login",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(loginPage),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        PageModelBundle pageModelBundle = new PageModelBundle(List.of(new PageModel(
                "login",
                "https://example.test/login",
                "/login",
                "Login",
                "Username Password Login",
                "authentication",
                new PageEvidenceModel("", ""),
                List.of(
                        pageModelElement(
                                "username",
                                "input",
                                "input",
                                "",
                                "username",
                                "username",
                                "",
                                new PageLocatorModel("name", "username", 0.88d, "name attribute", true, 3, 3, true)
                        ),
                        pageModelElement(
                                "password",
                                "password-input",
                                "input",
                                "",
                                "password",
                                "password",
                                "",
                                new PageLocatorModel("name", "password", 0.88d, "name attribute", true, 3, 3, true)
                        ),
                        pageModelElement(
                                "login-button",
                                "submit-button",
                                "button",
                                "Login",
                                "",
                                "login-button",
                                "",
                                new PageLocatorModel("css", "button[type='submit']", 0.82d, "submit button", true, 3, 3, true)
                        )
                ),
                List.of(),
                List.of(),
                List.of()
        )));
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                knowledge,
                null,
                pageModelBundle,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptUiEvidence evidence = new PromptUiEvidenceBuilder().build(context);

        Assert.assertTrue(evidence.requiredActions().stream()
                .anyMatch(action -> action.type().equals("semantic-business-intent")
                        && action.name().equals("AUTHENTICATION")));
        Assert.assertTrue(evidence.requiredActions().stream()
                .anyMatch(action -> action.name().contains("AUTHENTICATE")));
    }

    @Test
    public void promptEvidencePromotesPageEnrichmentStableUserMenuTriggerLocator() {
        MappedPage dashboard = new MappedPage(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "authenticated-area",
                "https://example.test/web/index.php/dashboard/index",
                "/dashboard/index",
                "Dashboard",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(dashboard),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        PageModelEnrichmentRecord enrichment = new PageModelEnrichmentRecord(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "/dashboard/index",
                "dashboard",
                "Dashboard with user menu",
                List.of("openUserMenu", "logout"),
                List.of(
                        "css=span.oxd-userdropdown-tab (stability=0.78, sameOrigin=true, element=User menu trigger, relevance=requirement, dependency=required-for-logout-menu-flow)",
                        "css=a.oxd-userdropdown-link (stability=0.78, sameOrigin=true, element=User menu trigger, relevance=requirement, dependency=required-for-logout-menu-flow)"
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("REQ-LOGOUT"),
                Map.of(),
                Map.of(),
                0.78d,
                "openai"
        );
        AiContextPackage context = new AiContextPackage(
                "Generate POM",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                knowledge,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(enrichment),
                List.of(),
                null
        );

        PromptUiEvidence evidence = new PromptUiEvidenceBuilder().build(context);

        Assert.assertTrue(evidence.requiredLocators().stream()
                .anyMatch(locator -> locator.fieldHint().equals("userMenuTrigger")
                        && locator.value().equals("span.oxd-userdropdown-tab")
                        && locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR));
        Assert.assertFalse(evidence.requiredLocators().stream()
                .anyMatch(locator -> locator.value().equals("a.oxd-userdropdown-link")));
    }

    private MappedPage loginPageWithPromotedLocator() {
        LocatorCandidate locator = new LocatorCandidate(
                LocatorStrategy.CSS,
                "#username",
                0.90d,
                "test",
                "textbox",
                "Username",
                "",
                "",
                "example.test",
                true,
                true,
                true,
                List.of()
        );
        return new MappedPage(
                "login",
                "LoginPage",
                "authentication",
                "https://example.test/login",
                "/login",
                "Login",
                List.of(),
                List.of(new MappedElement(
                        "username",
                        "Username",
                        "input",
                        "textbox",
                        "",
                        false,
                        true,
                        List.of(locator),
                        List.of("type"),
                        0.95d
                )),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
    }

    private PageElementModel pageModelElement(
            String elementId,
            String semanticType,
            String tag,
            String text,
            String name,
            String id,
            String href,
            PageLocatorModel locator
    ) {
        return new PageElementModel(
                elementId,
                tag,
                semanticType,
                tag,
                "",
                text,
                id,
                name,
                "",
                "",
                tag.equals("a") ? "link" : "",
                href,
                "",
                true,
                true,
                false,
                Map.of(),
                List.of(locator),
                locator,
                List.of(),
                0.90d
        );
    }
}
