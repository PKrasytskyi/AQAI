package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InteractionEvidenceExtractor {

    private static final Pattern ROUTE_PATTERN = Pattern.compile("/[a-zA-Z0-9/_\\-]+");

    public CanonicalInteractionEvidence extract(
            MappedPage page,
            MappedAction action,
            MappedElement sourceElement
    ) {
        List<String> locatorValues = sourceElement == null
                ? List.of()
                : sourceElement.locatorCandidates().stream()
                .map(LocatorCandidate::value)
                .filter(value -> value != null && !value.isBlank())
                .toList();

        Set<String> extractedRoutes = new LinkedHashSet<>();
        extractRoutes(extractedRoutes, page.url());
        extractRoutes(extractedRoutes, page.urlPattern());
        extractRoutes(extractedRoutes, page.title());
        extractRoutes(extractedRoutes, action.actionName());
        extractRoutes(extractedRoutes, action.description());
        for (String locator : locatorValues) {
            extractRoutes(extractedRoutes, locator);
        }
        if (sourceElement != null) {
            extractRoutes(extractedRoutes, sourceElement.text());
            extractRoutes(extractedRoutes, sourceElement.semanticName());
        }

        Set<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, page.pageName());
        addTokens(tokens, page.pageType());
        addTokens(tokens, page.url());
        addTokens(tokens, page.urlPattern());
        addTokens(tokens, page.title());
        addTokens(tokens, action.actionName());
        addTokens(tokens, action.actionType());
        addTokens(tokens, action.description());
        addTokens(tokens, action.targetPageId());
        for (String route : extractedRoutes) {
            addTokens(tokens, route);
        }
        if (sourceElement != null) {
            addTokens(tokens, sourceElement.semanticName());
            addTokens(tokens, sourceElement.elementType());
            addTokens(tokens, sourceElement.role());
            addTokens(tokens, sourceElement.text());
            sourceElement.supportedActions().forEach(value -> addTokens(tokens, value));
        }
        locatorValues.forEach(value -> addTokens(tokens, value));

        return new CanonicalInteractionEvidence(
                page.pageId(),
                page.pageName(),
                page.pageType(),
                page.url(),
                page.urlPattern(),
                page.title(),
                action.actionId(),
                action.actionName(),
                action.actionType(),
                action.description(),
                sourceElement == null ? action.sourceElementId() : sourceElement.elementId(),
                sourceElement == null ? "" : sourceElement.semanticName(),
                sourceElement == null ? "" : sourceElement.elementType(),
                sourceElement == null ? "" : sourceElement.role(),
                sourceElement == null ? "" : sourceElement.text(),
                action.targetPageId(),
                sourceElement == null ? List.of() : sourceElement.supportedActions(),
                locatorValues,
                List.copyOf(extractedRoutes),
                List.copyOf(tokens)
        );
    }

    private void extractRoutes(Set<String> routes, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (looksLikeDirectRoute(text)) {
            String canonical = RouteCanonicalizer.canonicalize(text);
            if (validExtractedRoute(canonical)) {
                routes.add(canonical);
            }
        }
        Matcher matcher = ROUTE_PATTERN.matcher(text);
        while (matcher.find()) {
            String route = RouteCanonicalizer.canonicalize(matcher.group());
            if (validExtractedRoute(route)) {
                routes.add(route);
            }
        }
    }

    private boolean validExtractedRoute(String route) {
        if (route == null || route.isBlank() || "/".equals(route)) {
            return false;
        }
        String normalized = route.toLowerCase(Locale.ROOT);
        return !normalized.contains(" ")
                && !normalized.startsWith("//")
                && !normalized.contains(".com")
                && !normalized.contains(".org")
                && !normalized.contains(".net");
    }

    private boolean looksLikeDirectRoute(String text) {
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("/");
    }

    private void addTokens(Set<String> tokens, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_\\- ]", " ").split("\\s+")) {
            if (!token.isBlank() && (token.length() >= 3 || token.startsWith("/"))) {
                tokens.add(token);
            }
        }
    }
}
