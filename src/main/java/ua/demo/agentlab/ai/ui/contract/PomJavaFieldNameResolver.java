package ua.demo.agentlab.ai.ui.contract;

import java.util.Locale;
import java.util.Set;

/** Resolves stable Java field names without changing evidence locator IDs. */
public final class PomJavaFieldNameResolver {

    private static final Set<String> SEMANTIC_SUFFIXES = Set.of(
            "input", "field", "button", "link", "trigger", "control", "select", "dropdown",
            "checkbox", "radio", "menu", "heading", "title", "root", "container", "modal",
            "table", "list", "message"
    );

    public String resolve(PomLocatorSpec locator) {
        if (locator == null) return "locator";
        String base = javaName(locator.id().isBlank() ? locator.elementName() : locator.id());
        String normalizedBase = base.toLowerCase(Locale.ROOT);
        if (SEMANTIC_SUFFIXES.stream().anyMatch(normalizedBase::endsWith)) return base;
        String role = normalize(locator.role() + " " + locator.elementName());
        if (containsAny(role, "password", "input", "textbox", "email", "search")) return base + "Input";
        if (containsAny(role, "button", "submit")) return base + "Button";
        if (containsAny(role, "link", "anchor")) return base + "Link";
        if (containsAny(role, "heading", "title")) return base + "Heading";
        return base;
    }

    private String javaName(String value) {
        String[] tokens = (value == null ? "" : value).split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            String normalized = allUpperCase(token) ? token.toLowerCase(Locale.ROOT) : token;
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            if (normalized.length() > 1) builder.append(normalized.substring(1));
        }
        if (builder.isEmpty()) return "locator";
        return Character.toLowerCase(builder.charAt(0)) + builder.substring(1);
    }

    private boolean allUpperCase(String value) {
        return value.chars().anyMatch(Character::isLetter)
                && value.equals(value.toUpperCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }
}
