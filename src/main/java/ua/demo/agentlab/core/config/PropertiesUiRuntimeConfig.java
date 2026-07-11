package ua.demo.agentlab.core.config;

import java.time.Duration;
import java.util.Objects;
import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesUiRuntimeConfig implements UiRuntimeConfig {

    private final RuntimeProperties properties;

    public PropertiesUiRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesUiRuntimeConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
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
        return properties.readOptional(key, key.toUpperCase().replace('.', '_').replace('-', '_'));
    }
}
