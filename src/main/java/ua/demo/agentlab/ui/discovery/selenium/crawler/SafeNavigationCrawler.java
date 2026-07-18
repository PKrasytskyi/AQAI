package ua.demo.agentlab.ui.discovery.selenium.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessResult;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessRuleResolver;
import ua.demo.agentlab.ui.discovery.selenium.readiness.PageReadinessWaiter;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiSessionManager;

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
    private final BiDiSessionManager biDiSessionManager;
    private final RequirementNavigationTargetSelector requirementTargetSelector = new RequirementNavigationTargetSelector();

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
        this(pageSnapshotCollector, crawlPolicy, pageEvidenceCaptureService, authenticationService,
                pageReadinessRuleResolver, pageReadinessWaiter, null);
    }

    public SafeNavigationCrawler(
            PageSnapshotCollector pageSnapshotCollector,
            DiscoveryCrawlPolicy crawlPolicy,
            PageEvidenceCaptureService pageEvidenceCaptureService,
            DiscoveryAuthenticationService authenticationService,
            PageReadinessRuleResolver pageReadinessRuleResolver,
            PageReadinessWaiter pageReadinessWaiter,
            BiDiSessionManager biDiSessionManager
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
        this.biDiSessionManager = biDiSessionManager == null
                ? new BiDiSessionManager(ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiDiscoveryConfig.disabled())
                : biDiSessionManager;
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
        List<PageReadinessResult> readinessResults = new ArrayList<>();
        Set<String> visitedUrls = new LinkedHashSet<>();
        ArrayDeque<NavigationTarget> queue = new ArrayDeque<>();
        boolean authenticatedSession = false;

        List<String> explicitStartUrls = crawlPolicy.absoluteStartUrls(projectProfile, requirementBundle);
        for (String startUrl : explicitStartUrls) {
            queue.add(new NavigationTarget(null, "", startUrl, "seed-route", "DIRECT", 0));
        }

        int effectiveMaxPages = crawlPolicy.effectiveMaxPages(projectProfile, requirementBundle);
        while (!queue.isEmpty() && pagesById.size() < effectiveMaxPages) {
            NavigationTarget target = queue.poll();
            String normalizedUrl = normalizeUrl(target.targetUrl());
            if (visitedUrls.contains(normalizedUrl)) {
                continue;
            }
            if (!crawlPolicy.allowsNavigation(projectProfile.baseUrl(), target.targetUrl())) {
                continue;
            }

            biDiSessionManager.start(driver, buildPageIdHint(target.targetUrl()), target.targetUrl());
            try {
                DiscoveryAuthenticationResult authenticationResult =
                        DiscoveryAuthenticationResult.skipped(false, false, target.targetUrl(), "authentication not attempted");
                if (!authenticatedSession && crawlPolicy.allowAuthentication() && authenticationService != null) {
                    authenticationResult = authenticationService.authenticate(driver, projectProfile, target.targetUrl());
                    biDiSessionManager.drain(driver);
                    if (authenticationResult.protectedTarget()) {
                        authenticationResults.add(authenticationResult);
                    }
                    authenticatedSession = authenticationResult.success();
                }
                boolean authenticated = authenticatedSession;
                if (!authenticated || !currentPageMatchesTarget(driver, projectProfile, target.targetUrl())) {
                    navigateToTarget(driver, target);
                    biDiSessionManager.start(driver, buildPageIdHint(driver.getCurrentUrl()), driver.getCurrentUrl());
                    biDiSessionManager.drain(driver);
                }
                PageReadinessRule readinessRule = pageReadinessRuleResolver.resolve(
                        projectProfile,
                        requirementBundle,
                        target.targetUrl()
                );
                PageReadinessResult readinessResult = pageReadinessWaiter.waitUntilReady(driver, readinessRule);
                readinessResults.add(readinessResult);
                biDiSessionManager.drain(driver);
                if (!readinessResult.ready()) {
                    // A timed-out SPA shell is useful diagnostic evidence, but it must not become
                    // mapper input or replace a previously rendered page in stability aggregation.
                    visitedUrls.add(normalizedUrl);
                    continue;
                }
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
                biDiSessionManager.drain(driver);

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

                // The session can remain authenticated after a SPA module transition even when
                // a shallow snapshot does not classify that module as an authenticated-area page.
                // Continue requirement-led traversal so multi-hop paths stay generic.
                if (authenticatedSession || snapshot.authenticatedArea()) {
                    enqueueRequirementTargets(
                            queue,
                            snapshot.pageId(),
                            snapshot.url(),
                            snapshot.links(),
                            target.depth() + 1,
                            projectProfile.baseUrl(),
                            visitedUrls,
                            requirementBundle
                    );
                }

                if (crawlPolicy.followLinks()) {
                    enqueueTargets(queue, snapshot.pageId(), snapshot.url(), snapshot.links(), "LINK", target.depth() + 1, projectProfile.baseUrl(), visitedUrls);
                }

                if (crawlPolicy.followButtons()) {
                    enqueueTargets(queue, snapshot.pageId(), snapshot.url(), snapshot.buttons(), "BUTTON", target.depth() + 1, projectProfile.baseUrl(), visitedUrls);
                }
            } finally {
                biDiSessionManager.stop(driver);
            }
        }

        return new SeleniumDiscoveryResult(
                projectProfile.baseUrl(),
                new ArrayList<>(pagesById.values()),
                transitions,
                1,
                Map.of(),
                authenticationResults,
                readinessResults
        );
    }

    private void enqueueTargets(
            Queue<NavigationTarget> queue,
            String fromPageId,
            String fromUrl,
            List<DiscoveredInteractiveElement> elements,
            String actionType,
            int depth,
            String baseUrl,
            Set<String> visitedUrls
    ) {
        for (DiscoveredInteractiveElement element : elements) {
            if (!element.visible()) {
                continue;
            }
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
                    fromUrl,
                    href,
                    element.visibleText() == null ? actionType.toLowerCase(Locale.ROOT) : element.visibleText(),
                    actionType,
                    depth
            ));
        }
    }

    private void enqueueRequirementTargets(
            ArrayDeque<NavigationTarget> queue,
            String fromPageId,
            String fromUrl,
            List<DiscoveredInteractiveElement> links,
            int depth,
            String baseUrl,
            Set<String> visitedUrls,
            NormalizedRequirementBundle requirements
    ) {
        List<DiscoveredInteractiveElement> selected = requirementTargetSelector.select(links, requirements);
        for (int index = selected.size() - 1; index >= 0; index--) {
            DiscoveredInteractiveElement element = selected.get(index);
            String href = element.href();
            if (!crawlPolicy.allowsNavigation(baseUrl, href) || visitedUrls.contains(normalizeUrl(href))) {
                continue;
            }
            queue.addFirst(new NavigationTarget(
                    fromPageId,
                    fromUrl,
                    href,
                    element.visibleText() == null || element.visibleText().isBlank()
                            ? "targeted navigation"
                            : element.visibleText(),
                    "TARGETED_LINK",
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

    private void navigateToTarget(WebDriver driver, NavigationTarget target) {
        if (targetedLinkClicked(driver, target)) {
            return;
        }
        driver.navigate().to(target.targetUrl());
    }

    private boolean targetedLinkClicked(WebDriver driver, NavigationTarget target) {
        if (driver == null || target == null || !"TARGETED_LINK".equals(target.actionType())
                || target.sourceUrl() == null || target.sourceUrl().isBlank()
                || !routeMatches(RouteCanonicalizer.canonicalize(driver.getCurrentUrl()),
                RouteCanonicalizer.canonicalize(target.sourceUrl()))) {
            return false;
        }
        String targetRoute = RouteCanonicalizer.canonicalize(target.targetUrl());
        for (WebElement link : driver.findElements(By.cssSelector("a[href]"))) {
            try {
                if (!link.isDisplayed() || !link.isEnabled()) {
                    continue;
                }
                String href = link.getDomProperty("href");
                if (routeMatches(RouteCanonicalizer.canonicalize(href), targetRoute)) {
                    link.click();
                    return true;
                }
            } catch (Exception ignored) {
                // The href navigation fallback remains constrained by the crawl policy.
            }
        }
        return false;
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
            String sourceUrl,
            String targetUrl,
            String actionLabel,
            String actionType,
            int depth
    ) {
    }
}
