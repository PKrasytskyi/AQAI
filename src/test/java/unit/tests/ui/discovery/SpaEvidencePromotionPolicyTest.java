package unit.tests.ui.discovery;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaEvidencePromotionPolicy;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;

public class SpaEvidencePromotionPolicyTest {
    private final SpaEvidencePromotionPolicy policy = new SpaEvidencePromotionPolicy();
    private final SpaInventoryConfig config = new SpaInventoryConfig(true, SpaDiscoveryMode.TARGETED, 30,
            true, 0.80d, 2, 2, true, true, true, 14, 30, false);

    @Test
    public void requiresLiveStabilityBeforePromptPromotion() {
        Assert.assertEquals(policy.afterLiveVerification(1, 0, 0.90d, config, true), SpaEvidenceStatus.LIVE_VERIFIED);
        Assert.assertEquals(policy.afterLiveVerification(2, 0, 0.90d, config, true), SpaEvidenceStatus.STABLE);
        Assert.assertEquals(policy.afterSmoke(SpaEvidenceStatus.STABLE, true, 0, config), SpaEvidenceStatus.PROMPT_ALLOWED);
    }

    @Test
    public void neverPromotesCandidateDirectlyFromSmokeAndDemotesRepeatedFailures() {
        Assert.assertEquals(policy.afterSmoke(SpaEvidenceStatus.CANDIDATE, true, 0, config), SpaEvidenceStatus.CANDIDATE);
        Assert.assertEquals(policy.afterLiveVerification(3, 2, 0.95d, config, false), SpaEvidenceStatus.DEGRADED);
    }
}
