package ua.demo.agentlab.ui.discovery.interaction.agent;

import ua.demo.agentlab.ui.discovery.catalog.ConfirmedUiCatalog;
import ua.demo.agentlab.ui.discovery.catalog.LocatorCandidateCoverageReport;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTrace;
import ua.demo.agentlab.ui.discovery.interaction.persistence.InteractionGraphProjectionResult;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceBundle;

public record UiInteractionEvidenceOutput(
        CanonicalInteractionEvidenceBundle canonical,
        LocatorCandidateCoverageReport coverage,
        ConfirmedUiCatalog catalog,
        InteractionGraphProjectionResult graphProjection,
        EvidenceProjectionTrace projectionTrace
) { }
