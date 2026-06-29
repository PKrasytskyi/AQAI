package ua.demo.agentlab.ui.catalog;

import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ConfirmedPageRegistry {

    private final List<ConfirmedPageCandidate> pages;

    public ConfirmedPageRegistry(List<ConfirmedPageCandidate> pages) {
        this.pages = pages == null
                ? List.of()
                : pages.stream()
                .filter(candidate -> candidate != null && candidate.hasRoute())
                .collect(java.util.stream.Collectors.toMap(
                        candidate -> normalizeRoute(candidate.route()),
                        candidate -> candidate,
                        this::higherConfidenceOrPriority,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    public List<ConfirmedPageCandidate> allPages() {
        return pages;
    }

    public Optional<ConfirmedPageCandidate> findByPageName(String pageName) {
        String normalized = normalize(pageName);
        return pages.stream()
                .filter(page -> normalize(page.pageName()).equals(normalized))
                .max(Comparator.comparingDouble(ConfirmedPageCandidate::confidence));
    }

    public Optional<ConfirmedPageCandidate> findByRoute(String route) {
        String normalized = normalizeRoute(route);
        return pages.stream()
                .filter(page -> normalizeRoute(page.route()).equals(normalized))
                .max(Comparator.comparingDouble(ConfirmedPageCandidate::confidence));
    }

    public Optional<ConfirmedPageCandidate> findByCapability(PageCapability capability) {
        if (capability == null) {
            return Optional.empty();
        }
        return pages.stream()
                .filter(page -> page.capability() == capability)
                .max(Comparator.comparingDouble(ConfirmedPageCandidate::confidence));
    }

    private ConfirmedPageCandidate higherConfidenceOrPriority(ConfirmedPageCandidate left, ConfirmedPageCandidate right) {
        int leftPriority = priority(left);
        int rightPriority = priority(right);
        if (rightPriority != leftPriority) {
            return rightPriority > leftPriority ? right : left;
        }
        return right.confidence() > left.confidence() ? right : left;
    }

    private int priority(ConfirmedPageCandidate candidate) {
        if (candidate == null || candidate.capability() == null) {
            return 0;
        }
        return switch (candidate.capability()) {
            case AUTHENTICATION -> 100;
            case DASHBOARD -> 95;
            case AUTHENTICATED_AREA -> 90;
            case SECURITY -> 80;
            case REGISTRATION -> 70;
            case RECOVERY -> 68;
            case FORM -> 60;
            case RECORD_LIST -> 55;
            case RECORD_DETAILS -> 50;
            case CONTAINER -> 45;
            case NAVIGATION -> 30;
            case GENERIC -> 10;
        };
    }

    private String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
