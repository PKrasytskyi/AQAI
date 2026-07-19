package ua.demo.agentlab.demo.snapshot;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContractBuilder;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.RuleBasedRequirementNormalizer;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceBundle;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceItem;
import ua.demo.agentlab.testcase.governance.RequirementGovernancePartitioner;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.testcase.planning.ScenarioPipelineRequirementToTestCaseGenerator;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.UiOperationIntent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.TreeMap;

import static ua.demo.agentlab.demo.snapshot.GoldenRequirementSnapshot.*;

/** Builds the BW-02 snapshot from the same deterministic services used by the workflow. */
public final class GoldenRequirementSnapshotBuilder {

    public GoldenRequirementSnapshot build(RequirementDocument document, ProjectProfile profile) {
        if (document == null) {
            throw new IllegalArgumentException("RequirementDocument is required");
        }
        if (profile == null) {
            throw new IllegalArgumentException("ProjectProfile is required");
        }

        NormalizedRequirementBundle normalized = new RuleBasedRequirementNormalizer().normalize(document);
        List<StructuredBehaviorContract> contracts = new StructuredBehaviorContractBuilder()
                .build(normalized.requirements());
        RequirementGovernanceBundle governance = new RequirementGovernancePartitioner().partition(normalized);
        CanonicalTestCaseBundle canonical = ScenarioPipelineRequirementToTestCaseGenerator.deterministic().generate(
                new RequirementToTestCaseInput(profile, normalized, null, null)
        );

        Map<String, StructuredBehaviorContract> contractsByRequirement = indexContracts(contracts);
        Map<String, CanonicalTestCase> scenariosByRequirement = indexScenarios(canonical.testCases());
        Map<String, RequirementGovernanceItem> governanceByRequirement = indexGovernance(governance.items());

        return new GoldenRequirementSnapshot(
                GoldenRequirementSnapshot.SCHEMA_VERSION,
                normalizePath(document.source()),
                normalizedBundle(normalized, contractsByRequirement, scenariosByRequirement, governanceByRequirement),
                behaviorContracts(contracts, scenariosByRequirement),
                canonicalBundle(canonical, profile),
                governanceBundle(governance)
        );
    }

    private NormalizedBundleSnapshot normalizedBundle(
            NormalizedRequirementBundle bundle,
            Map<String, StructuredBehaviorContract> contracts,
            Map<String, CanonicalTestCase> scenarios,
            Map<String, RequirementGovernanceItem> governance
    ) {
        List<NormalizedRequirementSnapshot> requirements = bundle.requirements().stream()
                .sorted(Comparator.comparing(NormalizedRequirement::id))
                .map(requirement -> normalizedRequirement(
                        requirement,
                        contracts.get(requirement.id()),
                        scenarios.get(requirement.id()),
                        governance.get(requirement.id())
                ))
                .toList();
        return new NormalizedBundleSnapshot(
                "normalized-requirement-bundle.v1",
                requirements,
                sorted(bundle.assumptions()),
                sorted(bundle.risks())
        );
    }

    private NormalizedRequirementSnapshot normalizedRequirement(
            NormalizedRequirement requirement,
            StructuredBehaviorContract contract,
            CanonicalTestCase scenario,
            RequirementGovernanceItem governance
    ) {
        return new NormalizedRequirementSnapshot(
                safe(requirement.id()),
                safe(requirement.title()),
                capability(requirement),
                safe(requirement.expectedResult()),
                contract == null ? List.of() : cleanOrdered(contract.actions()),
                sourceContext(scenario),
                targetContext(scenario),
                typedAssertions(requirement.structuredAssertions()),
                contract == null ? Map.of() : sortedMap(contract.dataRequirements()),
                sorted(requirement.tags()),
                governance != null && governance.testCaseEligible()
        );
    }

