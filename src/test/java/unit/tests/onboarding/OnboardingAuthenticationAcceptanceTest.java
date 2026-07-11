package unit.tests.onboarding;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.source.FileRequirementSource;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.testcase.planning.ScenarioPipelineRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedFileValidation;
import ua.demo.agentlab.validation.ValidationStatus;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeService;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;
import ua.demo.agentlab.validation.smoke.LiveSmokePlan;
import ua.demo.agentlab.validation.smoke.LiveSmokePlanResolver;

import java.util.List;

public class OnboardingAuthenticationAcceptanceTest {

    @Test
    public void theInternetProfileCanReachPomJavaAndSmokeValidationWithoutProjectSpecificPages() {
        ProjectProfile profile = theInternetProfile();
        NormalizedRequirementBundle requirements = new RuleBasedRequirementNormalizer().normalize(
                new FileRequirementSource().load(new RequirementInput(
                        SourceType.FILE,
                        "requirements/the-internet-valid-login-requirement.md"
                ))
        );
        MappedUiKnowledge discovery = syntheticDiscovery();

        CanonicalTestCaseBundle testCases = new ScenarioPipelineRequirementToTestCaseGenerator().generate(
                new RequirementToTestCaseInput(profile, requirements, discovery, null)
        );

        Assert.assertFalse(testCases.testCases().isEmpty(), "Requirements must produce canonical UI test cases");
        Assert.assertTrue(testCases.pageNames().stream().anyMatch(page -> page.contains("Authentication")
                        || page.contains("Login")),
                "Authentication source page should be present in canonical planning");
        Assert.assertTrue(testCases.pageNames().stream().anyMatch(page -> page.contains("Secure")
                        || page.contains("Authenticated")),
                "Authenticated target page should be present in canonical planning");

        List<PomContractSpec> contracts = List.of(authenticationContract(), secureAreaContract());
        List<GeneratedSourceFile> files = new DeterministicPomJavaWriter(profile.outputProfile().generatedPagesPackage())
                .write(contracts);
        GeneratedUiSources sources = new GeneratedUiSources(files, List.of());

        LiveSmokePlan smokePlan = withProfile("profiles/the-internet.project-profile.yaml",
                () -> new LiveSmokePlanResolver().resolve(sources));
        Assert.assertEquals(smokePlan.sourceRoute(), "/login");
        Assert.assertEquals(smokePlan.targetRoute(), "/secure");
        Assert.assertEquals(smokePlan.sourcePage().className(), "AuthenticationPage");
        Assert.assertEquals(smokePlan.targetPage().className(), "SecureAreaPage");

        GeneratedUiSmokeResult smoke = new GeneratedUiSmokeService().smoke(
                sources,
                files.stream().map(GeneratedSourceFile::relativePath).toList(),
                passedCompile(files),
                passedReview(files)
        );

        Assert.assertEquals(smoke.status(), GeneratedUiSmokeStatus.PASSED);
        Assert.assertTrue(files.stream().map(GeneratedSourceFile::content)
                .noneMatch(content -> content.contains("ListingPage")
                        || content.contains("DetailsPage")
                        || content.contains("CartPage")));
    }

    private ProjectProfile theInternetProfile() {
        return new ProjectProfile(
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
                new OutputProfile("ua.demo.agentlab.ui.generated.pages", "ua.demo.agentlab.ui.generated.tests")
        );
    }

