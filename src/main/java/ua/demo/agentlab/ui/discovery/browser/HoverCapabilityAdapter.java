package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

public class HoverCapabilityAdapter implements BrowserCapabilityAdapter {
    private final BrowserLocatorResolver locators = new BrowserLocatorResolver();

    @Override
    public boolean supports(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.HOVER;
    }

    @Override
    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        new Actions(driver).moveToElement(driver.findElement(locators.resolve(request))).perform();
        return BrowserCapabilityResult.passed("Pointer moved to the target element");
    }
}