    private List<BehaviorContractSnapshot> behaviorContracts(
            List<StructuredBehaviorContract> contracts,
            Map<String, CanonicalTestCase> scenarios
    ) {
        return contracts.stream()
                .sorted(Comparator.comparing(StructuredBehaviorContract::requirementId))
                .map(contract -> new BehaviorContractSnapshot(
                        contract.requirementId(),
                        contract.capability().toUpperCase(Locale.ROOT),
                        cleanOrdered(contract.actions()),
                        sourceContext(scenarios.get(contract.requirementId())),
                        targetContext(scenarios.get(contract.requirementId())),
                        typedAssertions(contract.assertions()),
                        sortedMap(contract.dataRequirements()),
                        contract.executable(),
                        sorted(contract.reviewReasons())
                ))
                .toList();
    }

    private CanonicalBundleSnapshot canonicalBundle(CanonicalTestCaseBundle bundle, ProjectProfile profile) {
        List<CanonicalScenarioSnapshot> scenarios = bundle.testCases().stream()
                .sorted(Comparator.comparing(CanonicalTestCase::id))
                .map(scenario -> canonicalScenario(scenario, profile))
                .toList();
        return new CanonicalBundleSnapshot(
                "canonical-test-case-bundle.v1",
                safe(bundle.primaryPage()),
                sorted(bundle.pageNames()),
                scenarios
        );
    }

    private CanonicalScenarioSnapshot canonicalScenario(CanonicalTestCase scenario, ProjectProfile profile) {
        return new CanonicalScenarioSnapshot(
                safe(scenario.id()),
                safe(scenario.title()),
                safe(scenario.canonicalFlowType()),
                sorted(scenario.requirementRefs()),
                sourceContext(scenario),
                targetContext(scenario),
                safe(scenario.precondition()),
                operations(scenario, profile),
                assertions(scenario),
                cleanOrdered(scenario.actions()),
                sorted(scenario.assertions())
        );
    }

    private GovernanceBundleSnapshot governanceBundle(RequirementGovernanceBundle bundle) {
        List<GovernanceRequirementSnapshot> requirements = bundle.governanceRequirements().stream()
                .sorted(Comparator.comparing(RequirementGovernanceItem::requirementId))
                .map(item -> new GovernanceRequirementSnapshot(
                        item.requirementId(),
                        item.category().name(),
                        item.governanceTarget(),
                        item.statement(),
                        sorted(item.tags())
                ))
                .toList();
        return new GovernanceBundleSnapshot(
                "requirement-governance-bundle.v1",
                bundle.totalRequirements(),
                bundle.canonicalTestCaseRequirements(),
                requirements
        );
    }

    private List<OperationSnapshot> operations(CanonicalTestCase scenario, ProjectProfile profile) {
        List<OperationSnapshot> result = new ArrayList<>();
        UiScenarioPrerequisite prerequisite = scenario.prerequisiteFlow();
        for (int index = 0; index < scenario.operationIntents().size(); index++) {
            UiOperationIntent operation = scenario.operationIntents().get(index);
            boolean setup = prerequisite != null && prerequisite.authenticationRequired()
                    && index < prerequisite.setupActions().size();
            String ownerPage = safe(operation.target());
            result.add(new OperationSnapshot(
                    operation.kind().name(),
                    ownerPage,
                    routeForOwner(scenario, operation, ownerPage, profile),
                    safe(operation.dataKey()),
                    setup
            ));
        }
        return List.copyOf(result);
    }

    private List<ScenarioAssertionSnapshot> assertions(CanonicalTestCase scenario) {
        return scenario.assertionIntents().stream()
                .map(assertion -> scenarioAssertion(assertion, scenario))
                .sorted(Comparator.comparing(ScenarioAssertionSnapshot::kind)
                        .thenComparing(ScenarioAssertionSnapshot::target)
                        .thenComparing(ScenarioAssertionSnapshot::expectedValue))
                .toList();
    }

