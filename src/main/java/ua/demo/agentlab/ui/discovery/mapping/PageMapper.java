package ua.demo.agentlab.ui.discovery.mapping;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public interface PageMapper {

    MappedUiKnowledge map(
            UiDiscoverySnapshot snapshot,
            SeleniumDiscoveryResult seleniumDiscoveryResult,
            PageModelBundle pageModelBundle
    );
}
