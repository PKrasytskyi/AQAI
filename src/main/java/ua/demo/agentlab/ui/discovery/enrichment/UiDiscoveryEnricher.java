package ua.demo.agentlab.ui.discovery.enrichment;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.classification.PageClassificationResult;
import ua.demo.agentlab.ui.discovery.classification.PageClassificationService;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiFlow;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UiDiscoveryEnricher {

    private final PageClassificationService pageClassificationService;

    public UiDiscoveryEnricher(PageClassificationService pageClassificationService) {
        if (pageClassificationService == null) {
            throw new IllegalArgumentException("pageClassificationService cannot be null");
        }
        this.pageClassificationService = pageClassificationService;
    }

    public UiDiscoverySnapshot enrich(UiDiscoverySnapshot baseSnapshot, SeleniumDiscoveryResult seleniumDiscoveryResult) {
        if (baseSnapshot == null) {
            throw new IllegalArgumentException("baseSnapshot cannot be null");
        }
        if (seleniumDiscoveryResult == null) {
            throw new IllegalArgumentException("seleniumDiscoveryResult cannot be null");
        }

        List<DiscoveredUiPage> mergedPages = mergePages(baseSnapshot.pages(), seleniumDiscoveryResult);
        List<DiscoveredUiFlow> mergedFlows = mergeFlows(baseSnapshot.flows(), seleniumDiscoveryResult, mergedPages);

        return new UiDiscoverySnapshot(
                baseSnapshot.projectProfileId(),
                baseSnapshot.projectName(),
                baseSnapshot.source(),
                mergedPages,
                mergedFlows
        );
    }

    private List<DiscoveredUiPage> mergePages(
            List<DiscoveredUiPage> basePages,
            SeleniumDiscoveryResult seleniumDiscoveryResult
    ) {
        Map<String, DiscoveredUiPage> pagesByName = new LinkedHashMap<>();
        for (DiscoveredUiPage page : basePages) {
            pagesByName.put(pageKey(page), page);
        }

        for (DiscoveredPageSnapshot snapshot : seleniumDiscoveryResult.pages()) {
            PageClassificationResult classification =
                    pageClassificationService.classifyPage(snapshot, new ArrayList<>(pagesByName.values()));
            DiscoveredUiPage browserPage = new DiscoveredUiPage(
                    classification.pageName(),
                    toRelativeRoute(snapshot.url(), seleniumDiscoveryResult.baseUrl()),
                    classification.inferredCapabilities(),
                    snapshot.locatorHints(),
                    "Selenium page enrichment: " + classification.classificationReason(),
                    classification.canonicalPageType(),
                    classification.pageIdentity()
            );

            pagesByName.merge(pageKey(browserPage), browserPage, this::mergePage);
        }

        return new ArrayList<>(pagesByName.values());
    }

    private List<DiscoveredUiFlow> mergeFlows(
            List<DiscoveredUiFlow> baseFlows,
            SeleniumDiscoveryResult seleniumDiscoveryResult,
            List<DiscoveredUiPage> mergedPages
    ) {
        Map<String, DiscoveredUiFlow> flowsByKey = new LinkedHashMap<>();
        for (DiscoveredUiFlow flow : baseFlows) {
            flowsByKey.put(flowMergeKey(flow), flow);
        }

        Map<String, String> pageNameById = new LinkedHashMap<>();
        Map<String, DiscoveredPageSnapshot> pagesById = new LinkedHashMap<>();
        for (DiscoveredPageSnapshot page : seleniumDiscoveryResult.pages()) {
            pagesById.put(page.pageId(), page);
            pageNameById.put(page.pageId(), pageClassificationService.classifyPage(page, mergedPages).pageName());
        }

        int index = 1;
        for (DiscoveredTransition transition : seleniumDiscoveryResult.transitions()) {
            DiscoveredPageSnapshot sourcePage = pagesById.get(transition.fromPageId());
            DiscoveredPageSnapshot targetPage = pagesById.get(transition.toPageId());
            String targetPageName = pageNameById.getOrDefault(transition.toPageId(), "HomePage");
            String flowType = pageClassificationService.inferFlowType(
                    transition,
                    sourcePage,
                    targetPage,
                    targetPageName
            );
            if (flowType == null) {
                continue;
            }

            DiscoveredUiFlow browserFlow = new DiscoveredUiFlow(
                    "enriched-browser-flow-" + index,
                    buildFlowName(transition, targetPageName),
                    flowType,
                    pageNameById.get(transition.fromPageId()),
                    sourcePage == null ? "/" : toRelativeRoute(sourcePage.url(), seleniumDiscoveryResult.baseUrl()),
                    targetPageName,
                    targetPage == null ? "/" : toRelativeRoute(targetPage.url(), seleniumDiscoveryResult.baseUrl()),
                    (sourcePage != null && sourcePage.authenticatedArea())
                            || (targetPage != null && targetPage.authenticatedArea()),
                    buildStepDescriptions(transition, targetPageName),
                    defaultExpectedOutcomes(flowType, targetPageName),
                    tokenize((transition.actionLabel() == null ? "" : transition.actionLabel()) + " " + transition.toUrl()),
                    List.of()
            );

            String key = flowMergeKey(browserFlow);
            flowsByKey.merge(key, browserFlow, this::mergeFlow);
            index++;
        }

        return new ArrayList<>(flowsByKey.values());
    }

    private String flowMergeKey(DiscoveredUiFlow flow) {
        if (flow.sourceRequirementIds() != null && !flow.sourceRequirementIds().isEmpty()) {
            return "requirement|" + String.join(",", flow.sourceRequirementIds());
        }

        return "browser|" + flow.flowType() + "|" + flow.targetPageName();
    }

    private DiscoveredUiPage mergePage(DiscoveredUiPage basePage, DiscoveredUiPage browserPage) {
        Set<String> capabilities = new LinkedHashSet<>(basePage.capabilities());
        capabilities.addAll(browserPage.capabilities());

        List<LocatorHint> locatorHints = mergeLocatorHints(basePage.locatorHints(), browserPage.locatorHints());
        String route = choosePreferredRoute(basePage.route(), browserPage.route());
        String reason = basePage.discoveryReason() + " + " + browserPage.discoveryReason();

        return new DiscoveredUiPage(
                basePage.pageName(),
                route,
                new ArrayList<>(capabilities),
                locatorHints,
                reason,
                basePage.canonicalPageType(),
                chooseIdentity(basePage, browserPage, route)
        );
    }

    private String pageKey(DiscoveredUiPage page) {
        if (page.pageIdentity() != null && !page.pageIdentity().key().isBlank()) {
            return page.pageIdentity().key();
        }
        return page.pageName();
    }

    private PageIdentity chooseIdentity(DiscoveredUiPage basePage, DiscoveredUiPage browserPage, String route) {
        if (browserPage.pageIdentity() != null && !browserPage.pageIdentity().key().isBlank()) {
            return browserPage.pageIdentity();
        }
        if (basePage.pageIdentity() != null && !basePage.pageIdentity().key().isBlank()) {
            return new PageIdentity(
                    basePage.pageIdentity().key(),
                    basePage.pageIdentity().canonicalPageType(),
                    basePage.pageIdentity().className(),
                    basePage.pageIdentity().displayName(),
                    route,
                    basePage.pageIdentity().aliases()
            );
        }
        return PageIdentity.legacy(basePage.pageName(), basePage.canonicalPageType().mappedType(), route);
    }

    private DiscoveredUiFlow mergeFlow(DiscoveredUiFlow baseFlow, DiscoveredUiFlow browserFlow) {
        Set<String> steps = new LinkedHashSet<>(baseFlow.stepDescriptions());
        steps.addAll(browserFlow.stepDescriptions());

        Set<String> outcomes = new LinkedHashSet<>(baseFlow.expectedOutcomes());
        outcomes.addAll(browserFlow.expectedOutcomes());

        Set<String> keywords = new LinkedHashSet<>(baseFlow.matchKeywords());
        keywords.addAll(browserFlow.matchKeywords());

        Set<String> requirementIds = new LinkedHashSet<>(baseFlow.sourceRequirementIds());
        requirementIds.addAll(browserFlow.sourceRequirementIds());

        return new DiscoveredUiFlow(
                baseFlow.flowId(),
                baseFlow.flowName(),
                baseFlow.flowType(),
                choosePreferredValue(baseFlow.sourcePageName(), browserFlow.sourcePageName()),
                choosePreferredValue(baseFlow.sourceRoute(), browserFlow.sourceRoute()),
                baseFlow.targetPageName(),
                choosePreferredValue(baseFlow.targetRoute(), browserFlow.targetRoute()),
                baseFlow.authenticationRequired() || browserFlow.authenticationRequired(),
                new ArrayList<>(steps),
                new ArrayList<>(outcomes),
                new ArrayList<>(keywords),
                new ArrayList<>(requirementIds)
        );
    }

    private String buildFlowName(DiscoveredTransition transition, String targetPageName) {
        if (transition.actionLabel() != null && !transition.actionLabel().isBlank()) {
            return transition.actionLabel();
        }
        return "Navigate to " + targetPageName;
    }

    private List<String> buildStepDescriptions(DiscoveredTransition transition, String targetPageName) {
        return List.of(
                "Open source page",
                "Trigger action: " + buildFlowName(transition, targetPageName),
                "Reach " + targetPageName
        );
    }

    private List<String> defaultExpectedOutcomes(String flowType, String targetPageName) {
        return switch (flowType) {
            case "RECOVERY" -> List.of("Recovery flow is reachable");
            case "LOGOUT" -> List.of("User leaves the authenticated area");
            case "OPEN_DETAILS" -> List.of(targetPageName + " is visible");
            case "CREATE_ENTITY", "SUBMIT_FORM" -> List.of("Submission succeeds");
            case "SECURITY_CHALLENGE" -> List.of("Security-related behavior is visible");
            case "AUTHENTICATE" -> List.of("Authentication succeeds");
            default -> List.of("Target page is reachable");
        };
    }

    private List<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (token.length() >= 4) {
                tokens.add(token);
            }
        }
        return new ArrayList<>(tokens);
    }

    private List<LocatorHint> mergeLocatorHints(List<LocatorHint> left, List<LocatorHint> right) {
        List<LocatorHint> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (LocatorHint locatorHint : left) {
            addLocator(merged, seen, locatorHint);
        }
        for (LocatorHint locatorHint : right) {
            addLocator(merged, seen, locatorHint);
        }

        return merged;
    }

    private void addLocator(List<LocatorHint> merged, Set<String> seen, LocatorHint locatorHint) {
        String key = locatorHint.elementName() + "|" + locatorHint.recommendedStrategy() + "|" + locatorHint.recommendedValue();
        if (seen.add(key)) {
            merged.add(locatorHint);
        }
    }

    private String choosePreferredRoute(String baseRoute, String browserRoute) {
        if (browserRoute != null && !browserRoute.isBlank() && !"/".equals(browserRoute)) {
            return browserRoute;
        }
        return baseRoute;
    }

    private String choosePreferredValue(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return secondary;
    }

    private String toRelativeRoute(String url, String baseUrl) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return "/";
            }
            return path;
        } catch (Exception exception) {
            if (baseUrl != null && url.startsWith(baseUrl)) {
                return url.substring(baseUrl.length());
            }
            return url;
        }
    }
}
