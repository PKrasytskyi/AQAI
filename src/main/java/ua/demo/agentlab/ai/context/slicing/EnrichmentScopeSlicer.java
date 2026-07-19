package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps only page-owned and requirement-owned enrichment facts. */
public final class EnrichmentScopeSlicer {

    public List<PageModelEnrichmentRecord> slice(List<PageModelEnrichmentRecord> records,
                                                  TargetPageScopeResolver.TargetPageScope target) {
        if (records == null || records.isEmpty()) return List.of();
        AiContextScope scope = target.requested();
        return records.stream()
                .filter(record -> !target.routeGuard().hasConfirmedPages()
                        || target.routeGuard().isConfirmed(record.pageName(), record.route()))
                .filter(record -> scope.targetRoutes().isEmpty()
                        ? scope.targetPageNames().stream().anyMatch(page ->
                        PageReferenceMatcher.matchesScenarioPage(record.pageName(), record.route(), page))
                        : scope.targetRoutes().stream().anyMatch(route ->
                        PageReferenceMatcher.routeMatches(record.route(), route)))
                .map(record -> scope.targetRequirementIds().isEmpty() ? record
                        : restrict(record, scope.targetRequirementIds()))
                .toList();
    }

    private PageModelEnrichmentRecord restrict(PageModelEnrichmentRecord record, List<String> requirementIds) {
        Set<String> allowed = requirementIds.stream().map(this::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, List<String>> actions = filter(record.actionsByRequirement(), allowed);
        Map<String, List<String>> postconditions = filter(record.postconditionsByRequirement(), allowed);
        List<String> traceability = record.requirementTraceability().stream()
                .filter(id -> allowed.contains(normalize(id))).toList();
        return new PageModelEnrichmentRecord(record.pageId(), record.pageName(), record.route(), record.businessIntent(),
                record.pageSummary(), flatten(actions), record.stableLocators(), record.preconditions(),
                flatten(postconditions), record.risks(), record.coverageGaps(), traceability, actions, postconditions,
                record.confidenceScore(), record.enrichmentSource());
    }

    private Map<String, List<String>> filter(Map<String, List<String>> facts, Set<String> allowed) {
        if (facts == null || facts.isEmpty()) return Map.of();
        Map<String, List<String>> result = new LinkedHashMap<>();
        facts.forEach((id, values) -> {
            if (allowed.contains(normalize(id))) result.put(id, values);
        });
        return result;
    }

    private List<String> flatten(Map<String, List<String>> facts) {
        return facts.values().stream().flatMap(List::stream).filter(value -> value != null && !value.isBlank())
                .distinct().toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
