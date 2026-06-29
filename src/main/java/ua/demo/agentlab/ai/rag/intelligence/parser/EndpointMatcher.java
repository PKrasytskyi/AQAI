package ua.demo.agentlab.ai.rag.intelligence.parser;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.EndpointMatch;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiEndpointDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EndpointMatcher {

    public List<EndpointMatch> match(
            List<ControllerRouteDefinition> controllerRoutes,
            List<OpenApiSpecification> specifications
    ) {
        List<EndpointMatch> matches = new ArrayList<>();
        List<OpenApiEndpointDefinition> endpoints = specifications.stream()
                .flatMap(spec -> spec.endpoints().stream())
                .toList();
        for (ControllerRouteDefinition route : controllerRoutes) {
            MatchCandidate candidate = endpoints.stream()
                    .map(endpoint -> score(route, endpoint))
                    .filter(MatchCandidate::matched)
                    .max(Comparator.comparingDouble(MatchCandidate::score))
                    .orElse(null);
            matches.add(candidate == null
                    ? new EndpointMatch(route, null, false, 0.0d, "UNMATCHED")
                    : new EndpointMatch(route, candidate.endpoint(), true, candidate.score(), candidate.strategy()));
        }
        return matches;
    }

    private MatchCandidate score(ControllerRouteDefinition route, OpenApiEndpointDefinition endpoint) {
        String normalizedRoutePath = normalize(route.fullPath());
        String normalizedEndpointPath = normalize(endpoint.path());
        String routeMethod = route.httpMethod().toUpperCase(Locale.ROOT);
        String endpointMethod = endpoint.httpMethod().toUpperCase(Locale.ROOT);

        if (!routeMethod.equals(endpointMethod)) {
            return MatchCandidate.unmatched(endpoint);
        }
        if (normalizedEndpointPath.equals(normalizedRoutePath)) {
            return new MatchCandidate(endpoint, 1.0d, "EXACT_PATH");
        }
        if (normalizeStructural(endpoint.path()).equals(normalizeStructural(route.fullPath()))) {
            return new MatchCandidate(endpoint, 0.94d, "PATH_VARIABLE_NORMALIZED");
        }
        if (!endpoint.operationId().isBlank()) {
            String operationId = endpoint.operationId().toLowerCase(Locale.ROOT);
            String routeSignature = (route.className() + "." + route.methodName()).toLowerCase(Locale.ROOT);
            if (operationId.contains(route.methodName().toLowerCase(Locale.ROOT))
                    || routeSignature.contains(operationId)
                    || operationId.contains(route.className().toLowerCase(Locale.ROOT).replace("controller", ""))) {
                return new MatchCandidate(endpoint, 0.83d, "OPERATION_ID");
            }
        }
        if (!endpoint.summary().isBlank()) {
            String summary = endpoint.summary().toLowerCase(Locale.ROOT);
            if (summary.contains(route.methodName().toLowerCase(Locale.ROOT))
                    || summary.contains(lastPathToken(route.fullPath()))) {
                return new MatchCandidate(endpoint, 0.72d, "SUMMARY_HINT");
            }
        }
        if (lastPathToken(endpoint.path()).equals(lastPathToken(route.fullPath()))) {
            return new MatchCandidate(endpoint, 0.66d, "PATH_SUFFIX");
        }
        return MatchCandidate.unmatched(endpoint);
    }

    private String normalize(String path) {
        return path == null ? "" : path.replaceAll("\\{[^}]+}", "{}").replaceAll("//+", "/");
    }

    private String normalizeStructural(String path) {
        return normalize(path).replaceAll("/[0-9]+", "/{}");
    }

    private String lastPathToken(String path) {
        String normalized = normalize(path);
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1).toLowerCase(Locale.ROOT) : normalized.toLowerCase(Locale.ROOT);
    }

    private record MatchCandidate(OpenApiEndpointDefinition endpoint, double score, String strategy) {
        private boolean matched() {
            return score > 0.0d;
        }

        private static MatchCandidate unmatched(OpenApiEndpointDefinition endpoint) {
            return new MatchCandidate(endpoint, 0.0d, "UNMATCHED");
        }
    }
}
