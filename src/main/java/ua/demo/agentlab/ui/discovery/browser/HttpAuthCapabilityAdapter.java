package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriver;

import java.net.URI;

public class HttpAuthCapabilityAdapter implements BrowserCapabilityAdapter {
    @Override
    public boolean supports(BrowserCapabilityAction action) {
        return action == BrowserCapabilityAction.HTTP_AUTHENTICATE;
    }

    @Override
    public BrowserCapabilityResult execute(WebDriver driver, BrowserCapabilityRequest request) {
        if (!(driver instanceof HasAuthentication authentication)) {
            return BrowserCapabilityResult.unsupported("WebDriver does not expose Selenium HasAuthentication");
        }
        if (request.username().isBlank() || request.targetUrl().isBlank()) {
            return BrowserCapabilityResult.failed("HTTP authentication requires username and targetUrl");
        }
        URI target = URI.create(request.targetUrl());
        authentication.register(uri -> target.getHost() != null && target.getHost().equalsIgnoreCase(uri.getHost()),
                UsernameAndPassword.of(request.username(), request.password()));
        driver.navigate().to(target.toString());
        return BrowserCapabilityResult.passed("Selenium HTTP authentication registered for target origin");
    }
}
