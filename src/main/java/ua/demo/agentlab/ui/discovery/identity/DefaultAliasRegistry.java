package ua.demo.agentlab.ui.discovery.identity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultAliasRegistry implements AliasRegistry {

    @Override
    public List<String> aliasesFor(
            CanonicalPageType canonicalPageType,
            String route,
            String title,
            List<String> headings,
            List<String> capabilities
    ) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(canonicalPageType.defaultAlias());
        addIfPresent(aliases, title);
        if (headings != null) {
            headings.stream().limit(3).forEach(value -> addIfPresent(aliases, value));
        }
        if (capabilities != null) {
            capabilities.forEach(value -> addIfPresent(aliases, value));
        }
        addRouteAliases(aliases, route);

        return aliases.stream()
                .map(this::normalizeAlias)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private void addRouteAliases(Set<String> aliases, String route) {
        String safeRoute = route == null ? "" : route.trim();
        if (safeRoute.isBlank() || "/".equals(safeRoute)) {
            aliases.add("home");
            aliases.add("landing");
            return;
        }
        for (String token : safeRoute.split("/")) {
            addIfPresent(aliases, token);
        }
        String lastToken = PageIdentity.lastRouteToken(safeRoute);
        addIfPresent(aliases, lastToken);
    }

    private void addIfPresent(Set<String> aliases, String value) {
        String normalized = normalizeAlias(value);
        if (!normalized.isBlank()) {
            aliases.add(normalized);
        }
    }

    private String normalizeAlias(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("page ")) {
            normalized = normalized.substring(5).trim();
        }
        if (normalized.endsWith(" page")) {
            normalized = normalized.substring(0, normalized.length() - 5).trim();
        }
        return normalized.replace(' ', '-');
    }
}
