package ua.demo.agentlab.ui.discovery.browser;

import org.openqa.selenium.By;

import java.util.Locale;

final class BrowserLocatorResolver {
    By resolve(BrowserCapabilityRequest request) {
        if (request == null || request.locator().isBlank()) {
            throw new IllegalArgumentException("A locator is required for this browser capability");
        }
        return switch (request.strategy().toLowerCase(Locale.ROOT)) {
            case "id" -> By.id(request.locator());
            case "name" -> By.name(request.locator());
            case "xpath" -> By.xpath(request.locator());
            default -> By.cssSelector(request.locator());
        };
    }
}
