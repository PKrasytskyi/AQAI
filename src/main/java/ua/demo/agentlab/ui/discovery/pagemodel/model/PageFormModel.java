package ua.demo.agentlab.ui.discovery.pagemodel.model;

import java.util.List;

public record PageFormModel(
        String formId,
        String formName,
        String action,
        List<String> fieldElementIds,
        List<String> submitElementIds
) {
    public PageFormModel {
        formId = formId == null ? "" : formId.trim();
        formName = formName == null ? "" : formName.trim();
        action = action == null ? "" : action.trim();
        fieldElementIds = fieldElementIds == null ? List.of() : List.copyOf(fieldElementIds);
        submitElementIds = submitElementIds == null ? List.of() : List.copyOf(submitElementIds);
    }
}
