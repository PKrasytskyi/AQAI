package ua.demo.agentlab.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.prompt.AiPromptContextFormatter;
import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

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
}
