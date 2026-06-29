package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.core.ui.wait.WaitActions;

public class JavascriptActions {

    private final WebDriver driver;
    private final WaitActions waits;

    public JavascriptActions(WebDriver driver, WaitActions waits) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (waits == null) {
            throw new IllegalArgumentException("waits cannot be null");
        }
        this.driver = driver;
        this.waits = waits;
    }

    public Object execute(String script, Object... arguments) {
        return executor().executeScript(script, arguments);
    }

    public Object executeAsync(String script, Object... arguments) {
        return executor().executeAsyncScript(script, arguments);
    }

    public void click(By locator) {
        WebElement element = waits.visible(locator);
        click(element);
    }

    public void click(WebElement element) {
        executor().executeScript("arguments[0].click();", element);
    }

    public void scrollIntoView(By locator) {
        WebElement element = waits.visible(locator);
        scrollIntoView(element);
    }

    public void scrollIntoView(WebElement element) {
        executor().executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
    }

    public void focus(By locator) {
        WebElement element = waits.visible(locator);
        executor().executeScript("arguments[0].focus();", element);
    }

    public void blur(By locator) {
        WebElement element = waits.visible(locator);
        executor().executeScript("arguments[0].blur();", element);
    }

    public void setValue(By locator, String value) {
        WebElement element = waits.visible(locator);
        executor().executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', {bubbles: true})); arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
                element,
                value
        );
    }

    public void setAttribute(By locator, String attributeName, String value) {
        WebElement element = waits.visible(locator);
        executor().executeScript("arguments[0].setAttribute(arguments[1], arguments[2]);", element, attributeName, value);
    }

    public void removeAttribute(By locator, String attributeName) {
        WebElement element = waits.visible(locator);
        executor().executeScript("arguments[0].removeAttribute(arguments[1]);", element, attributeName);
    }

    public void scrollToTop() {
        executor().executeScript("window.scrollTo(0, 0);");
    }

    public void scrollToBottom() {
        executor().executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    private JavascriptExecutor executor() {
        return (JavascriptExecutor) driver;
    }
}
