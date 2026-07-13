package ua.demo.agentlab.artifactreuse.flow;

import java.util.List;

public record FlowContract(
        String schemaVersion,
        String flowId,
        String displayName,
        FlowContractType type,
        FlowEndpoint source,
        FlowEndpoint target,
        List<FlowState> requiresStates,
        List<FlowState> producesStates,
        List<FlowContractStep> steps,
        List<String> assertions,
        List<String> requirementIds,
        List<String> evidence,
        List<String> artifactIds,
        String contractFingerprint,
        String lastSuccessfulSmoke,
        double runtimePassRate,
        double flakyRate,
        int runtimeSmokeAttempts,
        int runtimeSmokePasses,
        String lastRuntimeSmokeStatus,
        double confidence,
        FlowContractStatus status
) {
    public FlowContract(
            String schemaVersion,
            String flowId,
            String displayName,
            FlowContractType type,
            FlowEndpoint source,
            FlowEndpoint target,
            List<FlowState> requiresStates,
            List<FlowState> producesStates,
            List<FlowContractStep> steps,
            List<String> assertions,
            List<String> requirementIds,
            List<String> evidence,
            List<String> artifactIds,
            String contractFingerprint,
            String lastSuccessfulSmoke,
            double runtimePassRate,
            double flakyRate,
            double confidence,
            FlowContractStatus status
    ) {
        this(schemaVersion, flowId, displayName, type, source, target, requiresStates, producesStates, steps, assertions,
                requirementIds, evidence, artifactIds, contractFingerprint, lastSuccessfulSmoke, runtimePassRate,
                flakyRate, 0, 0, "", confidence, status);
    }

    public FlowContract(
            String schemaVersion,
            String flowId,
            String displayName,
            FlowContractType type,
            FlowEndpoint source,
            FlowEndpoint target,
            List<FlowState> requiresStates,
            List<FlowState> producesStates,
            List<FlowContractStep> steps,
            List<String> assertions,
            List<String> requirementIds,
            List<String> evidence,
            List<String> artifactIds,
            double confidence,
            FlowContractStatus status
    ) {
        this(schemaVersion, flowId, displayName, type, source, target, requiresStates, producesStates, steps, assertions,
                requirementIds, evidence, artifactIds,
                new FlowContractFingerprintBuilder().build(schemaVersion, type, source, target, requiresStates, producesStates, steps, assertions),
                "", 0.0d, 1.0d, 0, 0, "", confidence, status);
    }

    public FlowContract {
        schemaVersion = safe(schemaVersion, "flow-contract.v1");
        flowId = safe(flowId, "");
        displayName = safe(displayName, flowId);
        type = type == null ? FlowContractType.GENERIC : type;
        source = source == null ? new FlowEndpoint("", "") : source;
        target = target == null ? new FlowEndpoint("", "") : target;
        requiresStates = requiresStates == null ? List.of() : List.copyOf(requiresStates);
        producesStates = producesStates == null ? List.of() : List.copyOf(producesStates);
        steps = steps == null ? List.of() : List.copyOf(steps);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        artifactIds = artifactIds == null ? List.of() : List.copyOf(artifactIds);
        contractFingerprint = safe(contractFingerprint, new FlowContractFingerprintBuilder().build(
                schemaVersion, type, source, target, requiresStates, producesStates, steps, assertions));
        lastSuccessfulSmoke = safe(lastSuccessfulSmoke, "");
        runtimePassRate = normalizedRate(runtimePassRate, 0.0d);
        flakyRate = normalizedRate(flakyRate, 1.0d);
        runtimeSmokeAttempts = Math.max(0, runtimeSmokeAttempts);
        runtimeSmokePasses = Math.max(0, Math.min(runtimeSmokePasses, runtimeSmokeAttempts));
        lastRuntimeSmokeStatus = safe(lastRuntimeSmokeStatus, "");
        confidence = Double.isFinite(confidence) ? Math.max(0.0d, Math.min(1.0d, confidence)) : 0.0d;
        status = status == null ? FlowContractStatus.NEEDS_REVIEW : status;
    }

    public boolean confirmed() {
        return status == FlowContractStatus.CONFIRMED;
    }

    public FlowReuseQualityDecision reuseQuality() {
        return new FlowReuseQualityPolicy().evaluate(this);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static double normalizedRate(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0d, Math.min(1.0d, value)) : fallback;
    }
}
