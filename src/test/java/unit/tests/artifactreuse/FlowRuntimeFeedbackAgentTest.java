package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBundle;
import ua.demo.agentlab.artifactreuse.flow.FlowContractRegistry;
import ua.demo.agentlab.artifactreuse.flow.FlowRuntimeFeedback;
import ua.demo.agentlab.artifactreuse.flow.FlowRuntimeFeedbackAgent;
import ua.demo.agentlab.artifactreuse.flow.FlowRuntimeFeedbackResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionResult;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionStatus;

import java.util.List;

public class FlowRuntimeFeedbackAgentTest {

    @Test
    public void doesNotTreatGeneratedSmokeAsRuntimeFlowEvidenceWhenLiveSmokeIsDisabled() {
        CapturingRegistry registry = new CapturingRegistry();
        FlowRuntimeFeedbackAgent agent = new FlowRuntimeFeedbackAgent(config(), registry);
        var result = agent.execute(new FlowRuntimeFeedbackAgent.Input(
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of(flow())),
                new GeneratedUiSmokeResult(GeneratedUiSmokeStatus.PASSED, "passed", 1, List.of()),
                passedExecution(), false, "SKIPPED"), null);

        Assert.assertFalse(result.attempted());
        Assert.assertNull(registry.feedback);
    }

    @Test
    public void recordsFailureWhenRequiredLiveSmokeIsNotPassed() {
        CapturingRegistry registry = new CapturingRegistry();
        FlowRuntimeFeedbackAgent agent = new FlowRuntimeFeedbackAgent(config(), registry);
        agent.execute(new FlowRuntimeFeedbackAgent.Input(
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of(flow())),
                new GeneratedUiSmokeResult(GeneratedUiSmokeStatus.PASSED, "passed", 1, List.of()),
                passedExecution(), true, "SKIPPED"), null);

        Assert.assertFalse(registry.feedback.smokePassed());
        Assert.assertEquals(registry.feedback.smokeSource(), "live-ui-smoke");
    }

    @Test
    public void promotesLiveVerifiedFlowBeforePersistingSuccessfulFeedback() {
        CapturingRegistry registry = new CapturingRegistry();
        FlowRuntimeFeedbackAgent agent = new FlowRuntimeFeedbackAgent(config(), registry);
        var result = agent.execute(new FlowRuntimeFeedbackAgent.Input(
                new FlowContractBundle("flow-contract-bundle.v1", null, List.of(flow())),
                new GeneratedUiSmokeResult(GeneratedUiSmokeStatus.PASSED, "passed", 1, List.of()),
                passedExecution(), true, "PASSED"), null);

        Assert.assertTrue(result.success());
        Assert.assertEquals(registry.persistCalls, 1);
        Assert.assertEquals(registry.persisted.contracts().get(0).status(), ua.demo.agentlab.artifactreuse.flow.FlowContractStatus.CONFIRMED);
        Assert.assertEquals(registry.persisted.contracts().get(0).runtimePassRate(), 1.0d);
        Assert.assertTrue(registry.feedback.smokePassed());
    }

    private ArtifactReuseRuntimeConfig config() {
        return new ArtifactReuseRuntimeConfig() {
            @Override public boolean enabled() { return true; }
            @Override public boolean forceRefresh() { return false; }
            @Override public String stableRoot() { return "target"; }
            @Override public boolean flowContractEnabled() { return true; }
        };
    }

    private GeneratedTestExecutionResult passedExecution() {
        return new GeneratedTestExecutionResult(
                GeneratedTestExecutionResult.SCHEMA_VERSION,
                GeneratedTestExecutionStatus.PASSED,
                1, 1, 0, 0, 1L,
                List.of("GeneratedTest"), List.of(), List.of(), "target/reports", "passed", ""
        );
    }

    private ua.demo.agentlab.artifactreuse.flow.FlowContract flow() {
        return new ua.demo.agentlab.artifactreuse.flow.FlowContract(
                "flow-contract.v1", "AUTH_LOGIN_TO_DASHBOARD", "Authentication", ua.demo.agentlab.artifactreuse.flow.FlowContractType.AUTHENTICATION,
                new ua.demo.agentlab.artifactreuse.flow.FlowEndpoint("LoginPage", "/login"),
                new ua.demo.agentlab.artifactreuse.flow.FlowEndpoint("DashboardPage", "/dashboard"),
                List.of(ua.demo.agentlab.artifactreuse.flow.FlowState.UNAUTHENTICATED),
                List.of(ua.demo.agentlab.artifactreuse.flow.FlowState.AUTHENTICATED), List.of(), List.of(), List.of(), List.of(), List.of(),
                0.9d, ua.demo.agentlab.artifactreuse.flow.FlowContractStatus.CONFIRMED);
    }

    private static final class CapturingRegistry implements FlowContractRegistry {
        private FlowRuntimeFeedback feedback;
        private int persistCalls;
        private FlowContractBundle persisted;

        @Override public ua.demo.agentlab.artifactreuse.flow.FlowContractPersistenceResult persist(FlowContractBundle bundle) {
            persistCalls++;
            persisted = bundle;
            return ua.demo.agentlab.artifactreuse.flow.FlowContractPersistenceResult.success("test", 0, "");
        }

        @Override public FlowRuntimeFeedbackResult recordRuntimeFeedback(FlowContractBundle bundle, FlowRuntimeFeedback feedback) {
            this.feedback = feedback;
            return FlowRuntimeFeedbackResult.success("test", bundle.contracts().size(), "updated");
        }
    }
}
