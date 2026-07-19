package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.compatibility.ActionCompatibilityService;
import ua.demo.agentlab.ui.discovery.interaction.model.SemanticAction;
import ua.demo.agentlab.ui.discovery.interaction.raw.RawUiElementEvidence;

import java.util.List;
import java.util.Map;

public class ActionCompatibilityServiceTest {

    @Test
    public void classifiesTechnicalActionsAndSemanticBusinessIntentWithoutProductRules() {
        ActionCompatibilityService service = new ActionCompatibilityService();
        RawUiElementEvidence username = element("input", "text", "", "username", "");
        RawUiElementEvidence search = element("button", "submit", "Search", "", "");
        RawUiElementEvidence logout = element("a", "", "Logout", "", "/logout");

        Assert.assertTrue(service.evaluate(username, SemanticAction.TYPE).compatible());
        Assert.assertFalse(service.evaluate(username, SemanticAction.CLICK).compatible());
        Assert.assertTrue(service.evaluate(search, SemanticAction.SEARCH).compatible());
        Assert.assertTrue(service.evaluate(search, SemanticAction.SUBMIT_FORM).compatible());
        Assert.assertTrue(service.evaluate(logout, SemanticAction.LOGOUT).compatible());
        Assert.assertTrue(service.evaluate(logout, SemanticAction.NAVIGATE).compatible());
    }

    private RawUiElementEvidence element(String tag, String type, String text, String name, String href) {
        return new RawUiElementEvidence("page", "/", "state", "container", "raw", tag, type, text,
                "", name, "", "", "", href, "", true, true, Map.of(), List.of(), List.of("test"));
    }
}
