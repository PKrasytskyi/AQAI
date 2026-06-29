package ua.demo.agentlab.policy.provider;

import ua.demo.agentlab.policy.model.FrameworkPolicy;
import ua.demo.agentlab.policy.model.GenerationPolicy;
import ua.demo.agentlab.policy.model.NamingPolicy;
import ua.demo.agentlab.policy.model.SelectorPolicy;

import java.util.List;

public class DefaultGenerationPolicyProvider implements GenerationPolicyProvider{

    private static final String DEFAULT_POLICY_ID = "default-selenium-testng";

    @Override
    public String name() {
        return "default-generation-policy-provider";
    }

    @Override
    public String defaultPolicyId() {
        return DEFAULT_POLICY_ID;
    }

    @Override
    public boolean supports(String policyId) {
        return (DEFAULT_POLICY_ID.equalsIgnoreCase(policyId)) || "default".equalsIgnoreCase(policyId);
    }

    @Override
    public GenerationPolicy provide(String policyId) {
        if(!supports(policyId)){
            throw new IllegalArgumentException("Unsupported policy id: " + policyId);
        }

        return new GenerationPolicy(
                DEFAULT_POLICY_ID,
                "Default policy for Selenium UI generation and Rest Assured API generation",
                new SelectorPolicy(
                        List.of(
                                SelectorPolicy.SelectorStrategy.DATA_TESTID,
                                SelectorPolicy.SelectorStrategy.ID,
                                SelectorPolicy.SelectorStrategy.NAME,
                                SelectorPolicy.SelectorStrategy.CSS,
                                SelectorPolicy.SelectorStrategy.LABEL,
                                SelectorPolicy.SelectorStrategy.TEXT
                        ),
                        false,
                        true,
                        true,
                        true,
                        List.of("data-testid", "data-test", "qa-id", "test-id")
                ),
                new FrameworkPolicy(
                        FrameworkPolicy.UiFramework.SELENIUM_JAVA,
                        FrameworkPolicy.ApiFramework.REST_ASSURED,
                        FrameworkPolicy.TestStyle.TESTNG,
                        true,
                        true,
                        true,
                        true
                ),
                new NamingPolicy(
                        "Page",
                        "Test",
                        "ApiTest",
                        "Element",
                        "valid",
                        "invalid",
                        true,
                        NamingPolicy.CaseStyle.PASCAL_CASE,
                        NamingPolicy.CaseStyle.CAMEL_CASE
                ),
                true,
                true,
                true,
                true
        );
    }
}
