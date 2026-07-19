package ua.demo.agentlab.validation.feedback;

import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.ReviewSeverity;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;
import ua.demo.agentlab.validation.smoke.LiveUiSmokeResult;

public class GeneratedUiClosedLoopPolicy {
    public Decision evaluate(GeneratedCodeValidationResult compile,
                             GeneratedCodeReviewReport review,
                             GeneratedUiSmokeResult generatedSmoke,
                             LiveUiSmokeResult liveSmoke) {
        if (compile == null || !compile.isPassed()) return Decision.ineligible("compile gate is not green");
        if (review == null) return Decision.ineligible("review artifact is missing");
        if (review.findings().stream().anyMatch(finding -> finding.severity() == ReviewSeverity.CRITICAL)) {
            return Decision.ineligible("code review contains critical findings");
        }
        if (generatedSmoke == null || !generatedSmoke.passed()) {
            return Decision.ineligible("generated source smoke is not green");
        }
        if (liveSmoke == null || liveSmoke.status() == GeneratedUiSmokeStatus.SKIPPED) {
            return Decision.ineligible("live browser smoke did not execute");
        }
        return new Decision(true, liveSmoke.status() == GeneratedUiSmokeStatus.PASSED,
                liveSmoke.status() == GeneratedUiSmokeStatus.PASSED
                        ? "compile, review, generated smoke, and live smoke passed"
                        : "live browser smoke failed after compile/review/generated smoke passed");
    }

    public record Decision(boolean eligibleForDbFeedback, boolean passed, String reason) {
        static Decision ineligible(String reason) { return new Decision(false, false, reason); }
    }
}
