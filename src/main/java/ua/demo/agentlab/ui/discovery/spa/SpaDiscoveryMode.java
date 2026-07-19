package ua.demo.agentlab.ui.discovery.spa;

import java.util.Locale;

public enum SpaDiscoveryMode {
    INVENTORY,
    TARGETED,
    REFRESH,
    FORCE;

    public static SpaDiscoveryMode from(String value) {
        if (value == null || value.isBlank()) {
            return INVENTORY;
        }
        try {
            return SpaDiscoveryMode.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return INVENTORY;
        }
    }
}
