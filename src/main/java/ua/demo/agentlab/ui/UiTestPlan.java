package ua.demo.agentlab.ui;

import java.util.List;

public record UiTestPlan(

        String sourceTestPlan,
        String targetPage,
        List<String> pageNames,
        List<UiTestScenario> scenarios
) {
}
