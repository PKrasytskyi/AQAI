package ua.demo.agentlab.ui.discovery.classification;

import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;

import java.util.List;

public interface PageClassificationService {

    PageClassificationResult classifyPage(DiscoveredPageSnapshot snapshot, List<DiscoveredUiPage> knownPages);

    String inferFlowType(
            DiscoveredTransition transition,
            DiscoveredPageSnapshot sourcePage,
            DiscoveredPageSnapshot targetPage,
            String targetPageName
    );
}
