package ua.demo.agentlab.ai.context;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CanonicalAliasDictionary {

    private static final Set<String> COLLECTION_ALIASES = Set.of(
            "list", "listing", "grid", "catalog", "results", "overview", "collection", "feed", "table"
    );
    private static final Set<String> ENTITY_ALIASES = Set.of(
            "entity", "item", "product", "record", "entry", "article", "card", "row", "result"
    );
    private static final Set<String> CONTAINER_ALIASES = Set.of(
            "container", "cart", "basket", "bag", "wishlist", "favorites", "saved", "queue", "selection"
    );
    private static final Set<String> DETAILS_ALIASES = Set.of(
            "details", "detail", "view", "profile", "preview", "expanded"
    );
    private static final Set<String> AUTH_ALIASES = Set.of(
            "login", "signin", "sign", "auth", "authentication", "password", "username", "session"
    );
    private static final Set<String> FORM_ALIASES = Set.of(
            "form", "submit", "save", "create", "update", "edit", "input"
    );
    private static final Set<String> FILE_ALIASES = Set.of(
            "file", "upload", "download", "export", "import", "attach"
    );

    public Set<String> resolveDomainHints(CanonicalInteractionEvidence evidence) {
        Set<String> hints = new LinkedHashSet<>();
        addMatchingHints(hints, evidence.tokens(), COLLECTION_ALIASES, "collection");
        addMatchingHints(hints, evidence.tokens(), ENTITY_ALIASES, "entity");
        addMatchingHints(hints, evidence.tokens(), CONTAINER_ALIASES, "container");
        addMatchingHints(hints, evidence.tokens(), DETAILS_ALIASES, "details");
        addMatchingHints(hints, evidence.tokens(), AUTH_ALIASES, "auth");
        addMatchingHints(hints, evidence.tokens(), FORM_ALIASES, "form");
        addMatchingHints(hints, evidence.tokens(), FILE_ALIASES, "file");
        addRouteHints(hints, evidence.extractedRoutes());
        return hints;
    }

    public boolean matchesCollection(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, COLLECTION_ALIASES);
    }

    public boolean matchesEntity(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, ENTITY_ALIASES);
    }

    public boolean matchesContainer(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, CONTAINER_ALIASES);
    }

    public boolean matchesDetails(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, DETAILS_ALIASES) || evidence.extractedRoutes().stream()
                .map(this::normalize)
                .anyMatch(route -> route.contains("/details") || route.contains("/item") || route.contains("/product") || route.contains("/record"));
    }

    public boolean matchesAuth(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, AUTH_ALIASES);
    }

    public boolean matchesForm(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, FORM_ALIASES);
    }

    public boolean matchesFile(CanonicalInteractionEvidence evidence) {
        return matchesAny(evidence, FILE_ALIASES);
    }

    public List<String> normalizedTokens(CanonicalInteractionEvidence evidence) {
        List<String> tokens = new ArrayList<>();
        for (String token : evidence.tokens()) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String normalized = normalizeToken(token);
            if (!normalized.isBlank()) {
                tokens.add(normalized);
            }
        }
        return List.copyOf(tokens);
    }

    public String normalizeToken(String token) {
        String value = normalize(token);
        if (CONTAINER_ALIASES.contains(value)) {
            return "container";
        }
        if (ENTITY_ALIASES.contains(value)) {
            return "entity";
        }
        if (COLLECTION_ALIASES.contains(value)) {
            return "collection";
        }
        if (DETAILS_ALIASES.contains(value)) {
            return "details";
        }
        if (AUTH_ALIASES.contains(value)) {
            return "auth";
        }
        if (FORM_ALIASES.contains(value)) {
            return "form";
        }
        if (FILE_ALIASES.contains(value)) {
            return "file";
        }
        return value;
    }

    private boolean matchesAny(CanonicalInteractionEvidence evidence, Set<String> aliases) {
        for (String token : evidence.tokens()) {
            if (aliases.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private void addMatchingHints(Set<String> hints, List<String> tokens, Set<String> aliases, String hint) {
        for (String token : tokens) {
            if (aliases.contains(normalize(token))) {
                hints.add(hint);
                return;
            }
        }
    }

    private void addRouteHints(Set<String> hints, List<String> routes) {
        for (String route : routes) {
            String normalized = normalize(route);
            if (normalized.contains("/cart") || normalized.contains("/basket") || normalized.contains("/wishlist")) {
                hints.add("container");
            }
            if (normalized.contains("/details") || normalized.contains("/product") || normalized.contains("/item")) {
                hints.add("details");
                hints.add("entity");
            }
            if (normalized.contains("/search") || normalized.contains("/results") || normalized.contains("/catalog")) {
                hints.add("collection");
            }
            if (normalized.contains("/login") || normalized.contains("/signin") || normalized.contains("/auth")) {
                hints.add("auth");
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
