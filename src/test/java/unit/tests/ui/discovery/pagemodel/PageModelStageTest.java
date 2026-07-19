package unit.tests.ui.discovery.pagemodel;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.stage.*;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.List;
import java.util.Map;

public class PageModelStageTest {

    @Test
    public void rawNormalizationIdentityAndLocatorAssemblyAreDeterministic() {
        RawElement username = raw("input", "text", "username", "username");
        Assert.assertEquals(new RawElementNormalizer().normalize(List.of(username, username)).size(), 2,
                "Raw discovery multiplicity must be preserved until semantic identity resolution");

        ElementIdentityResolver identities = new ElementIdentityResolver();
        Assert.assertEquals(identities.semanticName("User name"), "userName");
        Assert.assertEquals(identities.buildElementId("login", "username"), "login:element:username");

        var locators = new LocatorCandidateAssembler().fromRaw(username);
        Assert.assertEquals(locators.get(0).strategy(), "id");
        Assert.assertEquals(locators.get(0).value(), "username");
    }

    @Test
    public void actionAssemblyDoesNotDependOnLocatorSelection() {
        PageActionAssembler actions = new PageActionAssembler(new ElementIdentityResolver());
        Assert.assertEquals(actions.infer("login:submit", "BUTTON", "Login", "", true)
                .stream().map(action -> action.actionType()).toList(), List.of("click"));
        Assert.assertEquals(actions.forField("login:username", "text")
                .stream().map(action -> action.actionType()).toList(), List.of("type", "clear"));
    }

    private RawElement raw(String tag, String type, String id, String name) {
        return new RawElement("raw-1", tag, type, "", id, name, "", "", "", "", "", "",
                true, true, false, Map.of(), Map.of("id::" + id, 1), Map.of(), Map.of());
    }
}
