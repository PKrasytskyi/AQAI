package ua.demo.agentlab.artifactreuse.flow;

import java.util.ArrayList;
import java.util.List;

/** Quality threshold applied only to cross-run flow reuse, never to current-run discovery evidence. */
public class FlowReuseQualityPolicy {

    public static final double MIN_RUNTIME_PASS_RATE = 0.90d;
    public static final double MAX_FLAKY_RATE = 0.10d;
    public static final int MIN_SAMPLES_FOR_RATE_GATE = 3;

    public FlowReuseQualityDecision evaluate(FlowContract contract) {
        if (contract == null || !contract.confirmed()) {
            return FlowReuseQualityDecision.rejected("flow is not confirmed");
        }
        List<String> reasons = new ArrayList<>();
        if (contract.contractFingerprint().isBlank()) reasons.add("contract fingerprint is missing");
        if (contract.lastSuccessfulSmoke().isBlank()) reasons.add("no successful runtime smoke is recorded");
        if ("FAILED".equalsIgnoreCase(contract.lastRuntimeSmokeStatus())) {
            reasons.add("last runtime smoke failed");
        }
        boolean enoughSamples = contract.runtimeSmokeAttempts() >= MIN_SAMPLES_FOR_RATE_GATE;
        if (enoughSamples && contract.runtimePassRate() < MIN_RUNTIME_PASS_RATE) {
            reasons.add("runtime pass rate is below " + MIN_RUNTIME_PASS_RATE);
        }
        if (enoughSamples && contract.flakyRate() > MAX_FLAKY_RATE) {
            reasons.add("flaky rate is above " + MAX_FLAKY_RATE);
        }
        return reasons.isEmpty() ? FlowReuseQualityDecision.accepted() : FlowReuseQualityDecision.rejected(reasons);
    }
}
