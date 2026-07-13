package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.context.RuleBasedCanonicalInteractionLayer;
import ua.demo.agentlab.ai.flow.BusinessFlowResolver;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeAgent;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeService;
import ua.demo.agentlab.artifactreuse.config.PropertiesArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBuilder;
import ua.demo.agentlab.artifactreuse.flow.FlowContractBuilderAgent;
import ua.demo.agentlab.artifactreuse.flow.FlowContractPersistenceAgent;
import ua.demo.agentlab.artifactreuse.flow.FlowRuntimeFeedbackAgent;
import ua.demo.agentlab.artifactreuse.flow.Neo4jFlowContractRegistry;
import ua.demo.agentlab.artifactreuse.planner.FlowSemanticCandidateAgent;
import ua.demo.agentlab.artifactreuse.planner.ReusePlanner;
import ua.demo.agentlab.artifactreuse.planner.ReusePlannerAgent;
import ua.demo.agentlab.artifactreuse.semantic.FlowSemanticCandidateService;
import ua.demo.agentlab.artifactreuse.semantic.FlowSemanticIndexAgent;
import ua.demo.agentlab.artifactreuse.semantic.FlowSemanticIndexer;
import ua.demo.agentlab.ui.discovery.agent.UiPageKnowledgePersistenceAgent;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.GraphPageKnowledgeWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.QdrantPageKnowledgeWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesKnowledgeVectorRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.util.List;

public class KnowledgeStoreModuleFactory {

    public KnowledgeStoreModule create() {
        RuleBasedCanonicalInteractionLayer canonicalInteractionLayer = new RuleBasedCanonicalInteractionLayer();
        PropertiesArtifactReuseRuntimeConfig artifactReuseConfig = new PropertiesArtifactReuseRuntimeConfig();
        PropertiesKnowledgeVectorRuntimeConfig vectorConfig = new PropertiesKnowledgeVectorRuntimeConfig();
        PropertiesNeo4jRuntimeConfig neo4jConfig = new PropertiesNeo4jRuntimeConfig();
        Neo4jFlowContractRegistry flowRegistry = new Neo4jFlowContractRegistry(neo4jConfig);
        FlowSemanticIndexer flowSemanticIndexer = new FlowSemanticIndexer(artifactReuseConfig, vectorConfig);
        return new KnowledgeStoreModule(
                canonicalInteractionLayer,
                new UiPageKnowledgePersistenceAgent(List.of(
                        new GraphPageKnowledgeWriter(new PropertiesNeo4jRuntimeConfig()),
                        new QdrantPageKnowledgeWriter(new PropertiesKnowledgeVectorRuntimeConfig())
                )),
                new FlowScopedKnowledgeAgent(
                        new FlowScopedKnowledgeService(
                                new BusinessFlowResolver(),
                                canonicalInteractionLayer,
                                null
                        )
                ),
                new FlowContractBuilderAgent(new FlowContractBuilder()),
                new FlowContractPersistenceAgent(
                        artifactReuseConfig,
                        flowRegistry
                ),
                new FlowRuntimeFeedbackAgent(artifactReuseConfig, flowRegistry, flowSemanticIndexer),
                new FlowSemanticIndexAgent(flowSemanticIndexer),
                new FlowSemanticCandidateAgent(new FlowSemanticCandidateService(artifactReuseConfig, vectorConfig, flowRegistry)),
                new ReusePlannerAgent(new ReusePlanner(), artifactReuseConfig)
        );
    }
}
