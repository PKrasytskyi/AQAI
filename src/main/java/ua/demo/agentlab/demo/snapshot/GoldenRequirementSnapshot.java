package ua.demo.agentlab.demo.snapshot;

import java.util.List;
import java.util.Map;

/** Stable, run-independent projection of the requirement-to-scenario pipeline. */
public record GoldenRequirementSnapshot(
        String schemaVersion,
        String source,
        NormalizedBundleSnapshot normalizedRequirementBundle,
        List<BehaviorContractSnapshot> structuredBehaviorContracts,
        CanonicalBundleSnapshot canonicalTestCaseBundle,
        GovernanceBundleSnapshot governanceRequirementBundle
) {
    public static final String SCHEMA_VERSION = "golden-requirement-snapshot.v1";

    public record NormalizedBundleSnapshot(
            String schemaVersion,
            List<NormalizedRequirementSnapshot> requirements,
            List<String> assumptions,
            List<String> risks
    ) {
    }

    public record NormalizedRequirementSnapshot(
            String id,
            String title,
            String capability,
            String expectedResult,
            List<String> actions,
            PageContextSnapshot sourceContext,
            PageContextSnapshot targetContext,
            List<TypedAssertionSnapshot> assertions,
            Map<String, String> dataRequirements,
            List<String> tags,
            boolean executable
    ) {
    }

    public record BehaviorContractSnapshot(
            String requirementId,
            String capability,
            List<String> actions,
            PageContextSnapshot sourceContext,
            PageContextSnapshot targetContext,
            List<TypedAssertionSnapshot> assertions,
            Map<String, String> dataRequirements,
            boolean executable,
            List<String> reviewReasons
    ) {
    }

    public record CanonicalBundleSnapshot(
            String schemaVersion,
            String primaryPage,
            List<String> pageNames,
            List<CanonicalScenarioSnapshot> scenarios
    ) {
    }

    public record CanonicalScenarioSnapshot(
            String id,
            String title,
            String flowType,
            List<String> requirementRefs,
            PageContextSnapshot sourceContext,
            PageContextSnapshot targetContext,
            String precondition,
            List<OperationSnapshot> operations,
            List<ScenarioAssertionSnapshot> assertions,
            List<String> actions,
            List<String> expectedResults
    ) {
    }

    public record GovernanceBundleSnapshot(
            String schemaVersion,
            int totalRequirements,
            int executableRequirements,
            List<GovernanceRequirementSnapshot> requirements
    ) {
    }

    public record GovernanceRequirementSnapshot(
            String requirementId,
            String category,
            String governanceTarget,
            String statement,
            List<String> tags
    ) {
    }

    public record PageContextSnapshot(String page, String route) {
    }

    public record TypedAssertionSnapshot(String type, String target, String expectedValue) {
    }

    public record OperationSnapshot(String kind, String ownerPage, String route, String dataKey, boolean setup) {
    }

    public record ScenarioAssertionSnapshot(
            String kind,
            String target,
            String expectedValue,
            String ownerPage,
            String route
    ) {
    }
}
