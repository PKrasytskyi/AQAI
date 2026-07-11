package ua.demo.agentlab.ui.discovery.selenium.auth;

import java.time.Duration;
import ua.demo.agentlab.config.RuntimeProperties;

public class DiscoveryAuthenticationConfig {

    private final RuntimeProperties properties;

    public DiscoveryAuthenticationConfig() {
        this("framework.properties");
    }

    public DiscoveryAuthenticationConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
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
        return usableSecret(username()) && usableSecret(password());
    }

    private String readValue(String key, String fallback) {
        return properties.readValue(key, fallback);
    }

    private boolean usableSecret(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("${")
                && !value.contains("}");
    }

}
