package ua.demo.agentlab.ui.discovery.evidence.funnel;

import java.util.Locale;
import java.util.Optional;

/** Capability vocabulary used by the universal SPA acceptance boundary. */
public enum SpaAcceptanceCapability {
    AUTHENTICATION,
    AUTHENTICATED_AREA,
    MODULE_NAVIGATION,
    USER_MENU,
    RECORD_LIST,
    FILTER,
    MODAL;

    public static Optional<SpaAcceptanceCapability> from(String value) {
        String normalized = value == null ? "" : value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
