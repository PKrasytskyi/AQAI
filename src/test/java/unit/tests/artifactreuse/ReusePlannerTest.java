package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.flow.FlowActionType;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStatus;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStep;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.artifactreuse.flow.FlowEndpoint;
import ua.demo.agentlab.artifactreuse.flow.FlowState;
import ua.demo.agentlab.artifactreuse.planner.FlowSemanticCandidate;
import ua.demo.agentlab.artifactreuse.planner.FlowSemanticCandidateBundle;
import ua.demo.agentlab.artifactreuse.planner.ReuseDecisionType;
import ua.demo.agentlab.artifactreuse.planner.ReusePlanner;
import ua.demo.agentlab.artifactreuse.planner.ReusePlannerInput;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.List;

public class ReusePlannerTest {

    @Test
    public void reusesOnlyNeo4jConfirmedSemanticFlowCandidate() {
        FlowContract flow = authenticationFlow();
        var result = new ReusePlanner().plan(new ReusePlannerInput(
                new CanonicalTestCaseBundle("fixture", "LoginPage", List.of("LoginPage", "DashboardPage"), List.of(testCase())),
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of(flow)),
                new FlowSemanticCandidateBundle(List.of(new FlowSemanticCandidate(flow.flowId(), 0.93d, true, flow, "qdrant+neo4j")), true, true, "", List.of()),
                true, true));

        Assert.assertEquals(result.stableReuseCount(), 1);
        Assert.assertEquals(result.plans().get(0).preconditions().get(0).decision(), ReuseDecisionType.REUSE_STABLE);
    }

    @Test
    public void keepsCurrentRunFlowAsEvidenceButDoesNotPretendItIsReused() {
        FlowContract flow = authenticationFlow();
        var result = new ReusePlanner().plan(new ReusePlannerInput(
                new CanonicalTestCaseBundle("fixture", "LoginPage", List.of("LoginPage", "DashboardPage"), List.of(testCase())),
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of(flow)),
                FlowSemanticCandidateBundle.unavailable("semantic reuse is disabled"), true, true));

        Assert.assertEquals(result.stableReuseCount(), 0);
        Assert.assertTrue(result.plans().get(0).notes().stream().anyMatch(note -> note.contains("Current-run confirmed")));
    }

    @Test
    public void routesUnverifiedSemanticCandidateToReviewInsteadOfReuse() {
        var result = new ReusePlanner().plan(new ReusePlannerInput(
                new CanonicalTestCaseBundle("fixture", "LoginPage", List.of("LoginPage", "DashboardPage"), List.of(testCase())),
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of()),
                new FlowSemanticCandidateBundle(List.of(new FlowSemanticCandidate("AUTHENTICATION_LoginPage_TO_DashboardPage",
                        FlowContractType.AUTHENTICATION, "/login", "/dashboard", 0.91d,
                        false, null, "qdrant-unverified")), true, false, "", List.of()),
                true, true));

        Assert.assertEquals(result.needsReviewCount(), 1);
        Assert.assertEquals(result.plans().get(0).preconditions().get(0).decision(), ReuseDecisionType.NEEDS_REVIEW);
    }

    @Test
    public void doesNotSpreadAnUnverifiedLogoutCandidateToAuthenticationRequirements() {
        var result = new ReusePlanner().plan(new ReusePlannerInput(
                new CanonicalTestCaseBundle("fixture", "LoginPage", List.of("LoginPage", "DashboardPage"), List.of(testCase())),
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of()),
                new FlowSemanticCandidateBundle(List.of(new FlowSemanticCandidate("LOGOUT_DashboardPage_TO_LoginPage", 0.91d,
                        false, null, "qdrant-unverified")), true, false, "", List.of()),
                true, true));

        Assert.assertEquals(result.needsReviewCount(), 0);
        Assert.assertEquals(result.discoveryCount(), 1);
    }

    @Test
    public void exposesTheNeo4jVerificationFailureInsteadOfAGenericReuseReason() {
        var result = new ReusePlanner().plan(new ReusePlannerInput(
                new CanonicalTestCaseBundle("fixture", "LoginPage", List.of("LoginPage", "DashboardPage"), List.of(testCase())),
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of()),
                new FlowSemanticCandidateBundle(List.of(new FlowSemanticCandidate("AUTHENTICATION_LoginPage_TO_DashboardPage",
                        FlowContractType.AUTHENTICATION, "/login", "/dashboard", 0.91d,
                        false, null, "qdrant-neo4j-verification-failed")), true, false, "",
                        List.of("Neo4j exact flow lookup failed: authentication failed")),
                true, true));

        Assert.assertEquals(result.plans().get(0).preconditions().get(0).reason(),
                "Neo4j exact flow lookup failed: authentication failed");
    }

    private FlowContract authenticationFlow() {
        FlowContract baseline = new FlowContract("flow-contract.v1", "AUTH_LOGIN_TO_DASHBOARD", "Authentication", FlowContractType.AUTHENTICATION,
                new FlowEndpoint("LoginPage", "/login"), new FlowEndpoint("DashboardPage", "/dashboard"),
                List.of(FlowState.UNAUTHENTICATED), List.of(FlowState.AUTHENTICATED),
                List.of(new FlowContractStep(1, FlowActionType.AUTHENTICATE, "LoginPage", "/login", "", false)),
                List.of("route visible"), List.of("REQ-1"), List.of("transition"), List.of(), 0.95d, FlowContractStatus.CONFIRMED);
        return new FlowContract("flow-contract.v1", baseline.flowId(), baseline.displayName(), baseline.type(),
                baseline.source(), baseline.target(), baseline.requiresStates(), baseline.producesStates(), baseline.steps(),
                baseline.assertions(), baseline.requirementIds(), baseline.evidence(), baseline.artifactIds(),
                baseline.contractFingerprint(), "2026-07-12T09:00:00Z", 1.0d, 0.0d, baseline.confidence(), baseline.status());
    }

    private CanonicalTestCase testCase() {
        return new CanonicalTestCase("REQ-1", "Authenticate", List.of("REQ-1"), List.of(),
                List.of(new UiOperationIntent(UiOperationKind.AUTHENTICATE, "LoginPage", null)), List.of(), List.of("DashboardPage"),
                new UiScenarioPrerequisite("LoginPage", "/login", false, List.of()), "", "", "LoginPage", "DashboardPage",
                "/login", "/dashboard", "", UiAssertionProfile.BASIC, List.of("Authenticate"), List.of("route visible"), List.of(), "fixture");
    }
}
