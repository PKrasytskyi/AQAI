package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.review.RuleBasedGeneratedCodeReviewer;
import ua.demo.agentlab.review.agent.GeneratedCodeReviewAgent;
import ua.demo.agentlab.artifactreuse.agent.ArtifactLifecyclePromotionAgent;
import ua.demo.agentlab.artifactreuse.agent.ArtifactReuseMetricsAgent;
import ua.demo.agentlab.artifactreuse.agent.RunHistoryStatisticsAgent;
import ua.demo.agentlab.artifactreuse.config.PropertiesArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.lifecycle.ArtifactLifecyclePromotionService;
import ua.demo.agentlab.artifactreuse.registry.neo4j.Neo4jArtifactRegistry;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;
import ua.demo.agentlab.validation.GeneratedCodeValidator;
import ua.demo.agentlab.validation.MavenGeneratedCodeValidator;
import ua.demo.agentlab.validation.SimpleGeneratedUiContractValidator;
import ua.demo.agentlab.validation.agent.GeneratedCodeCompileAgent;
import ua.demo.agentlab.validation.agent.GeneratedUiContractValidationAgent;
import ua.demo.agentlab.validation.agent.GeneratedUiSmokeAgent;
import ua.demo.agentlab.validation.agent.RuntimeFeedbackDbUpdateAgent;
import ua.demo.agentlab.validation.feedback.GeneratedUiRuntimeFeedbackWriter;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeService;

import java.nio.file.Path;

public class ValidationModuleFactory {

    public ValidationModule create() {
        GeneratedCodeValidator generatedCodeValidator =
                new MavenGeneratedCodeValidator(Path.of("").toAbsolutePath().normalize().toString());
        return new ValidationModule(
                generatedCodeValidator,
                new GeneratedUiContractValidationAgent(new SimpleGeneratedUiContractValidator()),
                new GeneratedCodeCompileAgent(generatedCodeValidator),
                new GeneratedCodeReviewAgent(new RuleBasedGeneratedCodeReviewer()),
                new GeneratedUiSmokeAgent(new GeneratedUiSmokeService()),
                new ArtifactLifecyclePromotionAgent(new ArtifactLifecyclePromotionService(
                        new PropertiesArtifactReuseRuntimeConfig(),
                        new Neo4jArtifactRegistry(new PropertiesNeo4jRuntimeConfig())
                )),
                new ArtifactReuseMetricsAgent(),
                new RuntimeFeedbackDbUpdateAgent(new GeneratedUiRuntimeFeedbackWriter(new PropertiesNeo4jRuntimeConfig())),
                new RunHistoryStatisticsAgent()
        );
    }
}
