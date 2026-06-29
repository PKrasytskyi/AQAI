package ua.demo.agentlab.validation;

import java.util.List;

public record GeneratedCodeValidationResult(
        ValidationStatus status,
        String summary,
        String compilerOutput,
        List<GeneratedFileValidation> files
) {

    public boolean isPassed() {
        return status == ValidationStatus.PASSED;
    }

    public boolean isFailed() {
        return status == ValidationStatus.FAILED;
    }

    public boolean isUnavailable() {
        return status == ValidationStatus.UNAVAILABLE;
    }
}
