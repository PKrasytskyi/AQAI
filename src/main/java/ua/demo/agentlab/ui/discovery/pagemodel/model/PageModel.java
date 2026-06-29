package ua.demo.agentlab.ui.discovery.pagemodel.model;

import java.util.List;

public record PageModel(
        String pageId,
        String url,
        String route,
        String title,
        String visibleText,
        String featureGuess,
        PageEvidenceModel evidence,
        List<PageElementModel> elements,
        List<PageFormModel> forms,
        List<PageApiRelationModel> apiRelations,
        List<PageFlowModel> flows
) {
    public PageModel {
        pageId = pageId == null ? "" : pageId.trim();
        url = url == null ? "" : url.trim();
        route = route == null ? "" : route.trim();
        title = title == null ? "" : title.trim();
        visibleText = visibleText == null ? "" : visibleText.trim();
        featureGuess = featureGuess == null ? "" : featureGuess.trim();
        evidence = evidence == null ? new PageEvidenceModel("", "") : evidence;
        elements = elements == null ? List.of() : List.copyOf(elements);
        forms = forms == null ? List.of() : List.copyOf(forms);
        apiRelations = apiRelations == null ? List.of() : List.copyOf(apiRelations);
        flows = flows == null ? List.of() : List.copyOf(flows);
    }
}
