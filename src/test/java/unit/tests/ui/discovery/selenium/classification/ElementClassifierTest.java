package unit.tests.ui.discovery.selenium.classification;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.selenium.classification.ElementClassifier;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;

import java.util.Map;

public class ElementClassifierTest {

    @Test
    public void classifiesDropdownLinksAsMenuItemsInsteadOfMenuTriggers() {
        RawElement about = element(
                "a", "About", "menuitem", "#", "oxd-userdropdown-link", Map.of());

        var classification = new ElementClassifier().classify("dashboard", about);

        Assert.assertEquals(classification.technicalType(), "LINK");
        Assert.assertEquals(classification.semanticType(), "MENU_ITEM");
    }

    @Test
    public void classifiesUserDropdownTabAsMenuTrigger() {
        RawElement opener = element(
                "span", "Jane Doe", "", "", "oxd-userdropdown-tab", Map.of("aria-haspopup", "true"));

        var classification = new ElementClassifier().classify("dashboard", opener);

        Assert.assertEquals(classification.technicalType(), "BUTTON");
        Assert.assertEquals(classification.semanticType(), "USER_MENU_TRIGGER");
    }

    private RawElement element(
            String tag,
            String text,
            String role,
            String href,
            String cssClass,
            Map<String, String> attributes
    ) {
        return new RawElement(
                "raw", tag, "", text, "", "", "", "", role, href, "", cssClass,
                true, true, false, attributes);
    }
}
