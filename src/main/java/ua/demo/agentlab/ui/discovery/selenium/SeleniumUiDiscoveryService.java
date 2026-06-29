package ua.demo.agentlab.ui.discovery.selenium;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.UiDiscoveryService;
import ua.demo.agentlab.ui.discovery.classification.PageClassificationResult;
import ua.demo.agentlab.ui.discovery.classification.PageClassificationService;
import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiFlow;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.policy.DiscoveryCrawlPolicy;
import ua.demo.agentlab.ui.discovery.selenium.crawler.SafeNavigationCrawler;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.discovery.selenium.stability.SeleniumDiscoveryStabilityAggregator;
import ua.demo.agentlab.core.ui.driver.DriverFactory;

import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SeleniumUiDiscoveryService implements UiDiscoveryService {

    private final DriverFactory driverFactory;
    private final DiscoveryCrawlPolicy crawlPolicy;
    private final SafeNavigationCrawler safeNavigationCrawler;
    private final PageClassificationService pageClassificationService;
    private final SeleniumDiscoveryStabilityAggregator stabilityAggregator;
    private final int discoveryRunCount;

    public SeleniumUiDiscoveryService(
            DriverFactory driverFactory,
            DiscoveryCrawlPolicy crawlPolicy,
            SafeNavigationCrawler safeNavigationCrawler,
            PageClassificationService pageClassificationService
    ) {
        this(driverFactory, crawlPolicy, safeNavigationCrawler, pageClassificationService,
                new SeleniumDiscoveryStabilityAggregator(), configuredDiscoveryRunCount());
    }

    public SeleniumUiDiscoveryService(
            DriverFactory driverFactory,
            DiscoveryCrawlPolicy crawlPolicy,
            SafeNavigationCrawler safeNavigationCrawler,
            PageClassificationService pageClassificationService,
            SeleniumDiscoveryStabilityAggregator stabilityAggregator,
            int discoveryRunCount
    ) {
        if (driverFactory == null) {
            throw new IllegalArgumentException("driverFactory cannot be null");
        }
        if (crawlPolicy == null) {
            throw new IllegalArgumentException("crawlPolicy cannot be null");
        }
        if (safeNavigationCrawler == null) {
            throw new IllegalArgumentException("safeNavigationCrawler cannot be null");
        }
        if (pageClassificationService == null) {
            throw new IllegalArgumentException("pageClassificationService cannot be null");
        }
        if (stabilityAggregator == null) {
            throw new IllegalArgumentException("stabilityAggregator cannot be null");
        }
        this.driverFactory = driverFactory;
        this.crawlPolicy = crawlPolicy;
        this.safeNavigationCrawler = safeNavigationCrawler;
        this.pageClassificationService = pageClassificationService;
        this.stabilityAggregator = stabilityAggregator;
        this.discoveryRunCount = Math.max(1, Math.min(5, discoveryRunCount));
    }

    @Override
    public UiDiscoveryResult discover(ProjectProfile projectProfile, NormalizedRequirementBundle requirementBundle) {
        SeleniumDiscoveryResult rawResult = discoverRaw(projectProfile, requirementBundle);
        UiDiscoverySnapshot snapshot = new UiDiscoverySnapshot(
                projectProfile.profileId(),
                projectProfile.projectName(),
                requirementBundle.source(),
                toDiscoveredPages(rawResult),
                toDiscoveredFlows(rawResult)
        );
        return new UiDiscoveryResult(snapshot, rawResult);
    }

    public SeleniumDiscoveryResult discoverRaw(ProjectProfile projectProfile) {
        return discoverRaw(projectProfile, null);
    }

    public SeleniumDiscoveryResult discoverRaw(
            ProjectProfile projectProfile,
            NormalizedRequirementBundle requirementBundle
    ) {
        List<SeleniumDiscoveryResult> runs = new ArrayList<>();
        RuntimeException firstFailure = null;
        for (int runIndex = 0; runIndex < discoveryRunCount; runIndex++) {
            WebDriver driver = driverFactory.createDriver();
            try {
                runs.add(safeNavigationCrawler.crawl(driver, projectProfile, requirementBundle));
            } catch (RuntimeException exception) {
                if (firstFailure == null) {
                    firstFailure = exception;
                }
            } finally {
                driverFactory.shutdownDriver(driver);
            }
        }
        if (runs.isEmpty() && firstFailure != null) {
            throw firstFailure;
        }
        return stabilityAggregator.aggregate(runs);
    }

    private static int configuredDiscoveryRunCount() {
        String value = firstNonBlank(
                System.getProperty("ui.discovery.repeat-runs"),
                System.getenv("UI_DISCOVERY_REPEAT_RUNS")
        );
        if (value.isBlank()) {
            return 3;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 3;
        }
    }

    private List<DiscoveredUiPage> toDiscoveredPages(SeleniumDiscoveryResult rawResult) {
        List<DiscoveredUiPage> pages = new ArrayList<>();
        for (DiscoveredPageSnapshot snapshot : rawResult.pages()) {
            PageClassificationResult classification = pageClassificationService.classifyPage(snapshot, List.of());
            pages.add(new DiscoveredUiPage(
                    classification.pageName(),
                    toRelativeRoute(snapshot.url(), rawResult.baseUrl()),
                    classification.inferredCapabilities(),
                    snapshot.locatorHints(),
                    "Selenium snapshot discovery: " + classification.classificationReason(),
                    classification.canonicalPageType(),
                    classification.pageIdentity()
            ));
        }
        return pages;
    }

    private List<DiscoveredUiFlow> toDiscoveredFlows(SeleniumDiscoveryResult rawResult) {
        List<DiscoveredUiFlow> flows = new ArrayList<>();
        int index = 1;

        for (DiscoveredTransition transition : rawResult.transitions()) {
            if (!transition.success()) {
                continue;
            }

            DiscoveredPageSnapshot sourcePage = findPage(rawResult, transition.fromPageId());
            DiscoveredPageSnapshot targetPage = findPage(rawResult, transition.toPageId());
            PageClassificationResult targetClassification =
                    pageClassificationService.classifyPage(targetPage, List.of());
            String flowType = pageClassificationService.inferFlowType(
                    transition,
                    sourcePage,
                    targetPage,
                    targetClassification.pageName()
            );
            if (flowType == null) {
                continue;
            }

            String targetPageName = targetClassification.pageName();
            String sourcePageName = sourcePage == null
                    ? targetPageName
                    : pageClassificationService.classifyPage(sourcePage, List.of()).pageName();
            String flowName = transition.actionLabel() == null || transition.actionLabel().isBlank()
                    ? "Browser discovered flow " + index
                    : transition.actionLabel();

            flows.add(new DiscoveredUiFlow(
                    "browser-flow-" + index,
                    flowName,
                    flowType,
                    sourcePageName,
                    sourcePage == null ? "/" : toRelativeRoute(sourcePage.url(), rawResult.baseUrl()),
                    targetPageName,
                    toRelativeRoute(targetPage.url(), rawResult.baseUrl()),
                    (sourcePage != null && sourcePage.authenticatedArea()) || targetPage.authenticatedArea(),
                    List.of(
                            "Open source page",
                            "Trigger action: " + flowName,
                            "Reach " + targetPageName
                    ),
                    defaultExpectedOutcomes(flowType, targetPageName),
                    tokenize(flowName + " " + transition.toUrl()),
                    List.of()
            ));
            index++;
        }

        return flows;
    }

    private DiscoveredPageSnapshot findPage(SeleniumDiscoveryResult result, String pageId) {
        return result.pages().stream()
                .filter(page -> page.pageId().equals(pageId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown discovered page id: " + pageId));
    }

    private List<String> defaultExpectedOutcomes(String flowType, String targetPageName) {
        return switch (flowType) {
            case "RECOVERY" -> List.of("Recovery flow is reachable");
            case "LOGOUT" -> List.of("User leaves the authenticated area");
            case "OPEN_DETAILS" -> List.of(targetPageName + " is visible");
            case "CREATE_ENTITY", "SUBMIT_FORM" -> List.of("Submission succeeds");
            case "SECURITY_CHALLENGE" -> List.of("Security-related behavior is visible");
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
