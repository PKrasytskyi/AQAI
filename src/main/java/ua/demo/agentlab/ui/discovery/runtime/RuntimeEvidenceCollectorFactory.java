package ua.demo.agentlab.ui.discovery.runtime;

import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiRuntimeEvidenceCollector;
import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiEventBuffer;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

public final class RuntimeEvidenceCollectorFactory {

    private RuntimeEvidenceCollectorFactory() {
    }

    public static RuntimeEvidenceCollector fromRuntime() {
        return fromRuntime(null);
    }

    public static RuntimeEvidenceCollector fromRuntime(BiDiEventBuffer eventBuffer) {
        String mode = firstNonBlank(
                System.getProperty("ui.runtime.evidence.collector"),
                System.getenv("UI_RUNTIME_EVIDENCE_COLLECTOR"),
                frameworkProperty("ui.runtime.evidence.collector"),
                "selenium-log"
        ).toLowerCase(Locale.ROOT);
        if (mode.equals("bidi")) {
            return new BiDiRuntimeEvidenceCollector(eventBuffer);
        }
        return new SeleniumLogRuntimeEvidenceCollector();
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
