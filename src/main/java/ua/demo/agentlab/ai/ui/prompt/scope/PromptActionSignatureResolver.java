package ua.demo.agentlab.ai.ui.prompt.scope;

import ua.demo.agentlab.ui.discovery.catalog.ConfirmedCatalogAction;

import java.util.Locale;

/** Converts a confirmed semantic action into one exact public POM API signature. */
public final class PromptActionSignatureResolver {

    public String resolve(ConfirmedCatalogAction action) {
        if (action == null || action.intent().isBlank()) {
            return "";
        }
        String target = lowerCamel(action.targetElementId());
        String subject = upperCamel(stripControlSuffix(target));
        String parameter = target.isBlank() ? "value" : target;
        return switch (normalize(action.intent())) {
            case "TYPE", "ENTER_TEXT" -> "enter" + upperCamel(target) + "(String " + parameter + ")";
            case "SUBMIT_FORM" -> isLoginTarget(target) ? "clickLoginButton()" : "submit" + subject + "()";
            case "CLICK" -> "click" + upperCamel(target) + "()";
            case "OPEN_MENU" -> "open" + subject + "()";
            case "LOGOUT" -> "logout()";
            case "SEARCH" -> "search(String query)";
            case "FILTER" -> "applyFilter(String value)";
            case "SELECT", "SELECT_OPTION" -> "select" + subject + "(String value)";
            case "UPLOAD", "UPLOAD_FILE" -> "upload" + subject + "(String filePath)";
            case "DOWNLOAD", "DOWNLOAD_FILE" -> "download" + subject + "()";
            case "NAVIGATE", "MODULE_NAVIGATION", "OPEN_RECORD" -> "open" + subject + "()";
            case "OPEN_MODAL" -> "open" + subject + "()";
            case "CONFIRM_ACTION" -> "confirm" + subject + "()";
            default -> lowerCamel(action.intent()) + subject + "()";
        };
    }

    private boolean isLoginTarget(String target) {
        return target.equals("login") || target.equals("loginButton") || target.equals("submitLogin");
    }

    private String stripControlSuffix(String value) {
        return value.replaceFirst("(?i)(Trigger|Control|Button|Link|Input)$", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String lowerCamel(String value) {
        String pascal = upperCamel(value);
        return pascal.isBlank() ? "" : Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    private String upperCamel(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String token : value.trim().split("[^A-Za-z0-9]+")) {
            if (token.isBlank()) continue;
            result.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) result.append(token.substring(1));
        }
        return result.toString();
    }
}
