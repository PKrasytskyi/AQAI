package ua.demo.agentlab.ui.discovery.selenium.model;

import java.util.Locale;

public final class DiscoveryLocatorKey {

    private DiscoveryLocatorKey() {
    }

    public static String key(String pageId, String strategy, String value) {
        return normalize(pageId) + "::" + normalize(strategy) + "::" + normalize(value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
