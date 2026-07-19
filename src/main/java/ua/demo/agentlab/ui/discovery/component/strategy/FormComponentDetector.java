package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FormComponentDetector implements ComponentDetectionStrategy {
    private final ComponentEvidenceMatcher matcher = new ComponentEvidenceMatcher();

    @Override public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<ComponentCandidate> result = new ArrayList<>();
        Set<String> formElementIds = new LinkedHashSet<>();
        context.page().forms().forEach(form -> {
            List<String> ids = java.util.stream.Stream.concat(form.fieldElementIds().stream(),
                            form.submitElementIds().stream()).filter(context.elementsById()::containsKey).distinct().toList();
            if (ids.isEmpty()) return;
            formElementIds.addAll(ids);
            boolean filter = isFilter(form.submitElementIds().stream().map(context.elementsById()::get).toList());
            String name = filter ? "FilterPanelComponent" : display(form.formName().isBlank() ? "Form" : form.formName());
            result.add(new ComponentCandidate(sanitize(form.formName().isBlank() ? "form" : form.formName()), name,
                    filter ? ComponentType.FILTER_PANEL : ComponentType.FORM, ids, filter ? 0.84d : 0.90d,
                    List.of(), List.of(filter ? "component:filter-form" : "component:form", "formId=" + form.formId())));
        });
        List<String> standaloneFilterIds = matcher.ids(context, "filter", "criteria")
                .stream().filter(id -> !formElementIds.contains(id)).toList();
        if (!standaloneFilterIds.isEmpty()) {
            result.add(new ComponentCandidate("filter-panel", "FilterPanelComponent",
                    ComponentType.FILTER_PANEL, standaloneFilterIds, 0.78d, List.of(),
                    List.of("component:standalone-filter-evidence")));
        }
        return result;
    }

    private boolean isFilter(List<PageElementModel> submitElements) {
        String text = submitElements.stream().filter(java.util.Objects::nonNull)
                .map(element -> element.text() + " " + element.elementId()).collect(java.util.stream.Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
        return List.of("search", "filter", "apply", "reset").stream().anyMatch(text::contains);
    }

    private String sanitize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", ""); }
    private String display(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1) + (value.endsWith("Component") ? "" : "Component"); }
}
