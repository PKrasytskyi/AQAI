package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.flow.FlowActionType;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractPersistencePolicy;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStatus;
import ua.demo.agentlab.artifactreuse.flow.FlowContractStep;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.artifactreuse.flow.FlowEndpoint;
import ua.demo.agentlab.artifactreuse.flow.FlowState;

import java.util.List;

public class FlowContractPersistencePolicyTest {

    private final FlowContractPersistencePolicy policy = new FlowContractPersistencePolicy();

    @Test
    public void acceptsOnlyConfirmedExactFlowWithRuntimeEvidence() {
        Assert.assertTrue(policy.isEligible(contract(FlowContractStatus.CONFIRMED, List.of("transition:/login->/secure"))));
        Assert.assertFalse(policy.isEligible(contract(FlowContractStatus.NEEDS_REVIEW, List.of("transition:/login->/secure"))));
        Assert.assertFalse(policy.isEligible(contract(FlowContractStatus.CONFIRMED, List.of("requirement:REQ-1"))));
    }

    private FlowContract contract(FlowContractStatus status, List<String> evidence) {
        return new FlowContract(
                "flow-contract.v1", "AUTH_LOGIN_TO_SECURE", "Authentication", FlowContractType.AUTHENTICATION,
                new FlowEndpoint("LoginPage", "/login"), new FlowEndpoint("SecurePage", "/secure"),
                List.of(FlowState.UNAUTHENTICATED), List.of(FlowState.AUTHENTICATED),
                List.of(new FlowContractStep(1, FlowActionType.AUTHENTICATE, "LoginPage", "/login", "credentials", false)),
                List.of("/secure"), List.of("REQ-1"), evidence, List.of(), 0.90d, status
        );
    }
}
