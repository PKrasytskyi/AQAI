package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.util.List;

/** Result of a separate browser session that validates only requirement-scoped SPA evidence. */
public record SpaLiveTargetedVerificationResult(
        String schemaVersion,
        KnowledgeRunMetadata runMetadata,
        boolean executed,
        boolean passed,
        List<TargetedLocatorVerification> locatorVerifications,
        List<TargetedActionVerification> actionVerifications,
        SpaStateGraph stateGraph,
        List<LiveTargetPageSnapshot> targetPageSnapshots,
        List<String> sourceTrace
) {
    public static final String SCHEMA_VERSION = "spa-live-targeted-verification.v1";

    public SpaLiveTargetedVerificationResult {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        locatorVerifications = locatorVerifications == null ? List.of() : List.copyOf(locatorVerifications);
        actionVerifications = actionVerifications == null ? List.of() : List.copyOf(actionVerifications);
        stateGraph = stateGraph == null ? SpaStateGraph.empty(runMetadata, "spa-state-graph:not-captured") : stateGraph;
        targetPageSnapshots = targetPageSnapshots == null ? List.of() : List.copyOf(targetPageSnapshots);
        sourceTrace = sourceTrace == null ? List.of() : List.copyOf(sourceTrace);
    }

    public SpaLiveTargetedVerificationResult(String schemaVersion, KnowledgeRunMetadata runMetadata, boolean executed, boolean passed,
                                              List<TargetedLocatorVerification> locatorVerifications,
                                              List<TargetedActionVerification> actionVerifications,
                                              List<String> sourceTrace) {
        this(schemaVersion, runMetadata, executed, passed, locatorVerifications, actionVerifications,
                SpaStateGraph.empty(runMetadata, "spa-state-graph:legacy-result"), List.of(), sourceTrace);
    }

    public SpaLiveTargetedVerificationResult(String schemaVersion, KnowledgeRunMetadata runMetadata, boolean executed,
                                              boolean passed, List<TargetedLocatorVerification> locatorVerifications,
                                              List<TargetedActionVerification> actionVerifications,
                                              SpaStateGraph stateGraph, List<String> sourceTrace) {
        this(schemaVersion, runMetadata, executed, passed, locatorVerifications, actionVerifications,
                stateGraph, List.of(), sourceTrace);
    }

    public static SpaLiveTargetedVerificationResult skipped(KnowledgeRunMetadata metadata, String reason) {
        return new SpaLiveTargetedVerificationResult(SCHEMA_VERSION, metadata, false, false, List.of(), List.of(),
                SpaStateGraph.empty(metadata, "spa-state-graph:verification-skipped"),
                List.of(), List.of(reason == null ? "spa-live-verification:skipped" : reason));
    }
}
