package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesSpaInventoryConfig {

    private final RuntimeProperties properties;

    public PropertiesSpaInventoryConfig() {
        this(new RuntimeProperties());
    }

    public PropertiesSpaInventoryConfig(RuntimeProperties properties) {
        this.properties = properties == null ? new RuntimeProperties() : properties;
    }

    public SpaInventoryConfig load() {
        return new SpaInventoryConfig(
                properties.readBoolean("spa.inventory.enabled", "true"),
                SpaDiscoveryMode.from(properties.readValue("spa.discovery.mode", "targeted")),
                integer("spa.inventory.max-components", 30),
                properties.readBoolean("spa.targeted-verification.enabled", "true"),
                decimal("spa.evidence.min-confirmed-score", 0.80d),
                decimal("spa.evidence.min-live-verification-score", 0.65d),
                integer("spa.evidence.promote-after-successes", 2),
                integer("spa.evidence.demote-after-failures", 2),
                properties.readBoolean("spa.live-verification.enabled", "true"),
                properties.readBoolean("spa.live-verification.execute-session-ending-actions", "false"),
                properties.readBoolean("spa.live-verification.execute-safe-actions", "true"),
                properties.readBoolean("spa.live-verification.execute-data-actions", "false"),
                properties.readBoolean("spa.evidence.retention.enabled", "true"),
                integer("spa.evidence.retention.degraded-days", 14),
                integer("spa.evidence.retention.orphan-days", 30),
                properties.readBoolean("spa.evidence.retention.hard-delete", "false")
        );
    }

    private int integer(String key, int fallback) {
        try {
            return Integer.parseInt(properties.readValue(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double decimal(String key, double fallback) {
        try {
            return Double.parseDouble(properties.readValue(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
