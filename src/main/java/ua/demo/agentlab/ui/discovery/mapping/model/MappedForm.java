package ua.demo.agentlab.ui.discovery.mapping.model;

import java.util.List;

public record MappedForm(
        String formId,
        String formName,
        String action,
        List<MappedField> fields,
        List<String> submitActionIds
) {
    public MappedForm {
        formId = formId == null ? "" : formId.trim();
        formName = formName == null ? "" : formName.trim();
        action = action == null ? "" : action.trim();
        fields = fields == null ? List.of() : List.copyOf(fields);
        submitActionIds = submitActionIds == null ? List.of() : List.copyOf(submitActionIds);
    }
}
