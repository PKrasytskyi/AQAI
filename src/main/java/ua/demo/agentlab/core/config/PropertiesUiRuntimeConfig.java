package ua.demo.agentlab.core.config;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

public class PropertiesUiRuntimeConfig implements UiRuntimeConfig {

    private final Properties properties = new Properties();

    public PropertiesUiRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesUiRuntimeConfig(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {

            if (input == null) {
                throw new IllegalStateException("Cannot find config resource: " + resourceName);
            }

            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load config resource: " + resourceName, exception);
        }
    }

    @Override
    public String getBaseUrl() {
        return firstRequired("ui.base-url", "project.base-url", "test.base-url");
    }

    @Override
    public String getBrowser() {
        return firstPresent("ui.browser", "test.browser", "chrome");
    }

    @Override
    public boolean isHeadless() {
        return Boolean.parseBoolean(firstPresent("ui.headless", "test.headless", "false"));
    }

    @Override
    public Duration getDefaultTimeout() {
        long seconds = Long.parseLong(firstPresent("ui.timeout-seconds", "test.timeout-seconds", "10"));
        return Duration.ofSeconds(seconds);
    }

    private String firstRequired(String... keys) {
        for (String key : keys) {
            String value = readValue(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        throw new IllegalStateException("Missing required property. Expected one of: " + String.join(", ", keys));
    }

    private String firstPresent(String primaryKey, String fallbackKey, String defaultValue) {
        String primaryValue = readValue(primaryKey);
        if (primaryValue != null && !primaryValue.isBlank()) {
            return primaryValue.trim();
        }

        String fallbackValue = readValue(fallbackKey);
        if (fallbackValue != null && !fallbackValue.isBlank()) {
            return fallbackValue.trim();
        }

        return Objects.requireNonNull(defaultValue);
    }

    private String readValue(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return null;
    }
}
