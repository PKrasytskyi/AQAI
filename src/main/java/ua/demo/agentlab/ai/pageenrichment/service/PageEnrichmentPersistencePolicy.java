package ua.demo.agentlab.ai.pageenrichment.service;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.Set;

public class PageEnrichmentPersistencePolicy {

    public boolean isEligible(PageModelEnrichmentRecord record, Set<String> failedPageIds) {
        if (record == null || failedPageIds.contains(record.pageId())) {
            return false;
        }
        if (record.pageId().isBlank()
                || record.pageName().isBlank()
                || RouteCanonicalizer.canonicalize(record.route()).isBlank()
                || record.requirementTraceability().isEmpty()) {
            return false;
        }
        if (record.confidenceScore() < 0.65d || record.enrichmentSource().toLowerCase().contains("fallback")) {
            return false;
        }
        return record.requirementTraceability().stream().allMatch(this::validRequirementId);
    }

    private boolean validRequirementId(String value) {
        return value != null && value.trim().matches("(?i)(REQ[-_].+|[A-Z]+[-_]?[0-9]+)");
    }
}
