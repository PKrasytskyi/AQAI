package ua.demo.agentlab.ui.discovery.evidence.funnel;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.capability.UiCapabilityRegistry;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTrace;

import java.util.Comparator;
import java.util.List;

/** Assembles read-only funnel projections without making ownership or promotion decisions. */
public final class UiEvidenceFunnelAssembler {

    private final RequirementFunnelProjection requirementProjection;
    private final LocatorFunnelMetricsProjection metricsProjection = new LocatorFunnelMetricsProjection();
    private final PageReadinessProjection readinessProjection = new PageReadinessProjection();
    private final TerminalFailureProjection terminalProjection = new TerminalFailureProjection();

    public UiEvidenceFunnelAssembler() {
        this(new UiCapabilityRegistry(), new RequirementEvidenceTraceResolver());
    }

    UiEvidenceFunnelAssembler(UiCapabilityRegistry capabilityRegistry) {
        this(capabilityRegistry, new RequirementEvidenceTraceResolver());
    }

    UiEvidenceFunnelAssembler(UiCapabilityRegistry capabilityRegistry,
                              RequirementEvidenceTraceResolver traceResolver) {
        this.requirementProjection = new RequirementFunnelProjection(capabilityRegistry, traceResolver);
    }

    public UiEvidenceFunnelReport assemble(UiEvidenceFunnelInput input) {
        if (input == null) throw new IllegalArgumentException("input cannot be null");
        List<UiEvidenceRequirementResult> requirements = input.requirements().stream()
                .sorted(Comparator.comparing(StructuredBehaviorContract::requirementId))
                .map(requirement -> requirementProjection.project(input, requirement))
                .toList();
        UiEvidenceFunnelMetrics metrics = metricsProjection.project(input, requirements);
        PageReadinessProjection.Readiness readiness = readinessProjection.project(input, requirements);
        EvidenceProjectionTrace trace = input.projectionTrace();
        return new UiEvidenceFunnelReport(UiEvidenceFunnelReport.SCHEMA_VERSION, input.runId(),
                readiness.completenessPassed(), readiness.pomReadinessPassed(), metrics, requirements,
                terminalProjection.findings(requirements), trace);
    }
}
