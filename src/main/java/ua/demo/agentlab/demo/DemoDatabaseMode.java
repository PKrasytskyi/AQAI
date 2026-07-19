package ua.demo.agentlab.demo;

import java.util.Locale;

public enum DemoDatabaseMode {
    WITHOUT_DB_BASELINE("without-db-baseline"),
    WITH_DB_REQUIRED("with-db-required"),
    ENVIRONMENT_CONTROLLED("environment-controlled"),
    UNKNOWN("");

    private final String wireName;

    DemoDatabaseMode(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static DemoDatabaseMode from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (DemoDatabaseMode mode : values()) {
            if (mode.wireName.equals(normalized)) {
                return mode;
            }
        }
        return UNKNOWN;
    }
}
