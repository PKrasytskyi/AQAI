package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.WebDriver;

import java.util.List;

public class BrowserCapabilityAdapterRegistry {
    private final List<BrowserCapabilityAdapter> adapters;

    public BrowserCapabilityAdapterRegistry() {
        this(List.of(new HttpAuthCapabilityAdapter(), new AlertCapabilityAdapter(), new WindowCapabilityAdapter(),
                new UploadCapabilityAdapter(), new HoverCapabilityAdapter(), new SliderCapabilityAdapter()));
    }

    public BrowserCapabilityAdapterRegistry(List<BrowserCapabilityAdapter> adapters) {
        this.adapters = adapters == null ? List.of() : List.copyOf(adapters);
    }

    public boolean supports(BrowserCapabilityAction action) {
        return action != null && adapters.stream().anyMatch(adapter -> adapter.supports(action));
    }

    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        if (driver == null || request == null || request.action() == null) {
            return BrowserCapabilityResult.unsupported("Browser capability request is incomplete");
        }
        return adapters.stream().filter(adapter -> adapter.supports(request.action())).findFirst()
                .map(adapter -> adapter.execute(driver, request))
                .orElseGet(() -> BrowserCapabilityResult.unsupported("No adapter for " + request.action()));
    }
}
