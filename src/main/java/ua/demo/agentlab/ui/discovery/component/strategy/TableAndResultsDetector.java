package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.ArrayList;
import java.util.List;

public final class TableAndResultsDetector implements ComponentDetectionStrategy {
    private final ComponentEvidenceMatcher matcher = new ComponentEvidenceMatcher();
    @Override public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<ComponentCandidate> result = new ArrayList<>();
        List<String> results = matcher.ids(context, "results", "records", "vacancies", "candidates", "collection",
                "rowgroup", "grid");
        if (!results.isEmpty()) result.add(new ComponentCandidate("results", "ResultsCollectionComponent",
                ComponentType.RESULTS_COLLECTION, results, 0.75d, List.of(),
                List.of("component:results-collection-evidence")));
        List<String> table = matcher.ids(context, "table", "rowgroup", "columnheader", "grid", "cell");
        if (!table.isEmpty()) result.add(new ComponentCandidate("table", "TableComponent", ComponentType.TABLE,
                table, 0.76d, List.of(), List.of("component:table-evidence")));
        return result;
    }
}
