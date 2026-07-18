package ua.demo.agentlab.ui.capability;

import java.util.Locale;
import java.util.List;

/** Defines how an authenticated user reaches the logout action. */
public enum LogoutAccessMode {
    USER_MENU,
    DIRECT_CONTROL,
    UNKNOWN;

    public List<String> lifecycle() {
        return switch (this) {
            case USER_MENU -> List.of("AUTHENTICATION", "AUTHENTICATED_AREA", "USER_MENU", "LOGOUT");
            case DIRECT_CONTROL -> List.of(
                    "AUTHENTICATION", "AUTHENTICATED_AREA", "DIRECT_LOGOUT_CONTROL", "LOGOUT");
            case UNKNOWN -> List.of();
        };
    }

    public static LogoutAccessMode from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
