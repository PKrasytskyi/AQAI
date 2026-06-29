package ua.demo.agentlab.ui.discovery;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.enrichment.UiDiscoveryEnricher;
import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.selenium.SeleniumUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public class CompositeUiDiscoveryService implements UiDiscoveryService {

    private final UiDiscoveryService ruleBasedDiscoveryService;
    private final SeleniumUiDiscoveryService seleniumUiDiscoveryService;
    private final UiDiscoveryEnricher uiDiscoveryEnricher;

    public CompositeUiDiscoveryService(
            UiDiscoveryService ruleBasedDiscoveryService,
            SeleniumUiDiscoveryService seleniumUiDiscoveryService,
            UiDiscoveryEnricher uiDiscoveryEnricher
    ) {
        if (ruleBasedDiscoveryService == null) {
            throw new IllegalArgumentException("ruleBasedDiscoveryService cannot be null");
        }
        if (seleniumUiDiscoveryService == null) {
            throw new IllegalArgumentException("seleniumUiDiscoveryService cannot be null");
        }
        if (uiDiscoveryEnricher == null) {
            throw new IllegalArgumentException("uiDiscoveryEnricher cannot be null");
        }
        this.ruleBasedDiscoveryService = ruleBasedDiscoveryService;
        this.seleniumUiDiscoveryService = seleniumUiDiscoveryService;
        this.uiDiscoveryEnricher = uiDiscoveryEnricher;
    }

    @Override
    public UiDiscoveryResult discover(ProjectProfile projectProfile, NormalizedRequirementBundle requirementBundle) {
        UiDiscoveryResult baseResult = ruleBasedDiscoveryService.discover(projectProfile, requirementBundle);
        UiDiscoverySnapshot baseSnapshot = baseResult.snapshot();

        try {
            SeleniumDiscoveryResult seleniumDiscoveryResult =
                    seleniumUiDiscoveryService.discoverRaw(projectProfile, requirementBundle);
            return new UiDiscoveryResult(
                    uiDiscoveryEnricher.enrich(baseSnapshot, seleniumDiscoveryResult),
                    seleniumDiscoveryResult
            );
        } catch (Exception exception) {
            System.err.println("Selenium discovery skipped, falling back to rule-based snapshot: "
                    + exception.getMessage());
            return baseResult;
        }
    }
}
