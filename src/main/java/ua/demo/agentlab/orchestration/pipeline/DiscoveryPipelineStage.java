package ua.demo.agentlab.orchestration.pipeline;

import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

public record DiscoveryPipelineStage(
        UiDiscoverySnapshot uiDiscoverySnapshot,
        SeleniumDiscoveryResult seleniumDiscoveryResult,
        PageModelBundle pageModelBundle,
        CanonicalPageFlowModel canonicalPageFlowModel
) {
}
