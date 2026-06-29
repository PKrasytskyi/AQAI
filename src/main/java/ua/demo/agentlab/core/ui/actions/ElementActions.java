package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.core.ui.wait.WaitActions;

import java.util.List;

public class ElementActions {

    private final WebDriver driver;
    private final WaitActions waits;

    public ElementActions(WebDriver driver, WaitActions waits) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (waits == null) {
            throw new IllegalArgumentException("waits cannot be null");
        }

        this.driver = driver;
        this.waits = waits;
    }

    public WebElement find(By locator) {
        return waits.visible(locator);
    }

    public List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    public boolean exists(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    public int count(By locator) {
        return driver.findElements(locator).size();
    }

    public void click(By locator) {
        waits.clickable(locator).click();
    }

    public void click(WebElement element) {
        element.click();
    }

    public void jsClick(By locator) {
        WebElement element = find(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click()", element);
    }

    public void sendKeys(By locator, String value) {
        waits.visible(locator).sendKeys(value);
    }

    public void appendText(By locator, String value) {
        waits.visible(locator).sendKeys(value);
    }

    public void clear(By locator) {
        waits.visible(locator).clear();
    }

    public void clearAndType(By locator, String text) {
        WebElement element = find(locator);
        element.clear();
        element.sendKeys(text);
    }

    public void submit(By locator) {
        waits.visible(locator).submit();
    }

    public boolean isVisible(By locator) {
        return waits.visible(locator).isDisplayed();
    }

    public boolean isEnabled(By locator) {
        return waits.visible(locator).isEnabled();
    }

    public boolean isSelected(By locator) {
        return waits.visible(locator).isSelected();
    }

    public String text(By locator) {
        return waits.visible(locator).getText();
    }

    public String tagName(By locator) {
        return waits.visible(locator).getTagName();
    }

    public String attribute(By locator, String attributeName) {
        return waits.visible(locator).getAttribute(attributeName);
    }

    public String domProperty(By locator, String propertyName) {
        return waits.visible(locator).getDomProperty(propertyName);
    }

    public String cssValue(By locator, String propertyName) {
        return waits.visible(locator).getCssValue(propertyName);
    }

    public Rectangle rect(By locator) {
        return waits.visible(locator).getRect();
    }

    public void selectIfNotSelected(By locator) {
        WebElement element = waits.visible(locator);
        if (!element.isSelected()) {
            element.click();
        }
    }

    public void unselectIfSelected(By locator) {
        WebElement element = waits.visible(locator);
        if (element.isSelected()) {
            element.click();
        }
    }
}
