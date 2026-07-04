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
}