    private MappedUiKnowledge syntheticDiscovery() {
        return new MappedUiKnowledge(
                List.of(
                        new MappedPage(
                                "login",
                                "AuthenticationPage",
                                "authentication",
                                "https://the-internet.herokuapp.com/login",
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
                        ),
                        new MappedPage(
                                "secure",
                                "SecureAreaPage",
                                "authenticated-area",
                                "https://the-internet.herokuapp.com/secure",
                                "/secure",
                                "Secure Area",
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                null,
                                "",
                                ""
                        )
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private PomContractSpec authenticationContract() {
        return new PomContractSpec(
                LlmOutputSchemaVersion.POM_CONTRACT,
                new PomPageSpec("AuthenticationPage", "/login", "AUTHENTICATION", "openAuthentication"),
                List.of(
                        locator("usernameInput", "username input", "id", "username", "input"),
                        locator("passwordInput", "password input", "id", "password", "password"),
                        locator("loginButton", "login button", "css", "button[type='submit']", "button")
                ),
                List.of(new PomActionSpec(
                        "login",
                        List.of(
                                new AiMethodParameterSpec("String", "username"),
                                new AiMethodParameterSpec("String", "password")
                        ),
                        List.of(
                                new PomStepSpec(PomStepAction.CLEAR_AND_TYPE, "usernameInput", "username", "", ""),
                                new PomStepSpec(PomStepAction.CLEAR_AND_TYPE, "passwordInput", "password", "", ""),
                                new PomStepSpec(PomStepAction.CLICK, "loginButton", "", "", "")
                        )
                )),
                List.of(new PomAssertionSpec(
                        "isAuthenticationFormVisible",
                        "boolean",
                        List.of(
                                new PomCheckSpec(PomCheckType.VISIBLE, "usernameInput", "", "", "", ""),
                                new PomCheckSpec(PomCheckType.VISIBLE, "passwordInput", "", "", "", ""),
                                new PomCheckSpec(PomCheckType.VISIBLE, "loginButton", "", "", "", "")
                        ),
                        "AND"
                )),
                List.of(),
                List.of()
        );
    }

    private PomContractSpec secureAreaContract() {
        return new PomContractSpec(
                LlmOutputSchemaVersion.POM_CONTRACT,
                new PomPageSpec("SecureAreaPage", "/secure", "AUTHENTICATED_AREA", "openSecureArea"),
                List.of(
                        locator("secureAreaHeading", "secure area heading", "css", "h2", "heading"),
                        locator("logoutLink", "logout link", "css", "a[href='/logout']", "link")
                ),
                List.of(new PomActionSpec(
                        "logout",
                        List.of(),
                        List.of(new PomStepSpec(PomStepAction.CLICK, "logoutLink", "", "", ""))
                )),
                List.of(
                        new PomAssertionSpec(
                                "isSecureAreaRouteVisible",
                                "boolean",
                                List.of(new PomCheckSpec(PomCheckType.URL_CONTAINS, "", "/secure", "", "", "/secure")),
                                "AND"
                        ),
                        new PomAssertionSpec(
                                "isLogoutActionVisible",
                                "boolean",
                                List.of(new PomCheckSpec(PomCheckType.VISIBLE, "logoutLink", "", "", "", "")),
                                "AND"
                        )
                ),
                List.of(),
                List.of()
        );
    }

    private PomLocatorSpec locator(String id, String elementName, String strategy, String value, String role) {
        return new PomLocatorSpec(id, elementName, strategy, value, role, 0.90d,
                "CONFIRMED_LOCATOR", true, true, List.of("acceptance-test"));
    }

    private GeneratedCodeValidationResult passedCompile(List<GeneratedSourceFile> files) {
        return new GeneratedCodeValidationResult(
                ValidationStatus.PASSED,
                "Compiled successfully",
                "",
                files.stream()
                        .map(file -> new GeneratedFileValidation(file.relativePath(), ValidationStatus.PASSED, "ok"))
                        .toList()
        );
    }

    private GeneratedCodeReviewReport passedReview(List<GeneratedSourceFile> files) {
        return new GeneratedCodeReviewReport("No critical findings", files.size(), 0, List.of());
    }

    private LiveSmokePlan withProfile(String profileFile, ProfileAction action) {
        String previous = System.getProperty("project.profile.file");
        System.setProperty("project.profile.file", profileFile);
        try {
            return action.run();
        } finally {
            if (previous == null) {
                System.clearProperty("project.profile.file");
            } else {
                System.setProperty("project.profile.file", previous);
            }
        }
    }

    @FunctionalInterface
    private interface ProfileAction {
        LiveSmokePlan run();
    }
}
