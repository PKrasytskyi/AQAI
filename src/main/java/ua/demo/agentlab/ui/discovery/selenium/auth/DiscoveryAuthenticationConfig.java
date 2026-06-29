package ua.demo.agentlab.ui.discovery.selenium.auth;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public class DiscoveryAuthenticationConfig {

    private final Properties properties = new Properties();

    public DiscoveryAuthenticationConfig() {
        this("framework.properties");
    }

    public DiscoveryAuthenticationConfig(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load discovery authentication config: " + resourceName, exception);
        }
    }

    public boolean enabled() {
        return Boolean.parseBoolean(readValue("discovery.auth.enabled", "false"));
    }

    public String username() {
        return readValue("discovery.auth.username",
                readValue("test.credentials.valid.username", ""));
    }

    public String password() {
        return readValue("discovery.auth.password",
                readValue("test.credentials.valid.password", ""));
    }

    public String usernameSelector() {
        return readValue("discovery.auth.username-selector",
                "input[name='username'], input#username, input[type='email'], input[name*='user' i], input[id*='user' i]");
    }

    public String passwordSelector() {
        return readValue("discovery.auth.password-selector",
                "input[name='password'], input#password, input[type='password'], input[name*='pass' i], input[id*='pass' i]");
    }

    public String submitSelector() {
        return readValue("discovery.auth.submit-selector",
                "button[type='submit'], input[type='submit'], button, input[type='button']");
    }

    public Duration timeout() {
        String rawValue = readValue("discovery.auth.timeout-seconds", "5");
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(rawValue)));
        } catch (NumberFormatException exception) {
            return Duration.ofSeconds(5);
        }
    }

    public boolean hasCredentials() {
        return !username().isBlank() && !password().isBlank();
    }

    private String readValue(String key, String fallback) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return resolvePlaceholders(systemValue.trim());
        }

        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return resolvePlaceholders(envValue.trim());
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return resolvePlaceholders(propertyValue.trim());
        }

        return fallback;
    }

    private String resolvePlaceholders(String value) {
        String resolved = value;
        for (int index = 0; index < 5; index++) {
            int start = resolved.indexOf("${");
            if (start < 0) {
                return resolved;
            }
            int end = resolved.indexOf('}', start);
            if (end < 0) {
                return resolved;
            }
            String key = resolved.substring(start + 2, end);
            String replacement = readRawValue(key);
            if (replacement == null) {
                return resolved;
            }
            resolved = resolved.substring(0, start) + replacement + resolved.substring(end + 1);
        }
        return resolved;
    }

    private String readRawValue(String key) {
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
        return propertyValue == null || propertyValue.isBlank() ? null : propertyValue.trim();
    }
}
