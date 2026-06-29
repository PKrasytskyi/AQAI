package ua.demo.agentlab.core.ui;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.core.ui.actions.AlertActions;
import ua.demo.agentlab.core.ui.actions.AdvancedUserActions;
import ua.demo.agentlab.core.ui.actions.DropdownActions;
import ua.demo.agentlab.core.ui.actions.ElementActions;
import ua.demo.agentlab.core.ui.actions.FrameActions;
import ua.demo.agentlab.core.ui.actions.JavascriptActions;
import ua.demo.agentlab.core.ui.actions.NavigationActions;
import ua.demo.agentlab.core.ui.actions.WindowActions;
import ua.demo.agentlab.core.ui.wait.WaitActions;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final UiRuntimeConfig runtimeConfig;
    protected final WaitActions waits;
    protected final ElementActions elements;
    protected final FrameActions frames;
    protected final DropdownActions dropdowns;
    protected final AlertActions alerts;
    protected final WindowActions windows;
    protected final NavigationActions navigation;
    protected final JavascriptActions scripts;
    protected final AdvancedUserActions interactions;

    protected BasePage(WebDriver driver, UiRuntimeConfig runtimeConfig) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtimeConfig must not be null");
        }

        this.driver = driver;
        this.runtimeConfig = runtimeConfig;
        this.waits = new WaitActions(driver, runtimeConfig.getDefaultTimeout());
        this.elements = new ElementActions(driver, waits);
        this.frames = new FrameActions(driver, waits);
        this.dropdowns = new DropdownActions(driver, waits);
        this.alerts = new AlertActions(driver, waits);
        this.windows = new WindowActions(driver);
        this.navigation = new NavigationActions(driver);
        this.scripts = new JavascriptActions(driver, waits);
        this.interactions = new AdvancedUserActions(driver, waits);
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    public void open(String relativePath) {
        String baseUrl = runtimeConfig.getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedRelativePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        navigation.to(normalizedBaseUrl + normalizedRelativePath);
    }

    public void openAbsolute(String url) {
        navigation.to(url);
    }
}
