package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.contract.rule.PomCapabilityRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomBusinessIntentRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomContractRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomContractValidationContext;
import ua.demo.agentlab.ai.ui.contract.rule.PomExpectedValueRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomFallbackSelectorRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomForbiddenMethodRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomLocatorProvenanceRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomNoCrossPageActionRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomNoFallbackLocatorRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomNoRequirementSentenceAssertionRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomOwnershipRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomPageOwnershipRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomProtectedPageFlowRule;
import ua.demo.agentlab.ai.ui.contract.rule.PomRouteRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PomContractQualityGate {

    private static final Set<String> LOCATOR_STRATEGIES = Set.of("id", "name", "css", "xpath", "partialLinkText");
    private static final Set<String> ALLOWED_RETURN_TYPES = Set.of("boolean", "String", "List<String>");
    private final List<PomContractRule> rules = List.of(
            new PomRouteRule(),
            new PomCapabilityRule(),
            new PomPageOwnershipRule(),
            new PomOwnershipRule(),
            new PomLocatorProvenanceRule(),
            new PomNoFallbackLocatorRule(),
            new PomFallbackSelectorRule(),
            new PomExpectedValueRule(),
            new PomForbiddenMethodRule(),
            new PomNoCrossPageActionRule(),
            new PomNoRequirementSentenceAssertionRule(),
            new PomProtectedPageFlowRule(),
            new PomBusinessIntentRule()
    );

    public PomContractQualityReport validate(PomContractSpec spec) {
        List<PomContractIssue> issues = new ArrayList<>();
        if (spec == null) {
            issues.add(blocker("POM_CONTRACT_PRESENT", "POM contract must be present", "null"));
            return new PomContractQualityReport(LlmOutputSchemaVersion.POM_CONTRACT, "", issues);
        }
        if (!LlmOutputSchemaVersion.POM_CONTRACT.equals(spec.schemaVersion())) {
            issues.add(blocker("POM_CONTRACT_SCHEMA_VERSION",
                    "POM contract schema version must match",
                    spec.schemaVersion()));
        }
        PomContractValidationContext context = new PomContractValidationContext(LOCATOR_STRATEGIES, ALLOWED_RETURN_TYPES);
        for (PomContractRule rule : rules) {
            rule.validate(spec, context, issues);
        }
        return new PomContractQualityReport(spec.schemaVersion(), spec.page().name(), issues);
    }

    private PomContractIssue blocker(String ruleId, String message, String evidence) {
        return new PomContractIssue(PomContractSeverity.BLOCKER, ruleId, message, evidence);
    }
}
