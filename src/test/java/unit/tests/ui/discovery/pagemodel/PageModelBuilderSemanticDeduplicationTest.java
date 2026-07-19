package unit.tests.ui.discovery.pagemodel;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.List;
import java.util.Map;

public class PageModelBuilderSemanticDeduplicationTest {

    @Test
    public void collapsesRepeatedRawOccurrenceBeforeAssigningSemanticElementIdentity() {
        RawElement trigger = new RawElement("raw-32", "span", "", "User", "", "", "", "", "", "",
                "", "oxd-userdropdown-tab", true, true, false, Map.of("class", "oxd-userdropdown-tab"),
                Map.of("css::span.oxd-userdropdown-tab", 1),
                Map.of("css::span.oxd-userdropdown-tab", 1), Map.of("css::span.oxd-userdropdown-tab", "header"));
        DiscoveredPageSnapshot page = new DiscoveredPageSnapshot("dashboard", "https://example.test/dashboard",
                "Dashboard", List.of(), List.of(), List.of(), List.of(), false, true, List.of(), List.of(),
                "fingerprint", null, null, List.of(trigger, trigger));

        var result = new PageModelBuilder().build(
                new UiDiscoverySnapshot("test", "Test", "unit-test", List.of(), List.of()),
                new SeleniumDiscoveryResult("https://example.test", List.of(page), List.of()));

        Assert.assertEquals(result.pages().get(0).elements().size(), 1);
        Assert.assertEquals(result.pages().get(0).elements().get(0).elementId(), "dashboard:element:user-menu-trigger");
    }

    @Test
    public void preservesDifferentElementsWhenStateSnapshotsReuseRawElementIds() {
        RawElement closedState = new RawElement("raw-36", "button", "", "Quick Launch", "", "", "", "",
                "", "", "", "", true, true, false, Map.of(), Map.of("css::button", 1),
                Map.of("css::button", 1), Map.of("css::button", "page"));
        RawElement openMenuState = new RawElement("raw-36", "a", "", "Logout", "", "", "", "",
                "menuitem", "/auth/logout", "", "oxd-userdropdown-link", true, true, false,
                Map.of("href", "/auth/logout", "role", "menuitem"),
                Map.of("css::a[href='/auth/logout']", 1), Map.of("css::a[href='/auth/logout']", 1),
                Map.of("css::a[href='/auth/logout']", "header"));
        DiscoveredPageSnapshot page = new DiscoveredPageSnapshot("dashboard", "https://example.test/dashboard",
                "Dashboard", List.of(), List.of(), List.of(), List.of(), false, true, List.of(), List.of(),
                "fingerprint", null, null, List.of(closedState, openMenuState));

        var result = new PageModelBuilder().build(
                new UiDiscoverySnapshot("test", "Test", "unit-test", List.of(), List.of()),
                new SeleniumDiscoveryResult("https://example.test", List.of(page), List.of()));

        Assert.assertEquals(result.pages().get(0).elements().size(), 2);
        Assert.assertTrue(result.pages().get(0).elements().stream()
                .anyMatch(element -> element.elementId().equals("dashboard:element:logout")));
    }
}
