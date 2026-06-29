package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.core.ui.wait.WaitActions;
import ua.demo.agentlab.core.ui.wait.WaitUtils;

public class AlertActions {

    private final WebDriver driver;
    private final WaitActions waits;

    public AlertActions(WebDriver driver, WaitActions waits) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (waits == null) {
            throw new IllegalArgumentException("waits cannot be null");
        }
        this.driver = driver;
        this.waits = waits;
    }

    public void accept() {
        WaitUtils.waitForAlert(driver, waits.defaultTimeout()).accept();
    }

    public void dismiss() {
        WaitUtils.waitForAlert(driver, waits.defaultTimeout()).dismiss();
    }

    public String text() {
        return WaitUtils.waitForAlert(driver, waits.defaultTimeout()).getText();
    }

    public void enterText(String text) {
        WaitUtils.waitForAlert(driver, waits.defaultTimeout()).sendKeys(text);
    }

    public boolean isPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException exception) {
            return false;
        }
    }

    public void acceptIfPresent() {
        if (isPresent()) {
            accept();
        }
    }

    public void dismissIfPresent() {
        if (isPresent()) {
            dismiss();
        }
    }
}
