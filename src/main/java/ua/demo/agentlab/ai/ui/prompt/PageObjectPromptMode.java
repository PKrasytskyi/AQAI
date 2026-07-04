package ua.demo.agentlab.ai.ui.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public enum PageObjectPromptMode {
    COMPACT,
    DEBUG;

    private static final String MODE_PROPERTY = "ai.page-object.prompt.mode";
    private static final String DEBUG_PROPERTY = "ai.prompt.debug";
    private static final String CONFIG_RESOURCE = "framework.properties";

    public static PageObjectPromptMode fromRuntime() {
        String debugValue = readSetting(DEBUG_PROPERTY);
        if (isTruthy(debugValue)) {
            return DEBUG;
        }
        String modeValue = readSetting(MODE_PROPERTY);
        if (modeValue == null || modeValue.isBlank()) {
            return COMPACT;
        }
        String normalized = modeValue.trim().toLowerCase(Locale.ROOT);
        if ("debug".equals(normalized) || "full".equals(normalized) || "verbose".equals(normalized)) {
            return DEBUG;
        }
        return COMPACT;
    }

    private static String readSetting(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String envKey = key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = PageObjectPromptMode.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
            return "";
        }
        return properties.getProperty(key, "");
    }

    private static boolean isTruthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }
}
