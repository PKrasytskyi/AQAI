package ua.demo.agentlab.ai.ui.model;

import java.util.List;

public record AiPageObjectSpec(
        String pageName,
        String route,
        String openMethodName,
        List<AiLocatorSpec> locators,
        List<AiMethodSpec> methods
) {
    public AiPageObjectSpec {
        pageName = pageName == null ? "" : pageName.trim();
        route = route == null ? "" : route.trim();
        openMethodName = openMethodName == null ? "" : openMethodName.trim();
        locators = locators == null ? List.of() : List.copyOf(locators);
        methods = methods == null ? List.of() : List.copyOf(methods);
    }
}
