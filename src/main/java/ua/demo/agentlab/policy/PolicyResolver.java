package ua.demo.agentlab.policy;

import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.policy.provider.GenerationPolicyProvider;

import java.util.List;

public class PolicyResolver {

    private final List<GenerationPolicyProvider> providers;

    public PolicyResolver(List<GenerationPolicyProvider> providers){
        this.providers = List.copyOf(providers);

        if(this.providers.isEmpty()) {
            throw new IllegalArgumentException("At least one GenerationPolicyProvider is required");
        }
    }

    public GenerationPolicy resolveDefault() {
        GenerationPolicyProvider provider = providers.get(0);
        return provider.provide(provider.defaultPolicyId());
    }

    public GenerationPolicy resolve(String policyId){
        return providers.stream()
                .filter(provider -> provider.supports(policyId))
                .findFirst()
                .orElseThrow(()-> new IllegalStateException(" No policy found for id " + policyId))
                .provide(policyId);
    }
}
