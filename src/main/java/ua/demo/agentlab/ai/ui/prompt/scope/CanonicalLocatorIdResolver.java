package ua.demo.agentlab.ai.ui.prompt.scope;

import ua.demo.agentlab.ai.context.PromptLocatorEvidence;

import java.util.Locale;

public class CanonicalLocatorIdResolver {

    public String resolve(PromptLocatorEvidence locator) {
        if (locator == null) {
            return "element";
        }
        String evidence = normalize(String.join(" ",
                locator.fieldHint(),
                locator.elementName(),
                locator.role(),
                locator.visibleText(),
                locator.value()
        ));
        if (containsAny(evidence, "logout", "log out", "signout", "sign out")) {
            return "logoutLink";
        }
        if (containsAny(evidence, "username", "user name", "userid", "user-id", "email")) {
            return "usernameInput";
        }
        if (containsAny(evidence, "password", "pass")) {
            return "passwordInput";
        }
        if (containsAny(evidence, "login", "log in", "signin", "sign in")) {
            return "loginButton";
        }
        if (containsAny(evidence, "submit")) {
            return "submitButton";
        }
        String role = normalize(locator.role());
        String base = firstNonBlank(locator.fieldHint(), locator.elementName(), semanticNameFromValue(locator.value()));
        String field = camel(base);
        if (field.isBlank()) {
            field = "element";
        }
        if (role.contains("button") && !field.endsWith("Button")) {
            return field + "Button";
        }
        if (role.contains("link") && !field.endsWith("Link")) {
            return field + "Link";
        }
        if ((role.contains("input") || role.contains("password")) && !field.endsWith("Input")) {
            return field + "Input";
        }
        return field;
    }

    private String semanticNameFromValue(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.contains("=")) {
            normalized = normalized.substring(normalized.lastIndexOf('=') + 1);
        }
        return normalized.replaceAll("[^A-Za-z0-9]+", " ").trim();
    }

    private String camel(String value) {
        String normalized = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase(Locale.ROOT)).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
