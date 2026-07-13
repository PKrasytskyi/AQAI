package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.policy.ArtifactReuseDecisionType;
import ua.demo.agentlab.artifactreuse.policy.ArtifactReusePolicy;
import ua.demo.agentlab.artifactreuse.policy.ArtifactReusePolicyInput;

public class ArtifactReusePolicyTest {

    private final ArtifactReusePolicy policy = new ArtifactReusePolicy();

    @Test
    public void reusesReadableStableArtifact() {
        Assert.assertEquals(policy.decide(new ArtifactReusePolicyInput(
                true, false, true, true, "STABLE", "pom-contract-v1", "pom-contract-v1"
        )).type(), ArtifactReuseDecisionType.REUSE_STABLE);
    }

    @Test
    public void callsLlmWhenReuseDisabled() {
        Assert.assertEquals(policy.decide(new ArtifactReusePolicyInput(
                false, false, true, true, "STABLE", "pom-contract-v1", "pom-contract-v1"
        )).type(), ArtifactReuseDecisionType.CALL_LLM);
    }

    @Test
    public void regeneratesWhenStableArtifactCannotBeRead() {
        Assert.assertEquals(policy.decide(new ArtifactReusePolicyInput(
                true, false, true, false, "STABLE", "pom-contract-v1", "pom-contract-v1"
        )).type(), ArtifactReuseDecisionType.REGENERATE_PREVIOUS_INVALID);
    }

    @Test
    public void doesNotReuseArtifactBeforeLifecyclePromotion() {
        Assert.assertEquals(policy.decide(new ArtifactReusePolicyInput(
                true, false, true, true, "SCHEMA_VALIDATED", "pom-contract-v1", "pom-contract-v1"
        )).type(), ArtifactReuseDecisionType.REGENERATE_PREVIOUS_INVALID);
    }
}
