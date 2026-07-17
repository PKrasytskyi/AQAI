package ua.demo.agentlab.core.ui.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import ua.demo.agentlab.core.config.UiRuntimeConfig;

import java.util.logging.Level;

public class DefaultDriverFactory implements DriverFactory {

    private final UiRuntimeConfig runtimeConfig;

    public DefaultDriverFactory(UiRuntimeConfig runtimeConfig) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtimeConfig cannot be null");
        }
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    public WebDriver createDriver() {
        String browser = runtimeConfig.getBrowser();

        return switch (browser.toLowerCase()) {
            case "chrome" -> createChromeDriver();
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: " + browser + ". Supported: chrome"
            );
        };
    }

    private WebDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();

        if (runtimeConfig.isHeadless()) {
            options.addArguments("--headless=new");
            // --start-maximized is ignored by headless Chrome. Keep SPA navigation in its desktop layout.
            options.addArguments("--window-size=1920,1080");
        }

        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        LoggingPreferences loggingPreferences = new LoggingPreferences();
        loggingPreferences.enable(LogType.BROWSER, Level.ALL);
        loggingPreferences.enable(LogType.PERFORMANCE, Level.ALL);
        options.setCapability("goog:loggingPrefs", loggingPreferences);

        return new ChromeDriver(options);
    }
}
