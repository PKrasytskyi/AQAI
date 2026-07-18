package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.ArrayList;
import java.util.List;

public final class HeaderAndUserMenuDetector implements ComponentDetectionStrategy {
    private final ComponentEvidenceMatcher matcher = new ComponentEvidenceMatcher();

    @Override
    public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<ComponentCandidate> result = new ArrayList<>();
        add(result, "user-menu", "UserMenuComponent", ComponentType.USER_MENU, 0.80d,
                matcher.ids(context, "user menu", "user-menu", "profile", "account", "logout", "sign out"),
                List.of("candidate-only-until-targeted-verification"), List.of("component:user-menu-evidence"));
        add(result, "header", "HeaderComponent", ComponentType.HEADER, 0.82d,
                matcher.ids(context, "header", "topbar", "toolbar", "appbar"), List.of(),
                List.of("component:header-evidence"));
        return result;
    }

    private void add(List<ComponentCandidate> output, String id, String name, ComponentType type, double confidence,
                     List<String> elements, List<String> risks, List<String> trace) {
        if (!elements.isEmpty()) output.add(new ComponentCandidate(id, name, type, elements, confidence, risks, trace));
    }
}
