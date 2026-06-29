package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.EndpointMatch;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.InitialTestIdea;
import ua.demo.agentlab.ai.rag.intelligence.model.InitialTestIdeaType;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiEndpointDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InitialTestIdeaGenerator {

    public List<InitialTestIdea> generate(
            List<ControllerRouteDefinition> routes,
            List<EndpointMatch> endpointMatches,
            List<ExistingTestDefinition> existingTests,
            List<OpenApiSpecification> openApiSpecifications
    ) {
        List<InitialTestIdea> ideas = new ArrayList<>();
        int sequence = 1;
        for (ControllerRouteDefinition route : routes) {
            ideas.add(new InitialTestIdea(
                    "TI-%03d".formatted(sequence++),
                    InitialTestIdeaType.API_POSITIVE,
                    "Verify " + route.httpMethod() + " " + route.fullPath() + " returns expected success response",
                    "Controller route discovered in " + route.className() + "." + route.methodName(),
                    List.of(route.relativePath())
            ));
            if (route.httpMethod().equalsIgnoreCase("POST") || route.httpMethod().equalsIgnoreCase("PUT")) {
                ideas.add(new InitialTestIdea(
                        "TI-%03d".formatted(sequence++),
                        InitialTestIdeaType.API_NEGATIVE,
                        "Verify invalid payload handling for " + route.httpMethod() + " " + route.fullPath(),
                        "Mutable endpoint detected; negative validation scenario is likely required",
                        List.of(route.relativePath())
                ));
            }
            if (isFailureSensitive(route)) {
                ideas.add(new InitialTestIdea(
                        "TI-%03d".formatted(sequence++),
                        InitialTestIdeaType.API_FAILURE_MODE,
                        "Verify failure-mode resilience for " + route.httpMethod() + " " + route.fullPath(),
                        "Endpoint keywords suggest elevated failure, timeout, or authorization risk.",
                        List.of(route.relativePath())
                ));
            }
            if (isUiLikeRoute(route)) {
                ideas.add(new InitialTestIdea(
                        "TI-%03d".formatted(sequence++),
                        InitialTestIdeaType.UI_FLOW,
                        "Verify UI navigation flow for " + route.fullPath(),
                        "GET route naming suggests a user-facing page or discovery flow.",
                        List.of(route.relativePath())
                ));
            }
        }
        for (EndpointMatch endpointMatch : endpointMatches) {
            if (endpointMatch.matched()) {
                ideas.add(new InitialTestIdea(
                        "TI-%03d".formatted(sequence++),
                        InitialTestIdeaType.API_CONTRACT,
                        "Verify contract alignment for " + endpointMatch.controllerRoute().httpMethod()
                                + " " + endpointMatch.controllerRoute().fullPath(),
                        "Swagger/OpenAPI endpoint matches controller route via " + endpointMatch.matchStrategy()
                                + " with confidence " + endpointMatch.confidenceScore(),
                        List.of(
                                endpointMatch.controllerRoute().relativePath(),
                                endpointMatch.openApiEndpoint().sourcePath()
                        )
                ));
            } else {
                ideas.add(new InitialTestIdea(
                        "TI-%03d".formatted(sequence++),
                        InitialTestIdeaType.BUG_RISK,
                        "Investigate undocumented controller route " + endpointMatch.controllerRoute().httpMethod()
                                + " " + endpointMatch.controllerRoute().fullPath(),
                        "Controller route did not align with any OpenAPI endpoint and may represent contract drift.",
                        List.of(endpointMatch.controllerRoute().relativePath())
                ));
            }
        }
        boolean hasAuthTests = existingTests.stream()
                .anyMatch(test -> test.className().toLowerCase(Locale.ROOT).contains("login")
                        || test.className().toLowerCase(Locale.ROOT).contains("auth"));
        if (!hasAuthTests && routes.stream().anyMatch(route -> route.fullPath().toLowerCase(Locale.ROOT).contains("auth")
                || route.fullPath().toLowerCase(Locale.ROOT).contains("login"))) {
            ideas.add(new InitialTestIdea(
                    "TI-%03d".formatted(sequence),
                    InitialTestIdeaType.REGRESSION_GAP,
                    "Add regression tests for authentication-related endpoints",
                    "Authentication-related routes exist but matching tests were not detected",
                    routes.stream()
                            .filter(route -> route.fullPath().toLowerCase(Locale.ROOT).contains("auth")
                                    || route.fullPath().toLowerCase(Locale.ROOT).contains("login"))
                            .map(ControllerRouteDefinition::relativePath)
                            .distinct()
                            .toList()
            ));
        }
        for (OpenApiSpecification openApiSpecification : openApiSpecifications) {
            for (OpenApiEndpointDefinition endpoint : openApiSpecification.endpoints()) {
                boolean matched = endpointMatches.stream()
                        .anyMatch(match -> match.matched() && match.openApiEndpoint() != null
                                && match.openApiEndpoint().path().equals(endpoint.path())
                                && match.openApiEndpoint().httpMethod().equalsIgnoreCase(endpoint.httpMethod()));
                if (!matched) {
                    ideas.add(new InitialTestIdea(
                            "TI-%03d".formatted(sequence++),
                            InitialTestIdeaType.API_CONTRACT,
                            "Investigate unmatched OpenAPI endpoint " + endpoint.httpMethod() + " " + endpoint.path(),
                            "OpenAPI endpoint was parsed but no matching controller route was detected.",
                            List.of(endpoint.sourcePath())
                    ));
                }
            }
        }
        addRegressionGapIdeas(routes, existingTests, ideas, sequence);
        return ideas;
    }

    private int addRegressionGapIdeas(
            List<ControllerRouteDefinition> routes,
            List<ExistingTestDefinition> existingTests,
            List<InitialTestIdea> ideas,
            int startSequence
    ) {
        Set<String> testedKeywords = existingTests.stream()
                .flatMap(test -> test.testMethods().stream().map(String::toLowerCase))
                .collect(java.util.stream.Collectors.toSet());
        int sequence = startSequence;
        for (ControllerRouteDefinition route : routes) {
            String routeToken = lastPathToken(route.fullPath());
            if (routeToken.isBlank() || testedKeywords.stream().anyMatch(keyword -> keyword.contains(routeToken))) {
                continue;
            }
            ideas.add(new InitialTestIdea(
                    "TI-%03d".formatted(sequence++),
                    InitialTestIdeaType.REGRESSION_GAP,
                    "Add missing regression coverage for " + route.httpMethod() + " " + route.fullPath(),
                    "No existing test methods appear to reference this route family.",
                    List.of(route.relativePath())
            ));
        }
        return sequence;
    }

    private boolean isFailureSensitive(ControllerRouteDefinition route) {
        String normalized = route.fullPath().toLowerCase(Locale.ROOT);
        return normalized.contains("auth")
                || normalized.contains("login")
                || normalized.contains("checkout")
                || normalized.contains("payment")
                || normalized.contains("upload")
                || normalized.contains("admin");
    }

    private boolean isUiLikeRoute(ControllerRouteDefinition route) {
        if (!route.httpMethod().equalsIgnoreCase("GET")) {
            return false;
        }
        String normalized = route.fullPath().toLowerCase(Locale.ROOT);
        return normalized.contains("page")
                || normalized.contains("view")
                || normalized.contains("home")
                || normalized.contains("list")
                || normalized.contains("details")
                || normalized.contains("catalog")
                || normalized.contains("collection")
                || normalized.contains("product");
    }

    private String lastPathToken(String path) {
        String normalized = path == null ? "" : path.replaceAll("//+", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1).toLowerCase(Locale.ROOT) : normalized.toLowerCase(Locale.ROOT);
    }
}
