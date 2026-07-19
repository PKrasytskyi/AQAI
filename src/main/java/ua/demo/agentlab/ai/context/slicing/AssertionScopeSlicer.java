package ua.demo.agentlab.ai.context.slicing;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.context.AiContextScope;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Restricts assertions to the test cases selected by the current scope. */
public final class AssertionScopeSlicer {

    public List<AssertionContract> slice(List<AssertionContract> source, AiContextScope scope,
                                         CanonicalTestCaseBundle testCases) {
        if (source == null || source.isEmpty()) return List.of();
        Set<String> ids = testCases == null ? Set.of() : testCases.testCases().stream()
                .map(CanonicalTestCase::id).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return source.stream().filter(contract -> ids.contains(contract.testCaseId())
                || scope.targetRequirementIds().contains(contract.requirementId())).toList();
    }
}
