package ua.demo.agentlab.ui.discovery.spa.binding;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.BehaviorTargetContext;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Resolves one requirement target from explicit current-run page evidence. */
public final class BehaviorTargetStateResolver {

    private final BehaviorSourceStateResolver sourceResolver;

    public BehaviorTargetStateResolver(BehaviorSourceStateResolver sourceResolver) {
        this.sourceResolver = sourceResolver;
    }

    public Optional<SpaPageInventory> resolve(StructuredBehaviorContract contract,
                                              List<SpaPageInventory> pages,
                                              SourceStateBinding sourceBinding) {
        if (contract == null || pages == null || pages.isEmpty()) return Optional.empty();
        Optional<SpaPageInventory> confirmedSource = sourceResolver.resolve(pages, sourceBinding);
        if (confirmedSource.isPresent()) return confirmedSource;

        BehaviorTargetContext context = BehaviorTargetContext.parse(contract.targetContext());
        boolean moduleNavigation = normalize(contract.capability()).equals("modulenavigation");
        String route = explicitRoute(context.value(moduleNavigation ? "sourceRoute" : "targetRoute"));
        String pageName = semanticTarget(context.value(moduleNavigation ? "sourcePage" : "targetPage"));
        String capability = moduleNavigation ? "" : normalize(context.value("pageCapability"));

        if (!route.isBlank()) {
            return pages.stream().filter(page -> routeMatches(page.route(), route))
                    .filter(page -> capability.isBlank() || supports(page, capability))
                    .sorted(Comparator.comparing(SpaPageInventory::route)).findFirst();
        }
        if (!pageName.isBlank()) {
            return pages.stream().filter(page -> pageMatches(page, pageName))
                    .filter(page -> capability.isBlank() || supports(page, capability))
                    .sorted(Comparator.comparing(SpaPageInventory::route)).findFirst();
        }
        List<SpaPageInventory> matches = pages.stream()
                .filter(page -> !capability.isBlank() && supports(page, capability))
                .sorted(Comparator.comparing(SpaPageInventory::route)).toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    private boolean supports(SpaPageInventory page, String capability) {
        return java.util.Arrays.stream(page.capability().split("\\|"))
                .map(this::normalize).anyMatch(capability::equals);
    }

    private boolean pageMatches(SpaPageInventory page, String expected) {
        String target = normalize(expected);
        String evidence = normalize(page.pageName() + " " + page.pageId() + " " + page.route());
        return !target.isBlank() && (evidence.contains(target) || target.contains(normalize(page.pageName())));
    }

    private boolean routeMatches(String actual, String expected) {
        String left = normalize(actual);
        String right = normalize(expected);
        return !left.isBlank() && !right.isBlank()
                && (left.equals(right) || left.endsWith(right) || right.endsWith(left));
    }

    private String explicitRoute(String value) {
        String route = value == null ? "" : value.trim();
        return route.startsWith("/") ? route : "";
    }

    private String semanticTarget(String value) {
        return value == null ? "" : value
                .replaceAll("(?i)\\b(discovery|confirmed|page|route|target)\\b", " ")
                .replaceAll("[^A-Za-z0-9]+", " ").trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
