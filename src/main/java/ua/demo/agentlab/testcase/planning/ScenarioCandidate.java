package ua.demo.agentlab.testcase.planning;

import java.util.List;

public record ScenarioCandidate(
        RequirementUnit primaryRequirement,
        List<String> supportingRequirementIds,
        String sourcePageName,
        String sourceRoute,
        String targetPageName,
        String targetRoute,
        List<ScenarioStepCandidate> steps,
        List<ScenarioAssertionCandidate> assertions,
        List<String> risks
) {
    public ScenarioCandidate {
        supportingRequirementIds = supportingRequirementIds == null ? List.of() : List.copyOf(supportingRequirementIds);
        sourcePageName = sourcePageName == null ? "" : sourcePageName.trim();
        sourceRoute = sourceRoute == null ? "" : sourceRoute.trim();
        targetPageName = targetPageName == null ? "" : targetPageName.trim();
        targetRoute = targetRoute == null ? "" : targetRoute.trim();
        steps = steps == null ? List.of() : List.copyOf(steps);
        assertions = assertions == null ? List.of() : List.copyOf(assertions);
        risks = risks == null ? List.of() : List.copyOf(risks);
    }
}
