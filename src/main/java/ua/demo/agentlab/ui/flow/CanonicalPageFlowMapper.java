package ua.demo.agentlab.ui.flow;

import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;

public interface CanonicalPageFlowMapper {

    CanonicalPageFlowModel map(UiDiscoverySnapshot discoverySnapshot);
}
