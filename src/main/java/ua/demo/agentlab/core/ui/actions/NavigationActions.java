package ua.demo.agentlab.core.ui.actions;

import org.openqa.selenium.WebDriver;

public class NavigationActions {

    private final WebDriver driver;

    public NavigationActions(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        this.driver = driver;
    }

    public void to(String url) {
        driver.navigate().to(url);
    }

    public void back() {
        driver.navigate().back();
    }

    public void forward() {
        driver.navigate().forward();
    }

    public void refresh() {
        driver.navigate().refresh();
    }
}
