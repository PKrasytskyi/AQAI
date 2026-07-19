package unit.tests.ui.discovery;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAction;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityAdapterRegistry;
import ua.demo.agentlab.ui.discovery.browser.BrowserCapabilityRequest;

import java.util.Arrays;

public class BrowserCapabilityAdapterRegistryTest {

    @Test
    public void exposesAnAdapterForEveryTypedBrowserCapability() {
        BrowserCapabilityAdapterRegistry registry = new BrowserCapabilityAdapterRegistry();

        Assert.assertTrue(Arrays.stream(BrowserCapabilityAction.values()).allMatch(registry::supports));
    }

    @Test
    public void failsClosedForIncompleteExecutionContext() {
        BrowserCapabilityAdapterRegistry registry = new BrowserCapabilityAdapterRegistry();
        var request = new BrowserCapabilityRequest(BrowserCapabilityAction.HOVER, "css", "button", "", "", "", "");

        Assert.assertFalse(registry.execute(null, request).executed());
        Assert.assertFalse(registry.execute(null, request).verified());
    }
}
