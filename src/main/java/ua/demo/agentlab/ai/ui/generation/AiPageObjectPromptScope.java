package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.catalog.ConfirmedUiCatalog;

import java.util.List;
import java.util.Map;

public record AiPageObjectPromptScope(
        String pageName,
        String fileStem,
        AiContextScope pageScope,
        AiContextPackage originalContext,
        AiContextPackage scopedContext,
        List<UiTestScenario> pageScenarios,
        AiPageObjectSpec baselineSpec,
        Map<String, Object> scopeTrace,
        ConfirmedUiCatalog confirmedUiCatalog
) {
    public AiPageObjectPromptScope(
            String pageName, String fileStem, AiContextScope pageScope,
            AiContextPackage originalContext, AiContextPackage scopedContext,
            List<UiTestScenario> pageScenarios, AiPageObjectSpec baselineSpec,
            Map<String, Object> scopeTrace
    ) {
        this(pageName, fileStem, pageScope, originalContext, scopedContext, pageScenarios,
                baselineSpec, scopeTrace, null);
    }
    public AiPageObjectPromptScope {
        pageName = pageName == null ? "" : pageName.trim();
        fileStem = fileStem == null || fileStem.isBlank() ? "page" : fileStem.trim();
        pageScenarios = pageScenarios == null ? List.of() : List.copyOf(pageScenarios);
        scopeTrace = scopeTrace == null ? Map.of() : Map.copyOf(scopeTrace);
    }
}
