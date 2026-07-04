package ua.demo.agentlab.ui.catalog;

import java.util.List;

public record CandidatePage(
        String pageName,
        String route,
        PageCapability capability,
        PageSource source,
        double confidence,
        List<String> evidence
) {
    public CandidatePage {
        pageName = pageName == null ? "" : pageName.trim();
        route = route == null ? "" : route.trim();
        capability = capability == null ? PageCapability.GENERIC : capability;
        source = source == null ? PageSource.REQUIREMENT_ROUTE : source;
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public ConfirmedPageCandidate confirm() {
        return new ConfirmedPageCandidate(pageName, route, capability, source, confidence, evidence);
    }
}
