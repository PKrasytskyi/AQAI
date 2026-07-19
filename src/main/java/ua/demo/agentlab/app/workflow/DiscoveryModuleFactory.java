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
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.PropertiesSpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventoryArtifactWriter;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventoryBuilder;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphBuilder;
import ua.demo.agentlab.ui.discovery.spa.ComponentInteractionGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetedVerificationArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.LiveTargetedVerificationRunner;
import ua.demo.agentlab.ui.discovery.spa.SpaEvidenceRetentionGraphWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaTargetedVerificationArtifactWriter;
import ua.demo.agentlab.ui.discovery.spa.SpaTargetedVerificationPlanner;
import ua.demo.agentlab.ui.discovery.interaction.inventory.agent.UiInteractionInventoryAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaComponentInteractionGraphAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiLiveSpaTargetedVerificationAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaEvidenceRetentionAgent;
import ua.demo.agentlab.ui.discovery.spa.agent.UiSpaTargetedVerificationAgent;
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
                new UiPageMappingAgent(new RuleBasedPageMapper()),
                new UiInteractionInventoryAgent(
                        new PropertiesSpaInventoryConfig(),
                        new UiInteractionInventoryBuilder(),
                        new UiInteractionInventoryArtifactWriter()
                ),
                new UiSpaComponentInteractionGraphAgent(
                        new ComponentInteractionGraphBuilder(),
                        new ComponentInteractionGraphWriter(new PropertiesNeo4jRuntimeConfig()),
                        new ComponentInteractionGraphArtifactWriter()
                ),
                new UiSpaTargetedVerificationAgent(
                        new PropertiesSpaInventoryConfig(),
                        new SpaTargetedVerificationPlanner(),
                        new SpaTargetedVerificationArtifactWriter()
                ),
                new UiLiveSpaTargetedVerificationAgent(
                        new PropertiesSpaInventoryConfig(),
                        new LiveTargetedVerificationRunner(),
                        new LiveTargetedVerificationArtifactWriter()
                ),
                new UiSpaEvidenceRetentionAgent(
                        new PropertiesSpaInventoryConfig(),
                        new SpaEvidenceRetentionGraphWriter(new PropertiesNeo4jRuntimeConfig())
                )
        );
    }
}
