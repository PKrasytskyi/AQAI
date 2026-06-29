package ua.demo.agentlab.futurefeat.testplan.model;

import java.util.List;

public record TestPlan(
        String objective,
        String source,
        List<FunctionalArea> functionalAreas,
        List<TestScenario> scenarios,
        List<String> assumptions,
        List<String> risks
) {
}
