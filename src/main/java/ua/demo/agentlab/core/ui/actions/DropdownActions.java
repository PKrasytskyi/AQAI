package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import ua.demo.agentlab.core.ui.wait.WaitActions;

import java.util.List;

public class DropdownActions {

    private final WaitActions waits;

    public DropdownActions(WebDriver driver, WaitActions waits) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (waits == null) {
            throw new IllegalArgumentException("waits cannot be null");
        }

        this.waits = waits;
    }

    public void selectByVisibleText(By locator, String text) {
        new Select(waits.visible(locator)).selectByVisibleText(text);
    }

    public void selectByValue(By locator, String value) {
        new Select(waits.visible(locator)).selectByValue(value);
    }

    public void selectByIndex(By locator, int index) {
        new Select(waits.visible(locator)).selectByIndex(index);
    }

    public String selectedText(By locator) {
        return new Select(waits.visible(locator)).getFirstSelectedOption().getText();
    }

    public List<WebElement> selectedOptions(By locator) {
        return new Select(waits.visible(locator)).getAllSelectedOptions();
    }

    public List<WebElement> allOptions(By locator) {
        return new Select(waits.visible(locator)).getOptions();
    }

    public void deselectAll(By locator) {
        new Select(waits.visible(locator)).deselectAll();
    }

    public void deselectByVisibleText(By locator, String text) {
        new Select(waits.visible(locator)).deselectByVisibleText(text);
    }

    public void deselectByValue(By locator, String value) {
        new Select(waits.visible(locator)).deselectByValue(value);
    }

    public void deselectByIndex(By locator, int index) {
        new Select(waits.visible(locator)).deselectByIndex(index);
    }

    public boolean isMultiple(By locator) {
        return new Select(waits.visible(locator)).isMultiple();
    }
}
