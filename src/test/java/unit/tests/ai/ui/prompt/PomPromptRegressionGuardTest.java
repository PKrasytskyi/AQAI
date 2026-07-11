package unit.tests.ai.ui.prompt;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.AiPageObjectPromptBuilder;
import ua.demo.agentlab.ai.ui.prompt.PageObjectPromptMode;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;
import java.util.Locale;

public class PomPromptRegressionGuardTest {

    @Test
    public void unrelatedAuthenticationPromptDoesNotLeakLegacyPageOrRouteFallbackVocabulary() {
        AiContextPackage context = contextForUnrelatedAuthenticationProject();
        AiPageObjectSpec baseline = new AiPageObjectSpec("AuthenticationPage", "/login", "openAuthentication", List.of(), List.of());

        String prompt = new AiPageObjectPromptBuilder(PageObjectPromptMode.COMPACT)
                .buildForPage(context, "AuthenticationPage", List.of(), baseline);
        String normalized = prompt.toLowerCase(Locale.ROOT);

        for (String forbidden : List.of(
                "listingpage",
                "detailspage",
                "cartpage",
                "catalog=",
                "products=",
                "details=",
                "cart=",
                "/profile",
                "a[href*='/products",
                "a[href*='/cart",
                "openEntityDetails",
                "openTargetContainer",
                "addEntityToContainer",
                "removeEntityFromContainer"
        )) {
            Assert.assertFalse(normalized.contains(forbidden.toLowerCase(Locale.ROOT)),
                    "POM prompt leaked forbidden fallback vocabulary: " + forbidden + System.lineSeparator() + prompt);
        }
        Assert.assertTrue(prompt.contains("AUTHENTICATION=/login"));
        Assert.assertTrue(prompt.contains("AUTHENTICATED_AREA=/secure"));
    }

    private AiContextPackage contextForUnrelatedAuthenticationProject() {
        ProjectProfile profile = new ProjectProfile(
                "the-internet",
                "The Internet Herokuapp",
                "https://the-internet.herokuapp.com",
                "/",
                "/login",
                "",
                "/secure",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/the-internet-valid-login-requirement.md",
                List.of(new NormalizedRequirement(
                        "REQ-999",
                        "Account profile text must not create a profile page",
                        "Account profile wording is governance context and is not an explicit route.",
                        "",
                        true,
                        false,
                        List.of("page-ownership-expectations"),
                        new SourceReference("requirements/the-internet-valid-login-requirement.md", 1, 1, "")
                )),
                List.of(),
                List.of()
        );
        PromptUiEvidence evidence = new PromptUiEvidence(
                "AuthenticationPage",
                "/login",
                false,
                List.of(),
                List.of("REQ-001"),
                List.of(
                        new PromptActionEvidence("username:TYPE", "semantic-action", "AuthenticationPage", "REQ-001"),
                        new PromptActionEvidence("password:TYPE", "semantic-action", "AuthenticationPage", "REQ-001"),
                        new PromptActionEvidence("loginButton:CLICK", "semantic-action", "AuthenticationPage", "REQ-001")
                ),
                List.of(
                        new PromptAssertionEvidence("URL_CONTAINS", "/login", "AuthenticationPage", "REQ-001", 1.0d),
                        new PromptAssertionEvidence("ELEMENT_VISIBLE", "usernameInput", "AuthenticationPage", "REQ-001", 0.95d)
                ),
                List.of(
                        locator("usernameInput", "username", "id", "username", "input"),
                        locator("passwordInput", "password", "id", "password", "password"),
                        locator("loginButton", "login button", "css", "button[type='submit']", "button")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("test"),
                0.95d
        );
        return new AiContextPackage(
                "Generate one scoped Page Object contract",
                requirements,
                null,
                profile,
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
    }

    private PromptLocatorEvidence locator(String id, String elementName, String strategy, String value, String role) {
        return new PromptLocatorEvidence(
                id,
                elementName,
                strategy,
                value,
                role,
                "",
                "",
                true,
                0.90d,
                "",
                "",
                1,
                1,
                true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of("test")
        );
    }
}
