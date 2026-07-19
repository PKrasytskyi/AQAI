package ua.demo.agentlab.validation.execution;

import java.util.List;

public record GeneratedTestExecutionResult(
        String schemaVersion,
        GeneratedTestExecutionStatus status,
        int total,
        int passed,
        int failed,
        int skipped,
        long durationMillis,
        List<String> testClasses,
        List<GeneratedTestCaseExecutionResult> tests,
        List<String> failures,
        String reportDirectory,
        String summary,
        String processOutput
) {
    public static final String SCHEMA_VERSION = "generated-tests-execution-result.v1";

    public GeneratedTestExecutionResult {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        status = status == null ? GeneratedTestExecutionStatus.FAILED : status;
        total = Math.max(0, total);
        passed = Math.max(0, passed);
        failed = Math.max(0, failed);
        skipped = Math.max(0, skipped);
        durationMillis = Math.max(0L, durationMillis);
        testClasses = testClasses == null ? List.of() : List.copyOf(testClasses);
        tests = tests == null ? List.of() : List.copyOf(tests);
        failures = failures == null ? List.of() : List.copyOf(failures);
        reportDirectory = safe(reportDirectory);
        summary = safe(summary);
        processOutput = safe(processOutput);
    }

    public boolean successful() {
        return status == GeneratedTestExecutionStatus.PASSED
                && total > 0
                && failed == 0;
    }

    public static GeneratedTestExecutionResult skipped(List<String> testClasses, String reason) {
        return new GeneratedTestExecutionResult(
                SCHEMA_VERSION,
                GeneratedTestExecutionStatus.SKIPPED,
                0,
                0,
                0,
                0,
                0L,
                testClasses,
                List.of(),
                List.of(),
                "",
                reason,
                ""
        );
    }

    public static GeneratedTestExecutionResult blocked(List<String> testClasses, String reason) {
        return new GeneratedTestExecutionResult(
                SCHEMA_VERSION,
                GeneratedTestExecutionStatus.FAILED,
                0,
                0,
                1,
                0,
                0L,
                testClasses,
                List.of(),
                List.of(reason),
                "",
                reason,
                ""
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
