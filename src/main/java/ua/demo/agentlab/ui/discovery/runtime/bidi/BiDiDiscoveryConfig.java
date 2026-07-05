package ua.demo.agentlab.ui.discovery.runtime.bidi;

import java.io.InputStream;
import java.util.Properties;

public record BiDiDiscoveryConfig(
        boolean enabled,
        boolean collectNetwork,
        boolean collectConsole,
        boolean collectDomMutations,
        int maxBufferedEvents
) {
    public BiDiDiscoveryConfig {
        maxBufferedEvents = Math.max(100, maxBufferedEvents);
    }

    public static BiDiDiscoveryConfig disabled() {
        return new BiDiDiscoveryConfig(false, true, true, false, 2_000);
    }

    public static BiDiDiscoveryConfig fromRuntime() {
        boolean collectorEnabled = "bidi".equalsIgnoreCase(firstNonBlank(
                System.getProperty("ui.runtime.evidence.collector"),
                System.getenv("UI_RUNTIME_EVIDENCE_COLLECTOR"),
                frameworkProperty("ui.runtime.evidence.collector")
        ));
        boolean explicitlyEnabled = Boolean.parseBoolean(firstNonBlank(
                System.getProperty("ui.bidi.enabled"),
                System.getenv("UI_BIDI_ENABLED"),
                frameworkProperty("ui.bidi.enabled"),
                "false"
        ));
        boolean enabled = collectorEnabled || explicitlyEnabled;
        int maxEvents = parseInt(firstNonBlank(
                System.getProperty("ui.bidi.max-buffered-events"),
                System.getenv("UI_BIDI_MAX_BUFFERED_EVENTS"),
                frameworkProperty("ui.bidi.max-buffered-events"),
                "5000"
        ), 5000);
        return new BiDiDiscoveryConfig(
                enabled,
                true,
                true,
                true,
                maxEvents
        );
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String frameworkProperty(String key) {
        Properties properties = new Properties();
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("framework.properties")) {
            if (input == null) {
                return "";
            }
            properties.load(input);
            return properties.getProperty(key, "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
