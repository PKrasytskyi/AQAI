package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.core.ui.driver.DriverFactory;

import java.util.Objects;

/** Owns exactly one browser used by a targeted verification run. */
public final class BrowserVerificationSession implements AutoCloseable {

    private final DriverFactory driverFactory;
    private WebDriver driver;

    public BrowserVerificationSession(DriverFactory driverFactory) {
        this.driverFactory = Objects.requireNonNull(driverFactory, "driverFactory");
    }

    public WebDriver open() {
        if (driver != null) {
            throw new IllegalStateException("Browser verification session is already open");
        }
        driver = driverFactory.createDriver();
        return driver;
    }

    public WebDriver driver() {
        if (driver == null) {
            throw new IllegalStateException("Browser verification session is not open");
        }
        return driver;
    }

    @Override
    public void close() {
        if (driver == null) return;
        try {
            driverFactory.shutdownDriver(driver);
        } finally {
            driver = null;
        }
    }
}
