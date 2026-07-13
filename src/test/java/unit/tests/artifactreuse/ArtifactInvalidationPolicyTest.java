package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.policy.ArtifactInvalidationInput;
import ua.demo.agentlab.artifactreuse.policy.ArtifactInvalidationPolicy;
import ua.demo.agentlab.artifactreuse.policy.ArtifactInvalidationReason;

public class ArtifactInvalidationPolicyTest {

    private final ArtifactInvalidationPolicy policy = new ArtifactInvalidationPolicy();

    @Test
    public void acceptsOnlyFullyCompatibleValidatedArtifact() {
        Assert.assertTrue(policy.evaluate(input(true, true, true, true)).reusable());
    }

    @Test
    public void blocksReuseWhenWriterOrPageEvidenceChanges() {
        var decision = policy.evaluate(new ArtifactInvalidationInput(
                true, false, true, true, true, false,
                true, true, true, false, true, true));

        Assert.assertFalse(decision.reusable());
        Assert.assertTrue(decision.reasons().contains(ArtifactInvalidationReason.WRITER_VERSION_CHANGED));
        Assert.assertTrue(decision.reasons().contains(ArtifactInvalidationReason.PAGE_FINGERPRINT_CHANGED));
    }

    private ArtifactInvalidationInput input(boolean writerMatches, boolean pageMatches, boolean actionsMatch, boolean assertionsMatch) {
        return new ArtifactInvalidationInput(true, false, true, true, true, writerMatches,
                true, true, true, pageMatches, actionsMatch, assertionsMatch);
    }
}
