package ua.demo.agentlab.policy.provider;

import ua.demo.agentlab.policy.model.GenerationPolicy;

public interface GenerationPolicyProvider {
    String name();
    String defaultPolicyId();
    boolean supports(String policyId);
    GenerationPolicy provide(String policyId);
}
