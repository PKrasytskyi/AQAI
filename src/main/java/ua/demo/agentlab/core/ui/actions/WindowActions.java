package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import java.util.Set;

public class WindowActions {

    private final WebDriver driver;

    public WindowActions(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        this.driver = driver;
    }

    public String currentHandle() {
        return driver.getWindowHandle();
    }

    public void switchToNewWindow() {
        driver.switchTo().newWindow(WindowType.WINDOW);
    }

    public void switchToNewTab() {
        driver.switchTo().newWindow(WindowType.TAB);
    }

    public void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }

    public Set<String> allHandles() {
        return driver.getWindowHandles();
    }

    public int count() {
        return driver.getWindowHandles().size();
    }

    public void switchToAnotherWindow(String currentHandle) {
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(currentHandle)) {
                driver.switchTo().window(handle);
                return;
            }
        }
        throw new IllegalStateException("No other browser window found");
    }

    public void switchToLastWindow() {
        String lastHandle = null;
        for (String handle : driver.getWindowHandles()) {
            lastHandle = handle;
        }
        if (lastHandle == null) {
            throw new IllegalStateException("No browser window found");
        }
        driver.switchTo().window(lastHandle);
    }

    public void closeCurrentWindow() {
        driver.close();
    }

    public void closeCurrentWindowAndSwitchBack(String handleToSwitchBack) {
        driver.close();
        driver.switchTo().window(handleToSwitchBack);
    }

    public Dimension size() {
        return driver.manage().window().getSize();
    }

    public void setSize(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    public Point position() {
        return driver.manage().window().getPosition();
    }

    public void setPosition(int x, int y) {
        driver.manage().window().setPosition(new Point(x, y));
    }

    public void maximize() {
        driver.manage().window().maximize();
    }

    public void minimize() {
        driver.manage().window().minimize();
    }

    public void fullscreen() {
        driver.manage().window().fullscreen();
    }
}
