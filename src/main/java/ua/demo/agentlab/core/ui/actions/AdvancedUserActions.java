package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.openqa.selenium.remote.RemoteWebDriver;
import ua.demo.agentlab.core.ui.wait.WaitActions;

import java.time.Duration;

public class AdvancedUserActions {

    private final WebDriver driver;
    private final WaitActions waits;

    public AdvancedUserActions(WebDriver driver, WaitActions waits) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (waits == null) {
            throw new IllegalArgumentException("waits cannot be null");
        }
        this.driver = driver;
        this.waits = waits;
    }

    public void moveTo(By locator) {
        perform().moveToElement(waits.visible(locator)).perform();
    }

    public void hover(By locator) {
        moveTo(locator);
    }

    public void doubleClick(By locator) {
        perform().doubleClick(waits.clickable(locator)).perform();
    }

    public void contextClick(By locator) {
        perform().contextClick(waits.clickable(locator)).perform();
    }

    public void clickAndHold(By locator) {
        perform().clickAndHold(waits.visible(locator)).perform();
    }

    public void release(By locator) {
        perform().release(waits.visible(locator)).perform();
    }

    public void dragAndDrop(By sourceLocator, By targetLocator) {
        WebElement source = waits.visible(sourceLocator);
        WebElement target = waits.visible(targetLocator);
        perform().dragAndDrop(source, target).perform();
    }

    public void dragAndDropBy(By sourceLocator, int xOffset, int yOffset) {
        perform().dragAndDropBy(waits.visible(sourceLocator), xOffset, yOffset).perform();
    }

    public void sendKeys(CharSequence... keys) {
        perform().sendKeys(keys).perform();
    }

    public void sendKeys(By locator, CharSequence... keys) {
        perform().sendKeys(waits.visible(locator), keys).perform();
    }

    public void keyDown(Keys key) {
        perform().keyDown(key).perform();
    }

    public void keyUp(Keys key) {
        perform().keyUp(key).perform();
    }

    public void pause(Duration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("duration cannot be null");
        }
        perform().pause(duration).perform();
    }

    public void scrollToElement(By locator) {
        perform().scrollToElement(waits.visible(locator)).perform();
    }

    public void scrollByAmount(int x, int y) {
        perform().scrollByAmount(x, y).perform();
    }

    public void scrollFromElement(By locator, int x, int y) {
        perform().scrollFromOrigin(WheelInput.ScrollOrigin.fromElement(waits.visible(locator)), x, y).perform();
    }

    public void releaseAll() {
        if (driver instanceof RemoteWebDriver remoteWebDriver) {
            remoteWebDriver.resetInputState();
        }
    }

    private Actions perform() {
        return new Actions(driver);
    }
}
