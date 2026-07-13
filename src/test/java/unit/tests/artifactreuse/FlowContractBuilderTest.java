package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.flow.FlowActionType;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBuilder;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBuilderInput;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStatus;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.artifactreuse.flow.FlowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageStateHints;
import ua.demo.agentlab.ui.flow.model.CanonicalFlow;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

import java.util.List;

public class FlowContractBuilderTest {

    private final FlowContractBuilder builder = new FlowContractBuilder();

    @Test
    public void buildsConfirmedAuthenticationAndLogoutContractsFromGenericEvidence() {
        var bundle = builder.build(new FlowContractBuilderInput(
                new CanonicalTestCaseBundle("requirements", "LoginPage", List.of("LoginPage", "DashboardPage"), List.of(
                        testCase("REQ-LOGIN", "Authenticate user", "LoginPage", "/login", "DashboardPage", "/dashboard",
                                List.of(new UiOperationIntent(UiOperationKind.AUTHENTICATE, "LoginPage", null))),
                        testCase("REQ-LOGOUT", "User can logout", "DashboardPage", "/dashboard", "LoginPage", "/login",
                                List.of(new UiOperationIntent(UiOperationKind.LOGOUT, "DashboardPage", null)))
                )),
                flows(),
                new MappedUiKnowledge(
                        List.of(page("LoginPage", "/login", List.of(form())), page("DashboardPage", "/dashboard", List.of())),
                        List.of(
                                new MappedTransition("login", "loginpage", "submit", "dashboardpage", "/dashboard", "AUTHENTICATE", true, 0.95d),
                                new MappedTransition("logout", "dashboardpage", "logout", "loginpage", "/login", "LOGOUT", true, 0.95d)
                        ), List.of(), List.of(), List.of()
                ),
                null
        ));

        var authentication = bundle.contracts().stream()
                .filter(contract -> contract.type() == FlowContractType.AUTHENTICATION)
                .findFirst().orElseThrow();
        var logout = bundle.contracts().stream()
                .filter(contract -> contract.type() == FlowContractType.LOGOUT)
                .findFirst().orElseThrow();

        Assert.assertEquals(authentication.status(), FlowContractStatus.CONFIRMED);
        Assert.assertTrue(authentication.requiresStates().contains(FlowState.UNAUTHENTICATED));
        Assert.assertTrue(authentication.producesStates().contains(FlowState.AUTHENTICATED));
        Assert.assertEquals(logout.status(), FlowContractStatus.CONFIRMED);
        Assert.assertTrue(logout.requiresStates().contains(FlowState.AUTHENTICATED));
        Assert.assertTrue(logout.producesStates().contains(FlowState.UNAUTHENTICATED));
    }

    @Test
    public void groupsFieldEntryAndPreservesDataKeysForAFormContract() {
        var bundle = builder.build(new FlowContractBuilderInput(
                new CanonicalTestCaseBundle("requirements", "LoginPage", List.of("LoginPage"), List.of(
                        testCase("REQ-USER", "Enter username", "LoginPage", "/login", "LoginPage", "/login",
                                List.of(new UiOperationIntent(UiOperationKind.ENTER_TEXT, "LoginPage", "username"))),
                        testCase("REQ-PASS", "Enter password", "LoginPage", "/login", "LoginPage", "/login",
                                List.of(new UiOperationIntent(UiOperationKind.ENTER_TEXT, "LoginPage", "password"))),
                        testCase("REQ-SUBMIT", "Submit login form", "LoginPage", "/login", "DashboardPage", "/dashboard",
                                List.of(new UiOperationIntent(UiOperationKind.SUBMIT_FORM, "LoginPage", null)))
                )),
                new CanonicalPageFlowModel("project", "Project", List.of(), List.of()),
                new MappedUiKnowledge(List.of(page("LoginPage", "/login", List.of(form()))), List.of(), List.of(), List.of(), List.of()),
                null
        ));

        var entry = bundle.contracts().stream()
                .filter(contract -> contract.type() == FlowContractType.FORM_ENTRY)
                .findFirst().orElseThrow();
        var submit = bundle.contracts().stream()
                .filter(contract -> contract.type() == FlowContractType.FORM_SUBMISSION)
                .findFirst().orElseThrow();

        Assert.assertEquals(entry.status(), FlowContractStatus.CONFIRMED);
        Assert.assertEquals(entry.steps().size(), 2);
        Assert.assertTrue(entry.steps().stream().allMatch(step -> step.action() == FlowActionType.ENTER_TEXT));
        Assert.assertTrue(entry.steps().stream().anyMatch(step -> "username".equals(step.dataKey())));
        Assert.assertTrue(entry.steps().stream().anyMatch(step -> "password".equals(step.dataKey())));
        Assert.assertEquals(submit.status(), FlowContractStatus.CONFIRMED);
        Assert.assertTrue(submit.producesStates().contains(FlowState.FORM_SUBMITTED));
    }

    private CanonicalPageFlowModel flows() {
        return new CanonicalPageFlowModel("project", "Project", List.of(), List.of(
                new CanonicalFlow("auth", "Authenticate", "AUTHENTICATE", "LoginPage", "/login",
                        "DashboardPage", "/dashboard", false, List.of(), List.of(), List.of(), List.of()),
                new CanonicalFlow("logout", "Logout", "LOGOUT", "DashboardPage", "/dashboard",
                        "LoginPage", "/login", true, List.of(), List.of(), List.of(), List.of())
        ));
    }

    private CanonicalTestCase testCase(
            String id,
            String title,
            String sourcePage,
            String sourceRoute,
            String page,
            String route,
            List<UiOperationIntent> operations
    ) {
        return new CanonicalTestCase(id, title, List.of(id), List.of(), operations, List.of(), List.of(page),
                new UiScenarioPrerequisite(sourcePage, sourceRoute, false, List.of()), "", "", sourcePage, page,
                sourceRoute, route, "", UiAssertionProfile.BASIC, List.of(title), List.of("expected"), List.of(), "fixture");
    }

    private MappedPage page(String name, String route, List<MappedForm> forms) {
        return new MappedPage(name.toLowerCase(), name, "page", route, route, name, List.of(), List.of(), forms,
                List.of(), List.of(), new PageStateHints(false, false, false, false, !forms.isEmpty(), false), "", "");
    }

    private MappedForm form() {
        return new MappedForm("form", "login", "", List.of(), List.of());
    }
}
