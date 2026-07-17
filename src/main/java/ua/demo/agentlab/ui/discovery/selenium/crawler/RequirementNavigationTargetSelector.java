package ua.demo.agentlab.ui.discovery.selenium.crawler;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Selects authenticated navigation links using requirement vocabulary, not product-specific routes. */
final class RequirementNavigationTargetSelector {

    private static final Pattern MODULE_TERM_PATTERN = Pattern.compile(
            "\\b([a-z0-9][a-z0-9 _-]{0,64}?)\\s+module\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> IGNORED_TOKENS = Set.of(
            "user", "users", "system", "with", "from", "that", "this", "page", "area", "application",
            "visible", "enabled", "selected", "displayed", "matching", "criteria", "successfully", "authenticated",
            "requirements", "functional", "assertion", "after", "before", "when", "then", "click", "open"
    );

    List<DiscoveredInteractiveElement> select(
            List<DiscoveredInteractiveElement> links,
            NormalizedRequirementBundle requirements
    ) {
        if (links == null || links.isEmpty() || requirements == null || requirements.requirements() == null) {
            return List.of();
        }
        NavigationVocabulary vocabulary = vocabulary(requirements);
        if (vocabulary.terms().isEmpty()) {
            return List.of();
        }
        return links.stream()
                .filter(link -> link != null && link.enabled()
                        && link.href() != null && !link.href().isBlank())
                .map(link -> new RankedLink(link, score(link, vocabulary)))
                .filter(ranked -> ranked.score() > 0)
                .sorted(Comparator.comparingInt(RankedLink::score).reversed()
                        .thenComparing(ranked -> safe(ranked.link().visibleText())))
                .map(RankedLink::link)
                .distinct()
                .limit(4)
                .toList();
    }

    private NavigationVocabulary vocabulary(NormalizedRequirementBundle requirements) {
        Set<String> terms = new LinkedHashSet<>();
        Set<String> moduleTerms = new LinkedHashSet<>();
        for (NormalizedRequirement requirement : requirements.requirements()) {
            if (requirement == null) {
                continue;
            }
            String text = requirement.title() + " " + requirement.statement();
            tokenize(text).stream()
                    .filter(token -> token.length() >= 4)
                    .filter(token -> !IGNORED_TOKENS.contains(token))
                    .forEach(terms::add);
            extractModuleTerms(text).forEach(moduleTerms::add);
        }
        return new NavigationVocabulary(Set.copyOf(terms), Set.copyOf(moduleTerms));
    }

    private Set<String> extractModuleTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = MODULE_TERM_PATTERN.matcher(safe(text).toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String candidate = matcher.group(1);
            List<String> tokens = tokenize(candidate).stream()
                    .filter(token -> token.length() >= 3)
                    .filter(token -> !IGNORED_TOKENS.contains(token))
                    .toList();
            if (!tokens.isEmpty()) {
                // The noun immediately before "module" is the route-root signal, e.g. Recruitment module.
                terms.add(tokens.get(tokens.size() - 1));
            }
        }
        return terms;
    }

    private int score(DiscoveredInteractiveElement link, NavigationVocabulary vocabulary) {
        String evidence = safe(link.visibleText()) + " " + safe(link.name()) + " " + safe(link.id()) + " " + safe(link.href());
        Set<String> linkTerms = tokenize(evidence);
        int score = (int) vocabulary.terms().stream().filter(linkTerms::contains).count();
        String visible = safe(link.visibleText()).toLowerCase(Locale.ROOT);
        if (!visible.isBlank() && vocabulary.terms().contains(visible)) {
            score += 2;
        }
        if (vocabulary.moduleTerms().contains(visible)) {
            score += 100;
        } else if (vocabulary.moduleTerms().stream().anyMatch(term -> visible.contains(term))) {
            score += 50;
        }
        return score;
    }

    private Set<String> tokenize(String value) {
        return java.util.Arrays.stream(safe(value).toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", " ").split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record RankedLink(DiscoveredInteractiveElement link, int score) {
    }

    private record NavigationVocabulary(Set<String> terms, Set<String> moduleTerms) {
    }
}
