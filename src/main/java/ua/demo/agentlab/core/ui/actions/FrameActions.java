package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.core.ui.wait.WaitActions;
import ua.demo.agentlab.core.ui.wait.WaitUtils;

public class FrameActions {

    private final WebDriver driver;
    private final WaitActions waits;

    public FrameActions(WebDriver driver, WaitActions waits) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (waits == null) {
            throw new IllegalArgumentException("waits cannot be null");
        }
        this.driver = driver;
        this.waits = waits;
    }

    public void switchToFrameByFrameElement(WebElement frameElement) {
        WaitUtils.waitForFrameAndSwitchToIt(driver, frameElement, waits.defaultTimeout());
    }

    public void switchToFrame(By locator) {
        switchToFrameByFrameElement(waits.visible(locator));
    }

    public void switchToFrameByNameOrId(String nameOrId) {
        WaitUtils.waitForFrameAndSwitchToIt(driver, nameOrId, waits.defaultTimeout());
    }

    public void switchToFrameByIndex(int index) {
        driver.switchTo().frame(index);
    }

    public void switchToParentFrame() {
        driver.switchTo().parentFrame();
    }

    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }
}
