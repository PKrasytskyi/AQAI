package ua.demo.agentlab.ui.discovery.selenium.crawler;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.evidence.PageEvidenceCaptureService;
import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;
import ua.demo.agentlab.ui.discovery.policy.DiscoveryCrawlPolicy;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationResult;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.collector.PageSnapshotCollector;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRule;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessWaiter;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class SafeNavigationCrawler {

    private final PageSnapshotCollector pageSnapshotCollector;
    private final DiscoveryCrawlPolicy crawlPolicy;
    private final PageEvidenceCaptureService pageEvidenceCaptureService;
    private final DiscoveryAuthenticationService authenticationService;
    private final PageReadinessRuleResolver pageReadinessRuleResolver;
    private final PageReadinessWaiter pageReadinessWaiter;

    public SafeNavigationCrawler(
            PageSnapshotCollector pageSnapshotCollector,
            DiscoveryCrawlPolicy crawlPolicy,
            PageEvidenceCaptureService pageEvidenceCaptureService
    ) {
        this(pageSnapshotCollector, crawlPolicy, pageEvidenceCaptureService, null);
    }

    public SafeNavigationCrawler(
            PageSnapshotCollector pageSnapshotCollector,
            DiscoveryCrawlPolicy crawlPolicy,
            PageEvidenceCaptureService pageEvidenceCaptureService,
            DiscoveryAuthenticationService authenticationService
    ) {
        this(pageSnapshotCollector, crawlPolicy, pageEvidenceCaptureService, authenticationService,
                new PageReadinessRuleResolver(), new PageReadinessWaiter());
    }

    public SafeNavigationCrawler(
            PageSnapshotCollector pageSnapshotCollector,
            DiscoveryCrawlPolicy crawlPolicy,
            PageEvidenceCaptureService pageEvidenceCaptureService,
            DiscoveryAuthenticationService authenticationService,
            PageReadinessRuleResolver pageReadinessRuleResolver,
            PageReadinessWaiter pageReadinessWaiter
    ) {
        if (pageSnapshotCollector == null) {
            throw new IllegalArgumentException("pageSnapshotCollector cannot be null");
        }
        if (crawlPolicy == null) {
            throw new IllegalArgumentException("crawlPolicy cannot be null");
        }
        if (pageEvidenceCaptureService == null) {
            throw new IllegalArgumentException("pageEvidenceCaptureService cannot be null");
        }
        this.pageSnapshotCollector = pageSnapshotCollector;
        this.crawlPolicy = crawlPolicy;
        this.pageEvidenceCaptureService = pageEvidenceCaptureService;
        this.authenticationService = authenticationService;
        this.pageReadinessRuleResolver = pageReadinessRuleResolver == null
                ? new PageReadinessRuleResolver()
                : pageReadinessRuleResolver;
        this.pageReadinessWaiter = pageReadinessWaiter == null
                ? new PageReadinessWaiter()
                : pageReadinessWaiter;
    }

    public SeleniumDiscoveryResult crawl(WebDriver driver, ProjectProfile projectProfile) {
        return crawl(driver, projectProfile, null);
    }

    public SeleniumDiscoveryResult crawl(
            WebDriver driver,
            ProjectProfile projectProfile,
            NormalizedRequirementBundle requirementBundle
    ) {
        Map<String, DiscoveredPageSnapshot> pagesById = new LinkedHashMap<>();
        List<DiscoveredTransition> transitions = new ArrayList<>();
        List<DiscoveryAuthenticationResult> authenticationResults = new ArrayList<>();
        Set<String> visitedUrls = new LinkedHashSet<>();
        ArrayDeque<NavigationTarget> queue = new ArrayDeque<>();

        for (String startUrl : crawlPolicy.absoluteStartUrls(projectProfile, requirementBundle)) {
            queue.add(new NavigationTarget(null, startUrl, "seed-route", "DIRECT", 0));
        }

        while (!queue.isEmpty() && pagesById.size() < crawlPolicy.maxPages()) {
            NavigationTarget target = queue.poll();
            String normalizedUrl = normalizeUrl(target.targetUrl());
            if (visitedUrls.contains(normalizedUrl)) {
                continue;
            }
            if (!crawlPolicy.allowsNavigation(projectProfile.baseUrl(), target.targetUrl())) {
                continue;
            }

            DiscoveryAuthenticationResult authenticationResult =
                    DiscoveryAuthenticationResult.skipped(false, false, target.targetUrl(), "authentication not attempted");
            boolean authenticated = false;
            if (crawlPolicy.allowAuthentication() && authenticationService != null) {
                authenticationResult = authenticationService.authenticate(driver, projectProfile, target.targetUrl());
                if (authenticationResult.protectedTarget()) {
                    authenticationResults.add(authenticationResult);
                }
                authenticated = authenticationResult.success();
            }
            if (!authenticated || !currentPageMatchesTarget(driver, projectProfile, target.targetUrl())) {
                driver.navigate().to(target.targetUrl());
            }
            PageReadinessRule readinessRule = pageReadinessRuleResolver.resolve(
                    projectProfile,
                    requirementBundle,
                    target.targetUrl()
            );
            pageReadinessWaiter.waitUntilReady(driver, readinessRule);
            if (shouldSkipRedirectedProtectedPage(driver, projectProfile, authenticationResult)) {
                visitedUrls.add(normalizedUrl);
                continue;
            }
            String pageIdHint = buildPageIdHint(driver.getCurrentUrl());
            DiscoveredPageEvidence evidence = pageEvidenceCaptureService.capture(driver, pageIdHint);
            DiscoveredPageSnapshot snapshot = pageSnapshotCollector.collect(
                    driver,
                    pageIdHint,
                    evidence
            );

            pagesById.putIfAbsent(snapshot.pageId(), snapshot);
            visitedUrls.add(normalizedUrl);

            if (target.fromPageId() != null) {
                transitions.add(new DiscoveredTransition(
                        target.fromPageId(),
                        target.actionLabel(),
                        target.actionType(),
                        snapshot.pageId(),
                        snapshot.url(),
                        true
                ));
            }

            if (target.depth() >= crawlPolicy.maxDepth()) {
                continue;
            }

            if (crawlPolicy.followLinks()) {
                enqueueTargets(queue, snapshot.pageId(), snapshot.links(), "LINK", target.depth() + 1, projectProfile.baseUrl(), visitedUrls);
            }

            if (crawlPolicy.followButtons()) {
                enqueueTargets(queue, snapshot.pageId(), snapshot.buttons(), "BUTTON", target.depth() + 1, projectProfile.baseUrl(), visitedUrls);
            }
        }

        return new SeleniumDiscoveryResult(
                projectProfile.baseUrl(),
                new ArrayList<>(pagesById.values()),
                transitions,
                1,
                Map.of(),
                authenticationResults
        );
    }

    private void enqueueTargets(
            Queue<NavigationTarget> queue,
            String fromPageId,
            List<DiscoveredInteractiveElement> elements,
            String actionType,
            int depth,
            String baseUrl,
            Set<String> visitedUrls
    ) {
        for (DiscoveredInteractiveElement element : elements) {
            String href = element.href();
            if (href == null || href.isBlank()) {
                continue;
            }
            if (crawlPolicy.isBlockedAction(element.visibleText())) {
                continue;
            }
            if (!crawlPolicy.allowsNavigation(baseUrl, href)) {
                continue;
            }

            String normalizedUrl = normalizeUrl(href);
            if (visitedUrls.contains(normalizedUrl)) {
                continue;
            }

            queue.add(new NavigationTarget(
                    fromPageId,
                    href,
                    element.visibleText() == null ? actionType.toLowerCase(Locale.ROOT) : element.visibleText(),
                    actionType,
                    depth
            ));
        }
    }

    private String buildPageIdHint(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return "home-page";
            }

            String normalized = path.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "");
            return normalized.isBlank() ? "page" : normalized.toLowerCase(Locale.ROOT);
        } catch (Exception exception) {
            return "page";
        }
    }

    private String normalizeUrl(String value) {
        try {
            URI uri = URI.create(value);
            URI normalized = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    null,
                    null
            );
            return normalized.toString();
        } catch (Exception exception) {
            return value;
        }
    }

    private boolean currentPageMatchesTarget(WebDriver driver, ProjectProfile projectProfile, String targetUrl) {
        if (driver == null || projectProfile == null || targetUrl == null || targetUrl.isBlank()) {
            return false;
        }
        String currentRoute = ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(driver.getCurrentUrl());
        String targetRoute = ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(targetUrl);
        String authenticatedRoute = ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(projectProfile.authenticatedRoute());
        String securityRoute = ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(projectProfile.securityRoute());
        return routeMatches(currentRoute, targetRoute)
                || routeMatches(currentRoute, authenticatedRoute)
                && routeMatches(targetRoute, authenticatedRoute)
                || routeMatches(currentRoute, securityRoute)
                && routeMatches(targetRoute, securityRoute);
    }

    private boolean shouldSkipRedirectedProtectedPage(
            WebDriver driver,
            ProjectProfile projectProfile,
            DiscoveryAuthenticationResult authenticationResult
    ) {
        if (driver == null || projectProfile == null || authenticationResult == null
                || !authenticationResult.protectedTarget() || authenticationResult.success()) {
            return false;
        }
        String currentRoute = ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(driver.getCurrentUrl());
        String loginRoute = ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.canonicalize(projectProfile.loginRoute());
        return routeMatches(currentRoute, loginRoute);
    }

    private boolean routeMatches(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        return ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.routeEqualsOrSuffix(left, right);
    }

    private record NavigationTarget(
            String fromPageId,
            String targetUrl,
            String actionLabel,
            String actionType,
            int depth
    ) {
    }
}
