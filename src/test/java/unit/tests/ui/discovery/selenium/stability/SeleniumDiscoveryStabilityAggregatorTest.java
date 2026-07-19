package unit.tests.ui.discovery.selenium.stability;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveryLocatorKey;
import ua.demo.agentlab.ui.discovery.selenium.model.RawElement;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.discovery.selenium.stability.SeleniumDiscoveryStabilityAggregator;

import java.util.List;
import java.util.Map;

public class SeleniumDiscoveryStabilityAggregatorTest {

    @Test
    public void submitControlLocatorIsObservedAcrossRuns() {
        SeleniumDiscoveryResult first = runWithSubmitButton();
        SeleniumDiscoveryResult second = runWithSubmitButton();
        SeleniumDiscoveryResult third = runWithSubmitButton();

        SeleniumDiscoveryResult aggregated = new SeleniumDiscoveryStabilityAggregator()
                .aggregate(List.of(first, second, third));

        String key = DiscoveryLocatorKey.key("login", "css", "button[type='submit']");
        Assert.assertEquals(aggregated.locatorObservationCounts().get(key), Integer.valueOf(3));
        Assert.assertEquals(aggregated.discoveryRunCount(), 3);
    }

    @Test
    public void stableSemanticClassLocatorIsObservedAcrossRuns() {
        SeleniumDiscoveryResult aggregated = new SeleniumDiscoveryStabilityAggregator().aggregate(List.of(
                runWithUserMenuTrigger(), runWithUserMenuTrigger(), runWithUserMenuTrigger()));

        String key = DiscoveryLocatorKey.key("dashboard", "css", "span.oxd-userdropdown-tab");
        Assert.assertEquals(aggregated.locatorObservationCounts().get(key), Integer.valueOf(3));
    }

    private SeleniumDiscoveryResult runWithSubmitButton() {
        RawElement submit = new RawElement(
                "raw-submit",
                "button",
                "submit",
                "Login",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                true,
                true,
                false,
                Map.of("type", "submit")
        );
        DiscoveredPageSnapshot page = new DiscoveredPageSnapshot(
                "login",
                "https://example.test/login",
                "Login",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                List.of(),
                List.of(),
                "fingerprint",
                null,
                null,
                List.of(submit)
        );
        return new SeleniumDiscoveryResult("https://example.test", List.of(page), List.of());
    }

    private SeleniumDiscoveryResult runWithUserMenuTrigger() {
        RawElement trigger = new RawElement("raw-menu", "span", "", "User", "", "", "", "", "", "",
                "", "oxd-userdropdown-tab", true, true, false, Map.of("class", "oxd-userdropdown-tab"));
        DiscoveredPageSnapshot page = new DiscoveredPageSnapshot("dashboard", "https://example.test/dashboard",
                "Dashboard", List.of(), List.of(), List.of(), List.of(), false, true, List.of(), List.of(),
                "fingerprint", null, null, List.of(trigger));
        return new SeleniumDiscoveryResult("https://example.test", List.of(page), List.of());
    }
}
