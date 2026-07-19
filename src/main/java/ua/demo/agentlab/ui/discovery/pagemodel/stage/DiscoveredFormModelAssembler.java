package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.pagemodel.model.*;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredField;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredForm;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts one discovered form into field/submit elements and an immutable form contract. */
public final class DiscoveredFormModelAssembler {
    private final ElementIdentityResolver identities;
    private final LocatorCandidateAssembler locators;
    private final PageActionAssembler actions;
    private final FormModelAssembler forms;

    public DiscoveredFormModelAssembler(ElementIdentityResolver identities, LocatorCandidateAssembler locators,
                                        PageActionAssembler actions, FormModelAssembler forms) {
        this.identities = identities;
        this.locators = locators;
        this.actions = actions;
        this.forms = forms;
    }

    public PageFormModel assemble(String pageId, DiscoveredForm form, int index,
                                  List<PageElementModel> elements, Map<String, String> identityIndex) {
        String formName = identities.semanticName(first(form.formName(), form.formId(), "form" + (index + 1)));
        List<String> fields = new ArrayList<>();
        for (int fieldIndex = 0; fieldIndex < form.fields().size(); fieldIndex++) {
            DiscoveredField field = form.fields().get(fieldIndex);
            String key = identities.rawElementKey("FIELD", field.label(), field.id(), field.name(), "");
            String existing = first(identityIndex.get(key), existingField(elements, field));
            if (!existing.isBlank()) {
                fields.add(existing);
                identityIndex.put(key, existing);
                continue;
            }
            String name = identities.semanticName(first(field.label(), field.name(), field.id(), "field" + (fieldIndex + 1)));
            String elementId = identities.uniqueElementId(elements, pageId, name);
            List<PageLocatorModel> candidates = locators.fromHint(field.locatorHint(), field.id(), field.name(), "", field.label());
            elements.add(new PageElementModel(elementId, fieldTechnicalType(field.fieldType()), fieldSemanticType(field),
                    fieldTag(field.fieldType()), safe(field.fieldType()), safe(field.label()), safe(field.id()),
                    safe(field.name()), safe(field.placeholder()), "", "", "", "", true, true, field.required(),
                    attributes("id", field.id(), "name", field.name(), "placeholder", field.placeholder(),
                            "type", field.fieldType()), candidates, candidates.stream().findFirst().orElse(null),
                    actions.forField(elementId, field.fieldType()), 0.82d));
            fields.add(elementId);
            identityIndex.put(key, elementId);
        }

        List<String> submits = new ArrayList<>();
        for (DiscoveredInteractiveElement submit : form.submitActions()) {
            String key = identities.rawElementKey("BUTTON", submit.visibleText(), submit.id(), submit.name(), submit.href());
            String existing = first(identityIndex.get(key), existingSubmit(elements, submit));
            if (!existing.isBlank()) {
                submits.add(existing);
                identityIndex.put(key, existing);
                continue;
            }
            String name = identities.semanticName(first(submit.visibleText(), submit.name(), submit.id(), formName + "Submit"));
            String elementId = identities.uniqueElementId(elements, pageId, name);
            List<PageLocatorModel> candidates = locators.fromSubmitHint(
                    submit.locatorHint(), submit.id(), submit.name(), submit.href(), submit.visibleText());
            elements.add(new PageElementModel(elementId, "SUBMIT_BUTTON", interactiveSemanticType(submit), "button",
                    "submit", safe(submit.visibleText()), safe(submit.id()), safe(submit.name()), "", "",
                    safe(submit.role()), safe(submit.href()), "", submit.visible(), submit.enabled(), false,
                    attributes("id", submit.id(), "name", submit.name(), "role", submit.role(), "type", "submit"),
                    candidates, candidates.stream().findFirst().orElse(null),
                    actions.infer(elementId, "SUBMIT_BUTTON", submit.visibleText(), submit.href(), submit.enabled()), 0.82d));
            submits.add(elementId);
            identityIndex.put(key, elementId);
        }
        return forms.assemble(pageId, formName, form.action(), fields, submits);
    }

