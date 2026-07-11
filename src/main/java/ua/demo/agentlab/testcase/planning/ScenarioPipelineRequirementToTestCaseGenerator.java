package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseGenerator;
import ua.demo.agentlab.testcase.generator.RequirementToTestCaseInput;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ScenarioPipelineRequirementToTestCaseGenerator implements RequirementToTestCaseGenerator {

    @Override
    public CanonicalTestCaseBundle generate(RequirementToTestCaseInput input) {
        if (input == null || input.normalizedRequirementBundle() == null) {
            return new CanonicalTestCaseBundle("unknown-source", "NoPages", List.of(), List.of());
        }
        ScenarioPageResolver pageResolver = new ScenarioPageResolver(
                input.projectProfile(),
                input.mappedUiKnowledge(),
                input.normalizedRequirementBundle()
        );
        RequirementUnitClassifier classifier = new RequirementUnitClassifier(pageResolver);
        List<RequirementUnit> units = input.normalizedRequirementBundle().requirements().stream()
                .map(classifier::classify)
                .toList();
        ExpectedResultContractCatalog expectedResults = new ExpectedResultContractCatalogBuilder().build(units);
        ScenarioCandidateBuilder candidateBuilder = new ScenarioCandidateBuilder(pageResolver, expectedResults);
        AtomicScenarioPlanner planner = new AtomicScenarioPlanner();
        ScenarioQualityGate qualityGate = new ScenarioQualityGate();
        CanonicalTestCaseCompiler compiler = new CanonicalTestCaseCompiler();

        List<CanonicalTestCase> testCases = units.stream()
                .filter(this::isExecutable)
                .map(candidateBuilder::build)
                .map(planner::plan)
                .map(candidate -> compiler.compile(candidate, qualityGate.validate(candidate)))
                .toList();
        List<CanonicalTestCase> deduplicated = deduplicate(testCases);
        List<String> pageNames = pageNames(deduplicated);
        return new CanonicalTestCaseBundle(
                input.normalizedRequirementBundle().source(),
                pageNames.isEmpty() ? "NoPages" : pageNames.get(0),
                pageNames,
                deduplicated
        );
    }

    private boolean isExecutable(RequirementUnit unit) {
        if (unit == null || unit.requirement() == null || !unit.requirement().uiRelevant()) {
            return false;
        }
        return unit.type() == RequirementUnitType.FUNCTIONAL
                || unit.type() == RequirementUnitType.ASSERTION
                || unit.type() == RequirementUnitType.ROUTE_EXPECTATION;
    }

    private List<CanonicalTestCase> deduplicate(List<CanonicalTestCase> testCases) {
        return testCases.stream()
                .collect(java.util.stream.Collectors.toMap(
                        CanonicalTestCase::id,
                        testCase -> testCase,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .toList();
    }

    private List<String> pageNames(List<CanonicalTestCase> testCases) {
        Set<String> pages = new LinkedHashSet<>();
        for (CanonicalTestCase testCase : testCases) {
            pages.addAll(testCase.targetPages());
        }
        return pages.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
    }
}
