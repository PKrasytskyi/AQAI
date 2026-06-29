package ua.demo.agentlab.ui.discovery.persistence;

import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.List;

public interface DiscoveryArtifactWriter {

    List<String> write(
            UiDiscoverySnapshot snapshot,
            SeleniumDiscoveryResult seleniumDiscoveryResult,
            MappedUiKnowledge mappedUiKnowledge
    );
}
