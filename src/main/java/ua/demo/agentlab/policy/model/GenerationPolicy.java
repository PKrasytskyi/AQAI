package ua.demo.agentlab.policy.model;

import java.util.Objects;

public record GenerationPolicy(
        String policyId,
        String description,
        SelectorPolicy selectorPolicy,
        FrameworkPolicy frameworkPolicy,
        NamingPolicy namingPolicy,
        boolean requireReviewGate,
        boolean requireCompileGate,
        boolean allowAiSuggestions,
        boolean requireHumanApprovalForCodeChange
) {

    public GenerationPolicy {
        if(policyId == null || policyId.isBlank()){
            throw new IllegalArgumentException("policyId cannot be blank");
        }

        if(description == null || description.isBlank()){
            throw new IllegalArgumentException("description cannot be blank");
        }

        selectorPolicy = Objects.requireNonNull(selectorPolicy, "selectorPolicy");
        frameworkPolicy = Objects.requireNonNull(frameworkPolicy, "frameworkPolicy");
        namingPolicy = Objects.requireNonNull(namingPolicy, "namingPolicy");
    }
}
