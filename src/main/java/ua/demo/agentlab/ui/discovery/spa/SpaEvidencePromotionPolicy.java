package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;

public class SpaEvidencePromotionPolicy {
    public SpaEvidenceStatus afterLiveVerification(int successes, int failures, double score,
                                                   SpaInventoryConfig config, boolean lastVerified) {
        if (failures >= config.demoteAfterFailures()) return SpaEvidenceStatus.DEGRADED;
        if (successes >= config.promoteAfterSuccesses() && score >= config.minConfirmedScore()) {
            return SpaEvidenceStatus.STABLE;
        }
        return lastVerified ? SpaEvidenceStatus.LIVE_VERIFIED : SpaEvidenceStatus.CANDIDATE;
    }

    public SpaEvidenceStatus afterSmoke(SpaEvidenceStatus current, boolean passed, int smokeFailures,
                                        SpaInventoryConfig config) {
        if (smokeFailures >= config.demoteAfterFailures()) return SpaEvidenceStatus.DEGRADED;
        if (passed && (current == SpaEvidenceStatus.STABLE || current == SpaEvidenceStatus.PROMPT_ALLOWED)) {
            return SpaEvidenceStatus.PROMPT_ALLOWED;
        }
        return current == null ? SpaEvidenceStatus.CANDIDATE : current;
    }
}
