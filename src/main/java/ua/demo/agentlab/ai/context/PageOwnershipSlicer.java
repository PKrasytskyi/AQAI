package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PageOwnershipSlicer {

    public PromptPageScope slice(AiContextPackage context) {
        if (context == null || context.mappedUiKnowledge() == null
                || context.mappedUiKnowledge().pages().size() != 1) {
            return null;
        }
        MappedPage targetPage = context.mappedUiKnowledge().pages().stream().findFirst().orElse(null);
        if (targetPage == null) return null;
        Set<String> requirementIds = requirementIds(context);
        boolean requiresAuthentication = requiresAuthentication(targetPage);
        List<String> prerequisitePages = prerequisitePages(context, targetPage, requiresAuthentication);
        List<ExcludedEvidence> excluded = context.mappedUiKnowledgeCurated() == null
                ? List.of()
                : context.mappedUiKnowledgeCurated().excludedEvidence().stream()
                .filter(evidence -> evidence.pageId().isBlank() || evidence.pageId().equals(targetPage.pageId()))
                .limit(20)
                .toList();
        List<String> sourceTrace = new ArrayList<>();
        sourceTrace.add("prompt-evidence:scoped-context");
        sourceTrace.add("prompt-evidence:targetPage=" + targetPage.pageName());
        sourceTrace.add("prompt-evidence:requirementIds=" + requirementIds);
        if (requiresAuthentication) {
            sourceTrace.add("prompt-evidence:requiresAuthentication=true");
        }
        return new PromptPageScope(
                targetPage,
                targetPage.pageName(),
                route(targetPage),
                requiresAuthentication,
                prerequisitePages,
                new ArrayList<>(requirementIds),
                excluded,
                sourceTrace
        );
    }

    public boolean canonicalTestCaseBelongsToTarget(CanonicalTestCase testCase, MappedPage targetPage) {
        if (testCase == null || targetPage == null) {
            return false;
        }
        if (pageNameMatches(testCase.pageName(), targetPage)
                || pageNameMatches(testCase.sourcePageName(), targetPage)
                || RouteCanonicalizer.routeEqualsOrSuffix(testCase.route(), route(targetPage))
                || RouteCanonicalizer.routeEqualsOrSuffix(testCase.sourceRoute(), route(targetPage))) {
            return true;
        }
        for (String page : testCase.targetPages()) {
            if (pageNameMatches(page, targetPage) || RouteCanonicalizer.routeEqualsOrSuffix(page, route(targetPage))) {
                return true;
            }
        }
        return false;
    }

    public boolean canonicalTestCaseSourceBelongsToTarget(CanonicalTestCase testCase, MappedPage targetPage) {
        if (testCase == null || targetPage == null) {
            return false;
        }
        return pageNameMatches(testCase.sourcePageName(), targetPage)
                || RouteCanonicalizer.routeEqualsOrSuffix(testCase.sourceRoute(), route(targetPage));
    }

    public boolean pageNameMatches(String ownerPage, MappedPage targetPage) {
        String owner = fieldHint(ownerPage);
        String target = fieldHint(targetPage.pageName());
        return !owner.isBlank() && owner.equalsIgnoreCase(target);
    }

    public boolean isLoginPage(MappedPage targetPage) {
        String pageName = targetPage.pageName().toLowerCase(Locale.ROOT);
        String route = route(targetPage).toLowerCase(Locale.ROOT);
        return pageName.contains("login") || route.contains("login") || route.contains("auth/login");
    }

    public boolean isAuthenticationPage(MappedPage targetPage) {
        if (targetPage == null) {
            return false;
        }
        String pageName = targetPage.pageName().toLowerCase(Locale.ROOT);
        String pageType = targetPage.pageType().toLowerCase(Locale.ROOT);
        String route = route(targetPage).toLowerCase(Locale.ROOT);
        return pageName.contains("login")
                || pageType.equals("authentication")
                || route.contains("login")
                || route.contains("auth/login");
    }

    public String route(MappedPage page) {
        return page.urlPattern().isBlank() ? page.url() : page.urlPattern();
    }

    public String fieldHint(String value) {
        String normalized = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "element";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            builder.append(parts[index].substring(0, 1).toUpperCase()).append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private boolean requiresAuthentication(MappedPage targetPage) {
        if (targetPage != null && targetPage.stateHints() != null && targetPage.stateHints().requiresAuthentication()) {
            return true;
        }
        String evidence = String.join(" ",
                targetPage == null ? "" : targetPage.pageName(),
                targetPage == null ? "" : targetPage.pageType(),
                targetPage == null ? "" : route(targetPage)
        ).toLowerCase(Locale.ROOT);
        return containsAny(evidence, "dashboard", "authenticated", "secure", "protected");
    }

    private List<String> prerequisitePages(
            AiContextPackage context,
            MappedPage targetPage,
            boolean requiresAuthentication
    ) {
        Set<String> pages = new LinkedHashSet<>();
        if (context != null && context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                if (canonicalTestCaseBelongsToTarget(testCase, targetPage)
                        && !canonicalTestCaseSourceBelongsToTarget(testCase, targetPage)
                        && testCase.sourcePageName() != null
                        && !testCase.sourcePageName().isBlank()) {
                    pages.add(testCase.sourcePageName().trim());
                }
            }
        }
        if (pages.isEmpty() && requiresAuthentication && !isLoginPage(targetPage)) {
            pages.add("LoginPage");
        }
        return new ArrayList<>(pages);
    }

    private Set<String> requirementIds(AiContextPackage context) {
        Set<String> ids = new LinkedHashSet<>();
        if (context.canonicalTestCaseBundle() != null) {
            context.canonicalTestCaseBundle().testCases().forEach(testCase -> ids.addAll(testCase.requirementRefs()));
        }
        for (AssertionContract contract : context.assertionContracts()) {
            addIfPresent(ids, contract.requirementId());
        }
        return ids;
    }

    private boolean containsAny(String value, String... fragments) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }
}
