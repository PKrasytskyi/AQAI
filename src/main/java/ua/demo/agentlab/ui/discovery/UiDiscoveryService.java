package ua.demo.agentlab.ui.discovery;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;

public interface UiDiscoveryService {

    UiDiscoveryResult discover(ProjectProfile projectProfile, NormalizedRequirementBundle requirementBundle);
}
