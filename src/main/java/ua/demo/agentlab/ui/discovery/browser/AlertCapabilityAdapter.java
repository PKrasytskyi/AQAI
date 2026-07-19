package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;

public class AlertCapabilityAdapter implements BrowserCapabilityAdapter {
    @Override
    public boolean supports(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.ACCEPT_ALERT
                || action == BrowserCapabilityAction.DISMISS_ALERT
                || action == BrowserCapabilityAction.ENTER_ALERT_TEXT;
    }

    @Override
    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        try {
            Alert alert = driver.switchTo().alert();
            if (request.action() == BrowserCapabilityAction.ENTER_ALERT_TEXT) {
                if (request.value().isBlank()) return BrowserCapabilityResult.failed("Alert text input is missing");
                alert.sendKeys(request.value());
                alert.accept();
            } else if (request.action() == BrowserCapabilityAction.DISMISS_ALERT) {
                alert.dismiss();
            } else {
                alert.accept();
            }
            return BrowserCapabilityResult.passed("Browser dialog action completed");
        } catch (RuntimeException exception) {
            return BrowserCapabilityResult.failed("Browser dialog action failed: " + concise(exception));
        }
    }

    private String concise(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
