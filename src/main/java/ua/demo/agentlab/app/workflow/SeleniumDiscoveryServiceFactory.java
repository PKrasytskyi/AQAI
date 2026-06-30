package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.core.config.PropertiesUiRuntimeConfig;
import ua.demo.agentlab.core.config.UiRuntimeConfig;
import ua.demo.agentlab.core.ui.driver.DefaultDriverFactory;
import ua.demo.agentlab.core.ui.driver.DriverFactory;
import ua.demo.agentlab.ui.discovery.classification.PageClassificationService;
import ua.demo.agentlab.ui.discovery.classification.RuleBasedPageClassificationService;
import ua.demo.agentlab.ui.discovery.evidence.LocalPageEvidenceCaptureService;
import ua.demo.agentlab.ui.discovery.policy.DiscoveryCrawlPolicy;
import ua.demo.agentlab.ui.discovery.selenium.SeleniumUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationConfig;
import ua.demo.agentlab.ui.discovery.selenium.auth.DiscoveryAuthenticationService;
import ua.demo.agentlab.ui.discovery.selenium.collector.PageSnapshotCollector;
import ua.demo.agentlab.ui.discovery.selenium.crawler.SafeNavigationCrawler;
import ua.demo.agentlab.ui.discovery.selenium.extractor.FormStructureExtractor;
import ua.demo.agentlab.ui.discovery.selenium.extractor.InteractiveElementExtractor;

public class SeleniumDiscoveryServiceFactory {

    public SeleniumUiDiscoveryService create(ProjectProfile projectProfile) {
        if (projectProfile == null) {
            throw new IllegalArgumentException("projectProfile cannot be null");
        }
        UiRuntimeConfig runtimeConfig = new PropertiesUiRuntimeConfig();
        DriverFactory discoveryDriverFactory = new DefaultDriverFactory(runtimeConfig);
        InteractiveElementExtractor interactiveElementExtractor = new InteractiveElementExtractor();
        FormStructureExtractor formStructureExtractor = new FormStructureExtractor(interactiveElementExtractor);
        PageSnapshotCollector pageSnapshotCollector = new PageSnapshotCollector(
                interactiveElementExtractor,
                formStructureExtractor
        );
        DiscoveryCrawlPolicy discoveryCrawlPolicy = DiscoveryCrawlPolicy.defaultPolicy(projectProfile);
        SafeNavigationCrawler safeNavigationCrawler = new SafeNavigationCrawler(
                pageSnapshotCollector,
                discoveryCrawlPolicy,
                new LocalPageEvidenceCaptureService(),
                new DiscoveryAuthenticationService(new DiscoveryAuthenticationConfig())
        );
        PageClassificationService pageClassificationService = new RuleBasedPageClassificationService();
        return new SeleniumUiDiscoveryService(
                discoveryDriverFactory,
                discoveryCrawlPolicy,
                safeNavigationCrawler,
                pageClassificationService
        );
    }
}
