package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.policy.PolicyLoadingAgent;
import ua.demo.agentlab.policy.PolicyResolver;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.policy.provider.DefaultGenerationPolicyProvider;
import ua.demo.agentlab.requirements.agent.RequirementReaderAgent;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.normalization.agent.RequirementNormalizationAgent;
import ua.demo.agentlab.requirements.source.FileRequirementSource;
import ua.demo.agentlab.requirements.source.RequirementSource;
import ua.demo.agentlab.requirements.source.UrlRequirementSource;

import java.util.List;

public class RequirementPolicyModuleFactory {

    public RequirementPolicyModule create() {
        DefaultGenerationPolicyProvider policyProvider = new DefaultGenerationPolicyProvider();
        GenerationPolicy defaultPolicy = policyProvider.provide(policyProvider.defaultPolicyId());
        List<RequirementSource> requirementSources = List.of(new FileRequirementSource(), new UrlRequirementSource());
        return new RequirementPolicyModule(
                policyProvider,
                defaultPolicy,
                new RequirementReaderAgent(requirementSources),
                new RequirementNormalizationAgent(new RuleBasedRequirementNormalizer()),
                new PolicyLoadingAgent(new PolicyResolver(List.of(policyProvider)))
        );
    }
}
