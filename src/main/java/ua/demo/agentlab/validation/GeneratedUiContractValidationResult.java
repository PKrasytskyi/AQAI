package ua.demo.agentlab.validation;

import java.util.List;

public record GeneratedUiContractValidationResult(
        ValidationStatus status,
        String summary,
        List<String> violations
) {

    public boolean isPassed() {
        return status == ValidationStatus.PASSED;
    }

    public boolean isFailed() {
        return status == ValidationStatus.FAILED;
    }
}
