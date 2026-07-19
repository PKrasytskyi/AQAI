package unit.tests.ai.ui.generation;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectPromptScope;
import ua.demo.agentlab.ai.ui.generation.PromptEvidenceProjectionInvariantGate;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;
import java.util.Map;

public class PromptEvidenceProjectionInvariantGateTest {

    @Test
    public void failsAtProjectionBoundaryWhenConfirmedRequirementLocatorDisappears() {
        PromptLocatorEvidence locator = new PromptLocatorEvidence(
                "userMenuTrigger", "user menu", "css", "span.user-menu", "button", "", "",
                true, 0.9d, "Header", "USER_MENU", 1, 1, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of("requirement-id:REQ-4", "live-targeted-verification"));
        PromptUiEvidence evidence = new PromptUiEvidence(
                "DashboardPage", "/dashboard", true, List.of("LoginPage"), List.of("REQ-4"),
                List.of(), List.of(), List.of(locator), List.of(), List.of(), List.of(), List.of(),
                List.of("test"), 0.9d);
        AiContextPackage context = new AiContextPackage(
                "POM",
                null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(locator), evidence);
        AiPageObjectPromptScope source = new AiPageObjectPromptScope(
                "DashboardPage", "DashboardPage", null, context, context, List.of(), null, Map.of());
        PromptReadyPomScope projected = new PromptReadyPomScope(
                "DashboardPage", "/dashboard", true, List.of("LoginPage"), List.of("REQ-4"),
                List.of(), List.of(), List.of(), List.of(), List.of(), 0.9d);

        IllegalStateException error = Assert.expectThrows(IllegalStateException.class,
                () -> new PromptEvidenceProjectionInvariantGate().validate(source, projected));

        Assert.assertTrue(error.getMessage().startsWith("EVIDENCE_PROJECTION_MISMATCH"));
    }
}
