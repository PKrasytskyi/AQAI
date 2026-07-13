package ua.demo.agentlab.artifactreuse.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * A pure, conservative policy. The caller decides whether a blocked record becomes STALE or INVALIDATED.
 */
public class ArtifactInvalidationPolicy {

    public ArtifactInvalidationDecision evaluate(ArtifactInvalidationInput input) {
        if (input == null) {
            return new ArtifactInvalidationDecision(false, List.of(ArtifactInvalidationReason.FINGERPRINT_CHANGED));
        }
        List<ArtifactInvalidationReason> reasons = new ArrayList<>();
        if (!input.reuseEnabled()) reasons.add(ArtifactInvalidationReason.REUSE_DISABLED);
        if (input.forceRefresh()) reasons.add(ArtifactInvalidationReason.FORCE_REFRESH);
        if (!input.fingerprintMatches()) reasons.add(ArtifactInvalidationReason.FINGERPRINT_CHANGED);
        if (!input.schemaMatches()) reasons.add(ArtifactInvalidationReason.SCHEMA_CHANGED);
        if (!input.promptTemplateMatches()) reasons.add(ArtifactInvalidationReason.PROMPT_TEMPLATE_CHANGED);
        if (!input.writerVersionMatches()) reasons.add(ArtifactInvalidationReason.WRITER_VERSION_CHANGED);
        if (!input.qualityGatePassed()) reasons.add(ArtifactInvalidationReason.QUALITY_GATE_FAILED);
        if (!input.compilePassed()) reasons.add(ArtifactInvalidationReason.COMPILE_FAILED);
        if (!input.smokePassed()) reasons.add(ArtifactInvalidationReason.SMOKE_FAILED);
        if (!input.pageFingerprintMatches()) reasons.add(ArtifactInvalidationReason.PAGE_FINGERPRINT_CHANGED);
        if (!input.requiredActionsMatch()) reasons.add(ArtifactInvalidationReason.REQUIRED_ACTIONS_CHANGED);
        if (!input.requiredAssertionsMatch()) reasons.add(ArtifactInvalidationReason.REQUIRED_ASSERTIONS_CHANGED);
        return new ArtifactInvalidationDecision(reasons.isEmpty(), reasons);
    }
}
