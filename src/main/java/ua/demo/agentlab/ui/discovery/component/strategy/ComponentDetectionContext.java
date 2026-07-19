package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;

import java.util.LinkedHashMap;
import java.util.Map;

public record ComponentDetectionContext(PageModel page, Map<String, PageElementModel> elementsById) {
    public ComponentDetectionContext(PageModel page) {
        this(page, page.elements().stream().collect(java.util.stream.Collectors.toMap(PageElementModel::elementId,
                element -> element, (left, right) -> left, LinkedHashMap::new)));
    }
}
