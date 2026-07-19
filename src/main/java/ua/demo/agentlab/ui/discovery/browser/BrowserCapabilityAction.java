package ua.demo.agentlab.ui.discovery.browser;

public enum BrowserCapabilityAction {
    HTTP_AUTHENTICATE,
    ACCEPT_ALERT,
    DISMISS_ALERT,
    ENTER_ALERT_TEXT,
    OPEN_NEW_WINDOW,
    SWITCH_WINDOW,
    UPLOAD_FILE,
    HOVER,
    SET_SLIDER;

    public static java.util.Optional<BrowserCapabilityAction> fromIntent(String intent) {
        String normalized = intent == null ? "" : intent.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "HTTP_AUTHENTICATE", "HTTP_AUTH", "BASIC_AUTH" -> java.util.Optional.of(HTTP_AUTHENTICATE);
            case "ACCEPT_ALERT" -> java.util.Optional.of(ACCEPT_ALERT);
            case "DISMISS_ALERT" -> java.util.Optional.of(DISMISS_ALERT);
            case "ENTER_ALERT_TEXT" -> java.util.Optional.of(ENTER_ALERT_TEXT);
            case "OPEN_NEW_WINDOW", "OPEN_WINDOW" -> java.util.Optional.of(OPEN_NEW_WINDOW);
            case "SWITCH_WINDOW" -> java.util.Optional.of(SWITCH_WINDOW);
            case "UPLOAD", "UPLOAD_FILE" -> java.util.Optional.of(UPLOAD_FILE);
            case "HOVER" -> java.util.Optional.of(HOVER);
            case "SET_SLIDER", "SLIDER" -> java.util.Optional.of(SET_SLIDER);
            default -> java.util.Optional.empty();
        };
    }
}
