package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFlowModel;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;

/** Maps one observed transition without inferring additional routes. */
public final class PageTransitionAssembler {

    public PageFlowModel assemble(DiscoveredTransition transition) {
        String actionLabel = firstNonBlank(transition.actionLabel(), transition.actionType(), "action");
        return new PageFlowModel(transition.fromPageId() + ":flow:" + sanitize(actionLabel),
                transition.fromPageId(), actionLabel, transition.actionType(), transition.toPageId(),
                transition.toUrl(), transition.success());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    private String sanitize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
