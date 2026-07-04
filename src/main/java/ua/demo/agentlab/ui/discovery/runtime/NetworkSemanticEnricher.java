package ua.demo.agentlab.ui.discovery.runtime;

import ua.demo.agentlab.ui.discovery.runtime.model.NetworkResponseEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.SemanticNetworkEvidence;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkSemanticEnricher {

    public List<SemanticNetworkEvidence> enrich(List<NetworkResponseEvent> responses) {
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }
        AtomicInteger counter = new AtomicInteger(1);
        return responses.stream()
                .filter(this::isRelevant)
                .map(response -> toEvidence(response, counter.getAndIncrement()))
                .toList();
    }

    private boolean isRelevant(NetworkResponseEvent response) {
        if (response == null || response.url().isBlank()) {
            return false;
        }
        String resourceType = response.resourceType().toLowerCase(Locale.ROOT);
        if (resourceType.contains("image") || resourceType.contains("font") || resourceType.contains("stylesheet")) {
            return false;
        }
        String path = response.path().toLowerCase(Locale.ROOT);
        return path.contains("/api")
                || path.contains("/auth")
                || path.contains("/login")
                || path.contains("/session")
                || path.contains("/token")
                || path.contains("/user")
                || path.contains("/employee")
                || path.contains("/dashboard")
                || path.contains("/pim");
    }

    private SemanticNetworkEvidence toEvidence(NetworkResponseEvent response, int index) {
        String operation = operation(response.method(), response.path());
        String intent = businessIntent(operation, response.path());
        return new SemanticNetworkEvidence(
                "network-evidence-" + index,
                response.pageId(),
                response.pageUrl(),
                response.method(),
                response.path().isBlank() ? response.url() : response.path(),
                response.status(),
                response.resourceType(),
                operation,
                intent,
                confidence(response, operation),
                "runtime-network:" + response.eventId()
        );
    }

    private String operation(String method, String path) {
        String normalizedMethod = method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
        String normalizedPath = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);
        if (normalizedMethod.equals("POST") && containsAny(normalizedPath, "login", "auth", "session", "token")) {
            return "AUTHENTICATE";
        }
        if (normalizedMethod.equals("GET") && containsAny(normalizedPath, "list", "users", "employees", "pim", "dashboard")) {
            return "LOAD_DATA";
        }
        if (normalizedMethod.equals("POST")) {
            return "CREATE_DATA";
        }
        if (normalizedMethod.equals("PUT") || normalizedMethod.equals("PATCH")) {
            return "UPDATE_DATA";
        }
        if (normalizedMethod.equals("DELETE")) {
            return "DELETE_DATA";
        }
        return normalizedMethod.isBlank() ? "OBSERVE_NETWORK" : normalizedMethod + "_RESOURCE";
    }

    private String businessIntent(String operation, String path) {
        String normalizedPath = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);
        if ("AUTHENTICATE".equals(operation)) {
            return "authentication";
        }
        if (containsAny(normalizedPath, "employee", "pim", "users")) {
            return "record-management";
        }
        if (containsAny(normalizedPath, "dashboard")) {
            return "dashboard-data";
        }
        if (containsAny(normalizedPath, "session", "token", "auth")) {
            return "security-session";
        }
        return "application-data";
    }

    private double confidence(NetworkResponseEvent response, String operation) {
        double score = 0.55d;
        if (response.status() >= 200 && response.status() < 400) {
            score += 0.20d;
        }
        if (!"OBSERVE_NETWORK".equals(operation) && !operation.endsWith("_RESOURCE")) {
            score += 0.20d;
        }
        if (!response.pageId().isBlank()) {
            score += 0.05d;
        }
        return Math.min(1.0d, score);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
