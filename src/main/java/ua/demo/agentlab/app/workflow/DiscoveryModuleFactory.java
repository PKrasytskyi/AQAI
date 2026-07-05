package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.CompositeUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.RuleBasedUiDiscoveryService;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryAgent;
import ua.demo.agentlab.ui.discovery.agent.UiDiscoveryArtifactPersistenceAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageMappingAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageModelAgent;
import ua.demo.agentlab.ui.discovery.agent.UiRuntimeEvidenceAgent;
import ua.demo.agentlab.ui.discovery.classification.RuleBasedPageClassificationService;
import ua.demo.agentlab.ui.discovery.enrichment.UiDiscoveryEnricher;
import ua.demo.agentlab.ui.discovery.mapping.RuleBasedPageMapper;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.persistence.LocalDiscoveryArtifactWriter;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidenceArtifactWriter;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidenceCollectorFactory;
import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiDiscoveryConfig;
import ua.demo.agentlab.ui.discovery.runtime.bidi.BiDiEventBuffer;
import ua.demo.agentlab.ui.flow.RuleBasedCanonicalPageFlowMapper;

public class DiscoveryModuleFactory {

    private final SeleniumDiscoveryServiceFactory seleniumDiscoveryServiceFactory;

    public DiscoveryModuleFactory() {
        this(new SeleniumDiscoveryServiceFactory());
    }

    DiscoveryModuleFactory(SeleniumDiscoveryServiceFactory seleniumDiscoveryServiceFactory) {
        if (seleniumDiscoveryServiceFactory == null) {
            throw new IllegalArgumentException("selenium discovery service factory cannot be null");
        }
        this.seleniumDiscoveryServiceFactory = seleniumDiscoveryServiceFactory;
    }

    public DiscoveryModule create(ProjectProfile projectProfile) {
        if (projectProfile == null) {
            throw new IllegalArgumentException("projectProfile cannot be null");
        }
        BiDiDiscoveryConfig biDiConfig = BiDiDiscoveryConfig.fromRuntime();
        BiDiEventBuffer biDiEventBuffer = new BiDiEventBuffer(biDiConfig.maxBufferedEvents());
        return new DiscoveryModule(
                new UiDiscoveryAgent(
                        new CompositeUiDiscoveryService(
                                new RuleBasedUiDiscoveryService(),
                                seleniumDiscoveryServiceFactory.create(projectProfile, biDiEventBuffer),
                                new UiDiscoveryEnricher(new RuleBasedPageClassificationService())
                        ),
                        new RuleBasedCanonicalPageFlowMapper()
                ),
                new UiRuntimeEvidenceAgent(
                        RuntimeEvidenceCollectorFactory.fromRuntime(biDiEventBuffer),
                        new RuntimeEvidenceArtifactWriter()
                ),
                new UiDiscoveryArtifactPersistenceAgent(new LocalDiscoveryArtifactWriter()),
                new UiPageModelAgent(new PageModelBuilder(), new PageModelArtifactWriter()),
                new UiPageMappingAgent(new RuleBasedPageMapper())
        );
    }
}
