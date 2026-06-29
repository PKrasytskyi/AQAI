package ua.demo.agentlab.core.ui.wait;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtils {

    private WaitUtils() {
    }

    public static WebElement waitForVisible(WebDriver driver, By locator, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static boolean waitForUrlContains(WebDriver driver, String urlPart, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.urlContains(urlPart));
    }

    public static boolean waitForUrlToBe(WebDriver driver, String url, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.urlToBe(url));
    }

    public static boolean waitForTitleContains(WebDriver driver, String titlePart, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.titleContains(titlePart));
    }

    public static WebDriver waitForFrameAndSwitchToIt(WebDriver driver, String nameOrId, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    public static WebDriver waitForFrameAndSwitchToIt(WebDriver driver, WebElement frameElement, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameElement));
    }

    public static Alert waitForAlert(WebDriver driver, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.alertIsPresent());
    }

    public static boolean waitForDisappearance(WebDriver driver, By locator, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public static WebElement waitForAppearance(WebDriver driver, By locator, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static boolean waitForElementEnabled(WebDriver driver, By locator, Duration timeout) {
        return buildWait(driver, timeout).until(webDriver -> webDriver.findElement(locator).isEnabled());
    }

    public static boolean waitForElementDisabled(WebDriver driver, By locator, Duration timeout) {
        return buildWait(driver, timeout).until(webDriver -> !webDriver.findElement(locator).isEnabled());
    }

    public static boolean waitForTextPresent(WebDriver driver, By locator, String text, Duration timeout) {
        return buildWait(driver, timeout).until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    private static WebDriverWait buildWait(WebDriver driver, Duration timeout) {
        return new WebDriverWait(driver, timeout);
    }
}
