package ua.demo.agentlab.futurefeat.testplan.model;

import java.util.List;

public record TestScenario(
        String id,
        String title,
        TestType type,
        TestPriority priority,
        String precondition,
        List<String> steps,
        String expectedResult,
        String sourceReference
) {
}
