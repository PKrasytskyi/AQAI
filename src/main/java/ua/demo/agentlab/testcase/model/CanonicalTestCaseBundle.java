package ua.demo.agentlab.testcase.model;

import java.util.List;

public record CanonicalTestCaseBundle(
        String source,
        String primaryPage,
        List<String> pageNames,
        List<CanonicalTestCase> testCases
) {
    public CanonicalTestCaseBundle {
        source = source == null ? "" : source.trim();
        primaryPage = primaryPage == null || primaryPage.isBlank() ? "NoPages" : primaryPage.trim();
        pageNames = pageNames == null ? List.of() : List.copyOf(pageNames);
        testCases = testCases == null ? List.of() : List.copyOf(testCases);
    }
}
