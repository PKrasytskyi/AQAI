package ua.demo.agentlab.core.ui;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.core.data.PropertiesTestDataProvider;
import ua.demo.agentlab.core.data.ScenarioData;
import ua.demo.agentlab.core.data.TestDataProvider;
import ua.demo.agentlab.core.data.UserCredentials;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.core.ui.driver.DriverFactory;

public abstract class BaseTest {

    protected WebDriver driver;
    protected UiRuntimeConfig runtimeConfig;
    protected DriverFactory driverFactory;
    protected TestDataProvider testDataProvider;

    @BeforeMethod(alwaysRun = true)
    public void setUpBaseTest() {
        this.runtimeConfig = createRuntimeConfig();
        this.driverFactory = createDriverFactory(runtimeConfig);
        this.testDataProvider = createTestDataProvider();
        this.driver = driverFactory.createDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownBaseTest() {
        if (driverFactory != null) {
            driverFactory.shutdownDriver(driver);
            return;
        }

        if (driver != null) {
            driver.quit();
        }
    }

    protected UiRuntimeConfig createRuntimeConfig() {
        return new PropertiesUiRuntimeConfig();
    }

    protected DriverFactory createDriverFactory(UiRuntimeConfig runtimeConfig) {
        return new DefaultDriverFactory(runtimeConfig);
    }

    protected TestDataProvider createTestDataProvider() {
        return new PropertiesTestDataProvider();
    }

    protected UserCredentials credentials(String profileName) {
        return testDataProvider.credentials(profileName);
    }

    protected ScenarioData scenarioData(String dataSetName) {
        return testDataProvider.scenarioData(dataSetName);
    }

    protected String baseUrl() {
        return testDataProvider.getBaseUrl();
    }
}
