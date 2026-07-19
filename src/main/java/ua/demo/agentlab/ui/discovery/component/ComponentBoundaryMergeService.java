package ua.demo.agentlab.ui.discovery.component;

import ua.demo.agentlab.ui.discovery.component.model.*;
import ua.demo.agentlab.ui.discovery.component.strategy.ComponentCandidate;
import ua.demo.agentlab.ui.discovery.component.strategy.ComponentDetectionContext;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.*;

/** Resolves candidate overlap once and materializes the component topology. */
public final class ComponentBoundaryMergeService {
    private final ScopedLocatorValidationService locatorValidation;

    public ComponentBoundaryMergeService(ScopedLocatorValidationService locatorValidation) {
        this.locatorValidation = locatorValidation;
    }

    public List<SemanticComponentModel> merge(ComponentDetectionContext context, List<ComponentCandidate> candidates) {
        Set<String> assigned = new LinkedHashSet<>();
        List<SemanticComponentModel> result = new ArrayList<>();
        Map<String, Set<String>> locatorIndex = locatorIndex(context.page().elements());
        for (ComponentCandidate candidate : candidates) {
            List<String> ids = candidate.elementIds().stream().filter(context.elementsById()::containsKey)
                    .filter(id -> !assigned.contains(id)).toList();
            if (ids.isEmpty()) continue;
            String componentId = context.page().pageId() + ":component:" + candidate.idSuffix();
            List<PageElementModel> elements = ids.stream().map(context.elementsById()::get).toList();
            List<ScopedLocatorCandidate> locators = locatorValidation.validate(context.page(), componentId, elements, locatorIndex);
            double locatorConfidence = locators.stream().mapToDouble(ScopedLocatorCandidate::finalScore).average().orElse(0.0d);
            PageLocatorModel root = root(candidate.type(), elements);
            result.add(new SemanticComponentModel(context.page().pageId(), componentId, candidate.name(), candidate.type(),
                    root == null ? "" : root.strategy(), root == null ? "" : root.value(), ids, locators,
                    reusable(candidate.type(), ids), Math.max(candidate.confidence(), locatorConfidence),
                    candidate.risks(), candidate.sourceTrace()));
            assigned.addAll(ids);
        }
        return result;
    }

    private Map<String, Set<String>> locatorIndex(List<PageElementModel> elements) {
        Map<String, Set<String>> index = new LinkedHashMap<>();
        elements.forEach(element -> element.locatorCandidates().forEach(locator -> index
                .computeIfAbsent(locator.strategy() + "::" + locator.value(), ignored -> new LinkedHashSet<>())
                .add(element.elementId())));
        return index;
    }

    private PageLocatorModel root(ComponentType type, List<PageElementModel> elements) {
        if (type == ComponentType.FORM || type == ComponentType.FILTER_PANEL)
            return new PageLocatorModel("css", "form", 0.50d, "component root fallback", false);
        if (type == ComponentType.NAVIGATION)
            return new PageLocatorModel("css", "nav, aside, [role='navigation']", 0.50d, "component root fallback", false);
        if (type == ComponentType.SEARCH)
            return new PageLocatorModel("css", "input[type='search'], input[placeholder*='Search']", 0.50d,
                    "component root fallback", false);
        return elements.stream().flatMap(element -> element.locatorCandidates().stream())
                .max(Comparator.comparingDouble(PageLocatorModel::score)).orElse(null);
    }

    private boolean reusable(ComponentType type, List<String> ids) {
        return type != ComponentType.CONTENT && type != ComponentType.UNKNOWN && ids.size() > 1;
    }
}