    private String existingField(List<PageElementModel> elements, DiscoveredField field) {
        String id = normalize(field.id());
        String name = normalize(field.name());
        String label = normalize(field.label());
        String placeholder = normalize(field.placeholder());
        return elements.stream().filter(element -> contains(element.technicalType(), "input", "dropdown", "select", "textarea")
                        || contains(element.tag(), "input", "select", "textarea"))
                .filter(element -> matches(element.id(), id, name, label, placeholder)
                        || matches(element.name(), id, name, label, placeholder)
                        || matches(element.text(), id, name, label, placeholder)
                        || matches(element.attributes().get("agentlab.field.label"), id, name, label, placeholder)
                        || matches(element.placeholder(), id, name, label, placeholder))
                .map(PageElementModel::elementId).findFirst().orElse("");
    }

    private String existingSubmit(List<PageElementModel> elements, DiscoveredInteractiveElement submit) {
        String id = normalize(submit.id());
        String name = normalize(submit.name());
        String text = normalize(submit.visibleText());
        return elements.stream().filter(element -> contains(element.technicalType(), "button", "submit")
                        || contains(element.tag(), "button") || "submit".equalsIgnoreCase(element.inputType()))
                .filter(element -> matches(element.id(), id, name, text)
                        || matches(element.name(), id, name, text) || matches(element.text(), id, name, text))
                .map(PageElementModel::elementId).findFirst().orElse("");
    }

    private String fieldTechnicalType(String type) {
        String value = normalize(type);
        if (value.equals("email")) return "EMAIL_INPUT";
        if (value.equals("password")) return "PASSWORD_INPUT";
        if (value.equals("select")) return "DROPDOWN";
        if (value.equals("checkbox")) return "CHECKBOX";
        if (value.equals("radio")) return "RADIO";
        if (value.equals("file")) return "FILE_INPUT";
        if (value.equals("textarea")) return "TEXTAREA";
        return "INPUT";
    }

    private String fieldSemanticType(DiscoveredField field) {
        String value = normalize(field.fieldType() + " " + field.label() + " " + field.name() + " " + field.id());
        if (value.contains("email")) return "EMAIL";
        if (value.contains("password")) return "PASSWORD";
        if (value.contains("size")) return "SIZE";
        if (value.contains("color") || value.contains("colour")) return "COLOR";
        if (value.contains("quantity") || value.contains("qty")) return "QUANTITY";
        return "FIELD";
    }

    private String fieldTag(String type) {
        String value = normalize(type);
        if (value.equals("select")) return "select";
        if (value.equals("textarea")) return "textarea";
        return "input";
    }

    private String interactiveSemanticType(DiscoveredInteractiveElement element) {
        String value = normalize(element.visibleText() + " " + element.name() + " " + element.id());
        if (value.contains("add") || value.contains("cart") || value.contains("buy")) return "ADD_TO_CART_BUTTON";
        if (value.contains("remove") || value.contains("delete") || value.contains("clear")) return "REMOVE_BUTTON";
        if (value.contains("search")) return "SEARCH_BUTTON";
        return "BUTTON";
    }

    private Map<String, String> attributes(String... pairs) {
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) if (!safe(pairs[i + 1]).isBlank()) result.put(pairs[i], pairs[i + 1]);
        return Map.copyOf(result);
    }

    private boolean matches(String actual, String... expected) {
        String value = normalize(actual);
        if (value.isBlank()) return false;
        for (String candidate : expected) if (!candidate.isBlank() && value.equals(candidate)) return true;
        return false;
    }

    private boolean contains(String value, String... fragments) {
        String normalized = normalize(value);
        for (String fragment : fragments) if (normalized.contains(fragment)) return true;
        return false;
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String safe(String value) { return value == null ? "" : value; }
}
