package ua.demo.agentlab.ai.ui.generation;

import java.util.List;

public record PromptPage(
        String pageName,
        String route,
        boolean eligible,
        boolean routeOnlyContract,
        boolean hasRawEvidence,
        boolean hasAllowedLocators,
        boolean hasStableCacheEvidence,
        List<String> reasons
) {
    public PromptPage {
        pageName = pageName == null ? "" : pageName.trim();
        route = route == null ? "" : route.trim();
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
