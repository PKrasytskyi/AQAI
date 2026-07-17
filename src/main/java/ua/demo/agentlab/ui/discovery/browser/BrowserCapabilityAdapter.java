package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.WebDriver;

public interface BrowserCapabilityAdapter {
    boolean supports(BrowserCapabilityAction action);

    BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request);
}
