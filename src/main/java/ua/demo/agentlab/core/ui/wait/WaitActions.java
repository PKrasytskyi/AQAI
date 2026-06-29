package ua.demo.agentlab.core.ui.wait;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class WaitActions {

    private final WebDriver driver;
    private final Duration defaultTimeout;

    public WaitActions(WebDriver driver, Duration defaultTimeout) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (defaultTimeout == null) {
            throw new IllegalArgumentException("defaultTimeout cannot be null");
        }

        this.driver = driver;
        this.defaultTimeout = defaultTimeout;
    }

    public WebElement visible(By locator) {
        return WaitUtils.waitForVisible(driver, locator, defaultTimeout);
    }

    public WebElement clickable(By locator) {
        return WaitUtils.waitForClickable(driver, locator, defaultTimeout);
    }

    public boolean urlContains(String text) {
        return WaitUtils.waitForUrlContains(driver, text, defaultTimeout);
    }

    public boolean urlToBe(String url) {
        return WaitUtils.waitForUrlToBe(driver, url, defaultTimeout);
    }

    public boolean titleContains(String text) {
        return WaitUtils.waitForTitleContains(driver, text, defaultTimeout);
    }

    public boolean disappears(By locator) {
        return WaitUtils.waitForDisappearance(driver, locator, defaultTimeout);
    }

    public boolean appears(By locator) {
        return WaitUtils.waitForAppearance(driver, locator, defaultTimeout).isDisplayed();
    }

    public boolean becomesEnabled(By locator) {
        return WaitUtils.waitForElementEnabled(driver, locator, defaultTimeout);
    }

    public boolean becomesDisabled(By locator) {
        return WaitUtils.waitForElementDisabled(driver, locator, defaultTimeout);
    }

    public boolean textPresent(By locator, String text) {
        return WaitUtils.waitForTextPresent(driver, locator, text, defaultTimeout);
    }

    public Duration defaultTimeout() {
        return defaultTimeout;
    }
}
