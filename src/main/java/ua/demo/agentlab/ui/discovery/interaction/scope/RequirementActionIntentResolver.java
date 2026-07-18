package ua.demo.agentlab.ui.discovery.interaction.scope;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class RequirementActionIntentResolver {

    public Set<SemanticAction> resolve(StructuredBehaviorContract contract) {
        Set<SemanticAction> result = new LinkedHashSet<>();
        if (contract == null) return result;
        String capability = normalize(contract.capability());
        for (String action : contract.actions()) addFromText(result, normalize(action));
        addFromText(result, capability);
        return Set.copyOf(result);
    }

    private void addFromText(Set<SemanticAction> target, String value) {
        if (containsAny(value, "authenticate", "authentication", "login", "sign in")) {
            target.add(SemanticAction.TYPE);
            target.add(SemanticAction.SUBMIT_FORM);
        }
        if (containsAny(value, "enter", "type", "fill")) target.add(SemanticAction.TYPE);
        if (containsAny(value, "submit")) target.add(SemanticAction.SUBMIT_FORM);
        if (containsAny(value, "open menu", "user menu", "dropdown")) target.add(SemanticAction.OPEN_MENU);
        if (containsAny(value, "logout", "log out", "sign out")) target.add(SemanticAction.LOGOUT);
        if (containsAny(value, "search", "find")) target.add(SemanticAction.SEARCH);
        if (containsAny(value, "filter")) target.add(SemanticAction.FILTER);
        if (containsAny(value, "select", "choose option")) target.add(SemanticAction.SELECT);
        if (containsAny(value, "upload")) target.add(SemanticAction.UPLOAD);
        if (containsAny(value, "download")) target.add(SemanticAction.DOWNLOAD);
        if (containsAny(value, "open modal", "dialog")) target.add(SemanticAction.OPEN_MODAL);
        if (containsAny(value, "navigate", "open page", "open module", "follow link")) target.add(SemanticAction.NAVIGATE);
        if (containsAny(value, "click", "press")) target.add(SemanticAction.CLICK);
        if (containsAny(value, "read", "visible", "display", "inspect")) target.add(SemanticAction.READ);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ');
    }
}
