package unit.tests.ai.ui.contract;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public class DeterministicPomJavaWriterTest {

    @Test
    public void adapterRendersJavaFromStructuredStepsOnly() {
        DeterministicPomJavaWriter writer = new DeterministicPomJavaWriter("ua.demo.agentlab.ui.generated.pages");
        PomContractSpec contract = loginContract();

        AiPageObjectSpec spec = writer.toAiPageObjectSpec(contract);
        List<GeneratedSourceFile> files = writer.write(List.of(contract));

        Assert.assertEquals(normalize(spec.methods().stream()
                .filter(method -> method.methodName().equals("login"))
                .findFirst()
                .orElseThrow()
                .body()), normalize("""
                elements.clearAndType(usernameInput, username);
                elements.clearAndType(passwordInput, password);
                elements.click(loginButton);""".strip()));
        Assert.assertEquals(files.size(), 1);
        String content = files.get(0).content();
        Assert.assertTrue(content.contains("public void login(String username, String password)"));
        Assert.assertTrue(content.contains("elements.clearAndType(usernameInput, username);"));
        Assert.assertFalse(content.contains("elements.type("));
        Assert.assertFalse(content.contains("elements.click(By.cssSelector"));
    }

    @Test
    public void writerRendersReusableComponentAndPageAccessor() {
        DeterministicPomJavaWriter writer = new DeterministicPomJavaWriter("ua.demo.agentlab.ui.generated.pages");
        PomContractSpec contract = dashboardContract();

        List<GeneratedSourceFile> files = writer.write(List.of(contract));

        Assert.assertEquals(files.size(), 2);
        String page = files.stream()
                .filter(file -> file.className().equals("DashboardPage"))
                .findFirst()
                .orElseThrow()
                .content();
        String component = files.stream()
                .filter(file -> file.className().equals("SidebarComponent"))
                .findFirst()
                .orElseThrow()
                .content();
        Assert.assertTrue(page.contains("public SidebarComponent sidebarComponent()"));
        Assert.assertTrue(page.contains("return new SidebarComponent(driver, runtimeConfig, sidebarRoot);"));
        Assert.assertTrue(component.contains("public class SidebarComponent extends BasePage"));
        Assert.assertTrue(component.contains("private WebElement child(By locator)"));
        Assert.assertTrue(component.contains("child(adminLink).click();"));
    }

    @Test
    public void writerEmitsOnlyReferencedPageLocators() {
        DeterministicPomJavaWriter writer = new DeterministicPomJavaWriter("ua.demo.agentlab.ui.generated.pages");
        PomContractSpec contract = new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(
                        new PomLocatorSpec("usernameInput", "username input", "name", "username", "input", 0.9d),
                        new PomLocatorSpec("password", "password duplicate", "name", "password", "password", 0.8d),
                        new PomLocatorSpec("passwordInput", "password input", "css", "input[type='password']", "password", 0.9d),
                        new PomLocatorSpec("loginButton", "login button", "css", "button[type='submit']", "button", 0.8d)
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
                List.of(),
                List.of(),
                List.of()
        );

        String content = writer.write(List.of(contract)).get(0).content();

        Assert.assertTrue(content.contains("private final By passwordInput"));
        Assert.assertFalse(content.contains("private final By password ="));
    }

    private PomContractSpec loginContract() {
        return new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION_FORM", "openLogin"),
                List.of(
                        new PomLocatorSpec("usernameInput", "username input", "id", "username", "input", 0.9d),
                        new PomLocatorSpec("passwordInput", "password input", "id", "password", "input", 0.9d),
                        new PomLocatorSpec("loginButton", "login button", "css", "button[type='submit']", "button", 0.8d)
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

    private PomContractSpec dashboardContract() {
        return new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("DashboardPage", "/dashboard/index", "DASHBOARD", "openDashboard"),
                List.of(new PomLocatorSpec(
                        "sidebarRoot",
                        "sidebar navigation",
                        "css",
                        "aside",
                        "navigation",
                        0.86d
                )),
                List.of(new PomComponentSpec(
                        "SidebarComponent",
                        "NAVIGATION",
                        "sidebarRoot",
                        List.of(
                                new PomLocatorSpec("adminLink", "admin link", "css", "a[href*='/admin']", "link", 0.84d),
                                new PomLocatorSpec("pimLink", "pim link", "css", "a[href*='/pim']", "link", 0.84d)
                        ),
                        List.of(new PomActionSpec(
                                "clickAdmin",
                                List.of(),
                                List.of(new PomStepSpec(PomStepAction.CLICK, "adminLink", "", "", ""))
                        )),
                        List.of(),
                        true
                )),
                List.of(),
                List.of(new PomAssertionSpec(
                        "isDashboardRouteVisible",
                        "boolean",
                        List.of(new PomCheckSpec(PomCheckType.URL_CONTAINS, "", "/dashboard/index", "", "", "/dashboard/index")),
                        "AND"
                )),
                List.of(),
                List.of()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n");
    }
}
