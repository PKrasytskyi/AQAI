package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public record UiPageMappingInput(
        ProjectProfile projectProfile,
        NormalizedRequirementBundle normalizedRequirementBundle,
        UiDiscoverySnapshot discoverySnapshot,
        SeleniumDiscoveryResult seleniumDiscoveryResult,
        PageModelBundle pageModelBundle
) {
}
