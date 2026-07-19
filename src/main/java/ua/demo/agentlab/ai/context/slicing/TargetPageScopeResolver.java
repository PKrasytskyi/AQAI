package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.ui.catalog.*;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.List;

/** Resolves the only page/route authority used by downstream context slicers. */
public final class TargetPageScopeResolver {

    private final ConfirmedPageSourceResolver sourceResolver;

    public TargetPageScopeResolver() {
        this(new ConfirmedPageSourceResolver());
    }

    public TargetPageScopeResolver(ConfirmedPageSourceResolver sourceResolver) {
        this.sourceResolver = sourceResolver;
    }

    public TargetPageScope resolve(AiContextPackage context, AiContextScope requested) {
        List<ConfirmedPageCandidate> mapped = context.mappedUiKnowledge() == null ? List.of()
                : context.mappedUiKnowledge().pages().stream().map(this::candidate).toList();
        ConfirmedPageRegistry registry = sourceResolver.resolve(context.projectProfile(),
                context.normalizedRequirementBundle(), mapped);
        ConfirmedRouteGuard guard = new ConfirmedRouteGuard(registry);
        List<ConfirmedPageCandidate> targets = registry.allPages().stream()
                .filter(page -> requested.targetPageNames().isEmpty()
                        || requested.targetPageNames().stream().anyMatch(name -> same(name, page.pageName())))
                .filter(page -> requested.targetRoutes().isEmpty()
                        || requested.targetRoutes().stream().anyMatch(route -> guard.isConfirmed(page.pageName(), route)
                        && guard.isConfirmed(page.pageName(), page.route())))
                .toList();
        return new TargetPageScope(requested, registry, guard, targets);
    }

    private ConfirmedPageCandidate candidate(MappedPage page) {
        return new ConfirmedPageCandidate(page.pageName(),
                !page.urlPattern().isBlank() ? page.urlPattern() : page.url(), PageCapability.GENERIC,
                PageSource.DB_STABLE_CACHE, 0.86d, List.of("current-ai-context-mapped-ui-knowledge"));
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    public record TargetPageScope(
            AiContextScope requested,
            ConfirmedPageRegistry registry,
            ConfirmedRouteGuard routeGuard,
            List<ConfirmedPageCandidate> targetPages
    ) {
        public TargetPageScope {
            targetPages = targetPages == null ? List.of() : List.copyOf(targetPages);
        }
    }
}
