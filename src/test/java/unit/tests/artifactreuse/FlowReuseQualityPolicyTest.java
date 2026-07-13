package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.flow.FlowActionType;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStatus;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStep;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.artifactreuse.flow.FlowEndpoint;
import ua.demo.agentlab.artifactreuse.flow.FlowState;

import java.util.List;

public class FlowReuseQualityPolicyTest {

    @Test
    public void rejectsFreshFlowUntilItHasSuccessfulRuntimeSmoke() {
        Assert.assertFalse(baseline().reuseQuality().reusable());
        Assert.assertTrue(baseline().reuseQuality().reasons().stream().anyMatch(reason -> reason.contains("successful runtime smoke")));
    }

    @Test
    public void acceptsHealthyFlowAndRejectsFlakyOne() {
        FlowContract healthy = withQuality(baseline(), "2026-07-12T09:00:00Z", 1.0d, 0.0d, 5, 5, "PASSED");
        FlowContract flaky = withQuality(baseline(), "2026-07-12T09:00:00Z", 0.88d, 0.12d, 25, 22, "PASSED");

        Assert.assertTrue(healthy.reuseQuality().reusable());
        Assert.assertFalse(flaky.reuseQuality().reusable());
        Assert.assertEquals(flaky.reuseQuality().reasons().size(), 2);
    }

    @Test
    public void acceptsLowSampleFlowWhenMostRecentRuntimeSmokePassed() {
        FlowContract warmup = withQuality(baseline(), "2026-07-12T09:00:00Z", 0.5d, 0.5d, 2, 1, "PASSED");

        Assert.assertTrue(warmup.reuseQuality().reusable());
    }

    @Test
    public void rejectsFlowWhenMostRecentRuntimeSmokeFailed() {
        FlowContract failedLast = withQuality(baseline(), "2026-07-12T09:00:00Z", 0.95d, 0.05d, 20, 19, "FAILED");

        Assert.assertFalse(failedLast.reuseQuality().reusable());
        Assert.assertTrue(failedLast.reuseQuality().reasons().contains("last runtime smoke failed"));
    }

    private FlowContract baseline() {
        return new FlowContract("flow-contract.v1", "AUTH_LOGIN_TO_DASHBOARD", "Authentication", FlowContractType.AUTHENTICATION,
                new FlowEndpoint("LoginPage", "/login"), new FlowEndpoint("DashboardPage", "/dashboard"),
                List.of(FlowState.UNAUTHENTICATED), List.of(FlowState.AUTHENTICATED),
                List.of(new FlowContractStep(1, FlowActionType.AUTHENTICATE, "LoginPage", "/login", "", false)),
                List.of("route visible"), List.of("REQ-1"), List.of("transition"), List.of(), 0.95d, FlowContractStatus.CONFIRMED);
    }

    private FlowContract withQuality(
            FlowContract source,
            String lastSmoke,
            double passRate,
            double flakyRate,
            int attempts,
            int passes,
            String lastStatus
    ) {
        return new FlowContract(source.schemaVersion(), source.flowId(), source.displayName(), source.type(), source.source(), source.target(),
                source.requiresStates(), source.producesStates(), source.steps(), source.assertions(), source.requirementIds(),
                source.evidence(), source.artifactIds(), source.contractFingerprint(), lastSmoke, passRate, flakyRate,
                attempts, passes, lastStatus, source.confidence(), source.status());
    }
}
