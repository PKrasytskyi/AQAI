package ua.demo.agentlab.core.data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesTestDataProvider implements TestDataProvider {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final Properties properties = new Properties();
    private final Properties frameworkProperties = new Properties();

    public PropertiesTestDataProvider() {
        this("test-data.properties");
    }

    public PropertiesTestDataProvider(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {

            if (input == null) {
                throw new IllegalStateException("Cannot find test-data resource: " + resourceName);
            }

            properties.load(input);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load test-data resource: " + resourceName, exception);
        }

        loadOptionalFrameworkProperties();
    }

    @Override
    public String getBaseUrl() {
        return firstRequired("base.url", "project.base-url", "ui.base-url", "test.base-url");
    }

    @Override
    public UserCredentials credentials(String profileName) {
        String prefix = "credentials." + profileName + ".";
        return new UserCredentials(
                required(prefix + "username"),
                required(prefix + "password")
        );
    }

    @Override
    public ScenarioData scenarioData(String dataSetName) {
        String prefix = "data." + dataSetName + ".";
        Map<String, String> values = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String fieldName = key.substring(prefix.length());
                values.put(fieldName, required(key));
            }
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("No scenario data found for dataset: " + dataSetName);
        }

        return ScenarioData.of(values);
    }

    private String required(String key) {
        String value = readResolvedValue(key, new HashSet<>());
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }

        return value;
    }

    private String firstRequired(String... keys) {
        for (String key : keys) {
            String value = readResolvedValue(key, new HashSet<>());
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        throw new IllegalStateException("Missing required property. Expected one of: " + String.join(", ", keys));
    }

    private String readResolvedValue(String key, Set<String> resolvingKeys) {
        String value = readRawValue(key);
        if (value == null || value.isBlank()) {
            return value;
        }

        return resolvePlaceholders(value.trim(), resolvingKeys);
    }

    private String resolvePlaceholders(String value, Set<String> resolvingKeys) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String placeholderKey = matcher.group(1).trim();
            if (!resolvingKeys.add(placeholderKey)) {
                throw new IllegalStateException("Circular property placeholder detected: " + placeholderKey);
            }
            String replacement = readResolvedValue(placeholderKey, resolvingKeys);
            resolvingKeys.remove(placeholderKey);
            if (replacement == null) {
                throw new IllegalStateException("Missing property for placeholder: " + placeholderKey);
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
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
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String frameworkValue = frameworkProperties.getProperty(key);
        if (frameworkValue != null && !frameworkValue.isBlank()) {
            return frameworkValue.trim();
        }

        return null;
    }

    private void loadOptionalFrameworkProperties() {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("framework.properties")) {
            if (input != null) {
                frameworkProperties.load(input);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load framework properties for test-data resolution", exception);
        }
    }
}
