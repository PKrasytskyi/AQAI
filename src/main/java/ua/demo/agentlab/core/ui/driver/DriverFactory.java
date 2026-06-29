package ua.demo.agentlab.core.ui.driver;

import org.openqa.selenium.WebDriver;

public interface DriverFactory {

    WebDriver createDriver();

    default void shutdownDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }
}
