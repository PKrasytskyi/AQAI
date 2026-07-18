package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFormModel;

import java.util.List;

/** Assembles a form only after its field and submit identities are resolved. */
public final class FormModelAssembler {

    public PageFormModel assemble(String pageId, String formName, String action,
                                  List<String> fieldIds, List<String> submitIds) {
        String normalizedName = formName == null || formName.isBlank() ? "form" : formName;
        return new PageFormModel(pageId + ":form:" + sanitize(normalizedName), normalizedName, action,
                fieldIds, submitIds);
    }

    private String sanitize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
