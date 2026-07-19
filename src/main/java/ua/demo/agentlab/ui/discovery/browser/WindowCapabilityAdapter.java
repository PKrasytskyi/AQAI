package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.WebDriver;

import java.util.LinkedHashSet;
import java.util.Set;

public class WindowCapabilityAdapter implements BrowserCapabilityAdapter {
    private final BrowserLocatorResolver locators = new BrowserLocatorResolver();

    @Override
    public boolean supports(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.OPEN_NEW_WINDOW || action == BrowserCapabilityAction.SWITCH_WINDOW;
    }

    @Override
    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        try {
            if (request.action() == BrowserCapabilityAction.SWITCH_WINDOW) {
                String current = driver.getWindowHandle();
                return driver.getWindowHandles().stream().filter(handle -> !handle.equals(current)).findFirst()
                        .map(handle -> {
                            driver.switchTo().window(handle);
                            return BrowserCapabilityResult.passed("Switched to another browser window");
                        }).orElseGet(() -> BrowserCapabilityResult.failed("No secondary browser window exists"));
            }
            Set<String> before = new LinkedHashSet<>(driver.getWindowHandles());
            driver.findElement(locators.resolve(request)).click();
            Set<String> after = new LinkedHashSet<>(driver.getWindowHandles());
            after.removeAll(before);
            if (after.isEmpty()) return BrowserCapabilityResult.failed("Action did not open a new browser window");
            driver.switchTo().window(after.iterator().next());
            return BrowserCapabilityResult.passed("New browser window opened and selected");
        } catch (RuntimeException exception) {
            return BrowserCapabilityResult.failed("Window action failed: " + exception.getClass().getSimpleName());
        }
    }
}