    private ScenarioAssertionSnapshot scenarioAssertion(AssertionIntent assertion, CanonicalTestCase scenario) {
        return new ScenarioAssertionSnapshot(
                assertion.kind().name(),
                safe(assertion.target()),
                safe(assertion.expectedValue()),
                safe(scenario.pageName()),
                safe(scenario.route())
        );
    }

    private List<TypedAssertionSnapshot> typedAssertions(List<StructuredAssertionRequirement> assertions) {
        if (assertions == null) {
            return List.of();
        }
        return assertions.stream()
                .map(assertion -> new TypedAssertionSnapshot(
                        safe(assertion.type()).toUpperCase(Locale.ROOT),
                        safe(assertion.target()),
                        safe(assertion.expectedValue())
                ))
                .sorted(Comparator.comparing(TypedAssertionSnapshot::type)
                        .thenComparing(TypedAssertionSnapshot::target)
                        .thenComparing(TypedAssertionSnapshot::expectedValue))
                .toList();
    }

    private PageContextSnapshot sourceContext(CanonicalTestCase scenario) {
        return scenario == null
                ? new PageContextSnapshot("", "")
                : new PageContextSnapshot(safe(scenario.sourcePageName()), safe(scenario.sourceRoute()));
    }

    private PageContextSnapshot targetContext(CanonicalTestCase scenario) {
        return scenario == null
                ? new PageContextSnapshot("", "")
                : new PageContextSnapshot(safe(scenario.pageName()), safe(scenario.route()));
    }

    private String routeForOwner(
            CanonicalTestCase scenario,
            UiOperationIntent operation,
            String ownerPage,
            ProjectProfile profile
    ) {
        UiScenarioPrerequisite prerequisite = scenario.prerequisiteFlow();
        if (prerequisite != null && safe(ownerPage).equals(safe(prerequisite.sourcePageName()))) {
            return safe(prerequisite.sourceRoute());
        }
        if (safe(ownerPage).equals(safe(scenario.sourcePageName()))) {
            return safe(scenario.sourceRoute());
        }
        if (safe(ownerPage).equals(safe(scenario.pageName()))) {
            return safe(scenario.route());
        }
        if (operation != null && operation.kind() == ua.demo.agentlab.ui.contract.UiOperationKind.AUTHENTICATE) {
            return safe(profile.loginRoute());
        }
        return "";
    }

    private String capability(NormalizedRequirement requirement) {
        return requirement.tags().stream()
                .filter(tag -> tag.startsWith("capability-"))
                .map(tag -> tag.substring("capability-".length()).replace('-', '_').toUpperCase(Locale.ROOT))
                .findFirst()
                .orElse("");
    }

    private Map<String, StructuredBehaviorContract> indexContracts(List<StructuredBehaviorContract> contracts) {
        Map<String, StructuredBehaviorContract> result = new LinkedHashMap<>();
        contracts.forEach(contract -> result.put(contract.requirementId(), contract));
        return Map.copyOf(result);
    }

    private Map<String, CanonicalTestCase> indexScenarios(List<CanonicalTestCase> scenarios) {
        Map<String, CanonicalTestCase> result = new LinkedHashMap<>();
        scenarios.forEach(scenario -> result.put(scenario.id(), scenario));
        return Map.copyOf(result);
    }

    private Map<String, RequirementGovernanceItem> indexGovernance(List<RequirementGovernanceItem> items) {
        Map<String, RequirementGovernanceItem> result = new LinkedHashMap<>();
        items.forEach(item -> result.put(item.requirementId(), item));
        return Map.copyOf(result);
    }

    private Map<String, String> sortedMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sorted = new TreeMap<>();
        values.forEach((key, value) -> sorted.put(safe(key), safe(value)));
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    private List<String> sorted(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(this::safe).filter(value -> !value.isBlank()).distinct().sorted().toList();
    }

    private List<String> cleanOrdered(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(this::safe).filter(value -> !value.isBlank()).distinct().toList();
    }

    private String normalizePath(String value) {
        return safe(value).replace('\\', '/');
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
