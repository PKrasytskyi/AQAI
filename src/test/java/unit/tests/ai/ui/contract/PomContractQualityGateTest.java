package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractQualityGate;
import ua.demo.agentlab.ai.ui.contract.PomContractQualityReport;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;

import java.util.List;

public class PomContractQualityGateTest {

    private final PomContractQualityGate gate = new PomContractQualityGate();

    @Test
    public void acceptsValidLoginContract() {
        PomContractQualityReport report = gate.validate(loginContract());

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
    }

    @Test
    public void blocksStepUsingUnknownLocator() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(locator("usernameInput")),
                List.of(new PomActionSpec(
                        "clickLoginButton",
                        List.of(),
                        List.of(new PomStepSpec(PomStepAction.CLICK, "missingLoginButton", "", "", ""))
                )),
                List.of(),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream().anyMatch(issue -> "POM_STEP_LOCATOR_EXISTS".equals(issue.ruleId())));
    }

    @Test
    public void blocksProtectedPageLogoutWithoutUserMenuPrerequisiteWhenMenuEvidenceExists() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard/index", "AUTHENTICATED_AREA", "openDashboard"),
                List.of(
                        confirmedLocator("userMenu", "User menu", "css", ".oxd-userdropdown-tab", "button"),
                        confirmedLocator("logoutLink", "Logout", "css", "a[href*='logout']", "link")
                ),
                List.of(new PomActionSpec(
                        "logout",
                        List.of(),
                        List.of(new PomStepSpec(PomStepAction.CLICK, "logoutLink", "", "", ""))
                )),
                List.of(new PomAssertionSpec(
                        "isLogoutActionVisible",
                        "boolean",
                        List.of(new PomCheckSpec(PomCheckType.VISIBLE, "logoutLink", "", "", "", "")),
                        "AND"
                )),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream()
                .anyMatch(issue -> "POM_PROTECTED_PAGE_LOGOUT_SEQUENCE".equals(issue.ruleId())));
    }

    @Test
    public void acceptsProtectedPageDirectLogoutWhenNoUserMenuEvidenceExists() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("SecureAreaPage", "/secure", "AUTHENTICATED_AREA", "openSecureArea"),
                List.of(confirmedLocator("logoutLink", "Logout", "css", "a[href='/logout']", "link")),
                List.of(new PomActionSpec(
                        "logout",
                        List.of(),
                        List.of(new PomStepSpec(PomStepAction.CLICK, "logoutLink", "", "", ""))
                )),
                List.of(new PomAssertionSpec(
                        "isLogoutActionVisible",
                        "boolean",
                        List.of(new PomCheckSpec(PomCheckType.VISIBLE, "logoutLink", "", "", "", "")),
                        "AND"
                )),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
    }

    @Test
    public void acceptsProtectedPageLogoutWithUserMenuPrerequisite() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard/index", "AUTHENTICATED_AREA", "openDashboard"),
                List.of(
                        confirmedLocator("userMenu", "User menu", "css", ".oxd-userdropdown-tab", "button"),
                        confirmedLocator("logoutLink", "Logout", "css", "a[href*='logout']", "link")
                ),
                List.of(
                        new PomActionSpec(
                                "openUserMenu",
                                List.of(),
                                List.of(new PomStepSpec(PomStepAction.CLICK, "userMenu", "", "", ""))
                        ),
                        new PomActionSpec(
                                "logout",
                                List.of(),
                                List.of(
                                        new PomStepSpec(PomStepAction.CLICK, "userMenu", "", "", ""),
                                        new PomStepSpec(PomStepAction.CLICK, "logoutLink", "", "", "")
                                )
                        )
                ),
                List.of(new PomAssertionSpec(
                        "isLogoutActionVisible",
                        "boolean",
                        List.of(new PomCheckSpec(PomCheckType.VISIBLE, "logoutLink", "", "", "", "")),
                        "AND"
                )),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertFalse(report.hasBlockingIssues(), report.issues().toString());
    }

    @Test
    public void blocksDropdownMenuItemAsUserMenuTrigger() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard/index", "AUTHENTICATED_AREA", "openDashboard"),
                List.of(
                        confirmedLocator("userMenuTrigger", "User menu trigger", "css", "a.oxd-userdropdown-link", "button"),
                        confirmedLocator("logoutLink", "Logout", "css", "a[href*='logout']", "link")
                ),
                List.of(new PomActionSpec(
                        "logout",
                        List.of(),
                        List.of(
                                new PomStepSpec(PomStepAction.CLICK, "userMenuTrigger", "", "", ""),
                                new PomStepSpec(PomStepAction.CLICK, "logoutLink", "", "", "")
                        )
                )),
                List.of(),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream()
                .anyMatch(issue -> "POM_PROTECTED_PAGE_MENU_TRIGGER_LOCATOR".equals(issue.ruleId())));
    }

    @Test
    public void blocksRequirementSentenceAsAssertionExpectedValue() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(confirmedLocator("usernameInput", "Username", "id", "username", "input")),
                List.of(),
                List.of(new PomAssertionSpec(
                        "isUsernameFieldVisible",
                        "boolean",
                        List.of(new PomCheckSpec(
                                PomCheckType.TEXT_CONTAINS,
                                "usernameInput",
                                "Username field is visible on the login page.",
                                "",
                                "",
                                ""
                        )),
                        "AND"
                )),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream()
                .anyMatch(issue -> "POM_NO_REQUIREMENT_SENTENCE_ASSERTION".equals(issue.ruleId())));
    }

    @Test
    public void blocksFallbackLocatorEvidence() {
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(new PomLocatorSpec(
                        "loginButton",
                        "Login button",
                        "css",
                        "button[type='submit']",
                        "button",
                        0.76d,
                        "FALLBACK_LOCATOR",
                        true,
                        true,
                        List.of("fallback-locator:page-model")
                )),
                List.of(new PomActionSpec(
                        "clickLoginButton",
                        List.of(),
                        List.of(new PomStepSpec(PomStepAction.CLICK, "loginButton", "", "", ""))
                )),
                List.of(),
                List.of(),
                List.of()
        );

        PomContractQualityReport report = gate.validate(contract);

        Assert.assertTrue(report.hasBlockingIssues());
        Assert.assertTrue(report.issues().stream()
                .anyMatch(issue -> "POM_NO_FALLBACK_LOCATOR".equals(issue.ruleId())
                        || "POM_LOCATOR_PROVENANCE_CONFIRMED".equals(issue.ruleId())));
    }

    private PomContractSpec loginContract() {
        return new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(locator("usernameInput"), locator("passwordInput"), locator("loginButton")),
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
                        "isLoginFormVisible",
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

    private PomLocatorSpec locator(String id) {
        return new PomLocatorSpec(id, id, "id", id, "input", 0.9d);
    }

    private PomLocatorSpec confirmedLocator(String id, String elementName, String strategy, String value, String role) {
        return new PomLocatorSpec(
                id,
                elementName,
                strategy,
                value,
                role,
                0.9d,
                "CONFIRMED_LOCATOR",
                true,
                true,
                List.of("test-confirmed")
        );
    }
}
