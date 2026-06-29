package ua.demo.agentlab.review;

public record GeneratedCodeReviewFinding(

        String filePath,
        ReviewSeverity severity,
        String ruleId,
        String message,
        String suggestion
) {
}
