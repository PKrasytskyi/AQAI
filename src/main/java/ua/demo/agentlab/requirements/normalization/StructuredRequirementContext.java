package ua.demo.agentlab.requirements.normalization;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads typed values from normalized capability-first requirement sections. */
public final class StructuredRequirementContext {

    private static final String FIELD_BOUNDARY =
            "pageCapability|componentCapability|sourceRoute|targetRoute|sourcePage|targetPage|logoutAccessMode";

    private StructuredRequirementContext() {
    }

    public static String targetRoute(NormalizedRequirement requirement) {
        return explicitRoute(value(requirement, "target context", "targetRoute"));
    }

    public static String sourceRoute(NormalizedRequirement requirement) {
        return explicitRoute(value(requirement, "target context", "sourceRoute"));
    }

    public static String targetPage(NormalizedRequirement requirement) {
        return value(requirement, "target context", "targetPage");
    }

    public static String sourcePage(NormalizedRequirement requirement) {
        return value(requirement, "target context", "sourcePage");
    }

    public static String pageCapability(NormalizedRequirement requirement) {
        return value(requirement, "target context", "pageCapability");
    }

    public static String componentCapability(NormalizedRequirement requirement) {
        return value(requirement, "target context", "componentCapability");
    }

    public static String logoutAccessMode(NormalizedRequirement requirement) {
        return value(requirement, "target context", "logoutAccessMode");
    }

    public static String value(NormalizedRequirement requirement, String section, String key) {
        if (requirement == null || requirement.structuredSections() == null || key == null || key.isBlank()) {
            return "";
        }
        List<String> values = requirement.structuredSections().getOrDefault(normalize(section), List.of());
        String text = String.join(" ", values).replace('`', ' ').trim();
        if (text.isBlank()) {
            return "";
        }
        Pattern pattern = Pattern.compile("(?i)(?:^|\\s)" + Pattern.quote(key)
                + "\\s*:\\s*(.+?)(?=\\s+(?:" + FIELD_BOUNDARY + ")\\s*:|$)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? clean(matcher.group(1)) : "";
    }

    public static String explicitRoute(String value) {
        String cleaned = clean(value);
        if (cleaned.equals("/")) {
            return cleaned;
        }
        if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            try {
                String path = java.net.URI.create(cleaned).getPath();
                return path == null || path.isBlank() ? "/" : path;
            } catch (IllegalArgumentException ignored) {
                return "";
            }
        }
        Matcher matcher = Pattern.compile("^(/[A-Za-z0-9._~!$&'()*+,;=:@%/-]+)$").matcher(cleaned);
        return matcher.matches() ? matcher.group(1) : "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("^[*\\-\\s]+|[.;,\\s]+$", "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
