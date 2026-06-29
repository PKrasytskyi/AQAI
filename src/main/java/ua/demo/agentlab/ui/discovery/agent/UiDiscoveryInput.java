package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

public record UiDiscoveryInput(
        ProjectProfile projectProfile,
        NormalizedRequirementBundle normalizedRequirementBundle
) {
}
