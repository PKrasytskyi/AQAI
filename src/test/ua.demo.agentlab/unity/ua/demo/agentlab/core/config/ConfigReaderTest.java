package ua.demo.agentlab.core.config;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigReaderTest {

    @Test
    public void resolvesFrameworkPlaceholdersFromRuntimeProperties() {
        System.setProperty("TEST_VALID_USERNAME", "runtime-user");
        System.setProperty("TEST_VALID_PASSWORD", "runtime-password");
        System.setProperty("API_AUTH_TOKEN", "runtime-token");
        try {
            Assert.assertEquals(ConfigReader.getValidUsername(), "runtime-user");
            Assert.assertEquals(ConfigReader.getValidPassword(), "runtime-password");
            Assert.assertEquals(ConfigReader.getApiAuthToken(), "runtime-token");
        } finally {
            System.clearProperty("TEST_VALID_USERNAME");
            System.clearProperty("TEST_VALID_PASSWORD");
            System.clearProperty("API_AUTH_TOKEN");
        }
    }
}
