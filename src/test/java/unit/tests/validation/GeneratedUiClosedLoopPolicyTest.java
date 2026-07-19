package unit.tests.validation;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.review.GeneratedCodeReviewFinding;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.ReviewSeverity;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.ValidationStatus;
import ua.demo.agentlab.validation.feedback.GeneratedUiClosedLoopPolicy;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;
import ua.demo.agentlab.validation.smoke.LiveUiSmokeResult;

import java.util.List;

public class GeneratedUiClosedLoopPolicyTest {
    private final GeneratedUiClosedLoopPolicy policy = new GeneratedUiClosedLoopPolicy();

    @Test
    public void allowsExactDbFeedbackOnlyAfterAllGatesAndLiveSmoke() {
        var decision = policy.evaluate(compile(ValidationStatus.PASSED), review(List.of()),
                generated(GeneratedUiSmokeStatus.PASSED), live(GeneratedUiSmokeStatus.PASSED));

        Assert.assertTrue(decision.eligibleForDbFeedback());
        Assert.assertTrue(decision.passed());
    }

    @Test
    public void skipsFeedbackForCompileOrReviewFailureAndDemotesOnlyAfterLiveFailure() {
        Assert.assertFalse(policy.evaluate(compile(ValidationStatus.FAILED), review(List.of()),
                generated(GeneratedUiSmokeStatus.PASSED), live(GeneratedUiSmokeStatus.PASSED)).eligibleForDbFeedback());
        var critical = new GeneratedCodeReviewFinding("Page.java", ReviewSeverity.CRITICAL, "R", "bad", "fix");
        Assert.assertFalse(policy.evaluate(compile(ValidationStatus.PASSED), review(List.of(critical)),
                generated(GeneratedUiSmokeStatus.PASSED), live(GeneratedUiSmokeStatus.PASSED)).eligibleForDbFeedback());

        var liveFailure = policy.evaluate(compile(ValidationStatus.PASSED), review(List.of()),
                generated(GeneratedUiSmokeStatus.PASSED), live(GeneratedUiSmokeStatus.FAILED));
        Assert.assertTrue(liveFailure.eligibleForDbFeedback());
        Assert.assertFalse(liveFailure.passed());
    }

    private GeneratedCodeValidationResult compile(ValidationStatus status) {
        return new GeneratedCodeValidationResult(status, status.name(), "", List.of());
    }

    private GeneratedCodeReviewReport review(List<GeneratedCodeReviewFinding> findings) {
        return new GeneratedCodeReviewReport("review", 1, findings.size(), findings);
    }

    private GeneratedUiSmokeResult generated(GeneratedUiSmokeStatus status) {
        return new GeneratedUiSmokeResult(status, status.name(), 1, List.of());
    }

    private LiveUiSmokeResult live(GeneratedUiSmokeStatus status) {
        return new LiveUiSmokeResult(status, status.name(), "https://example.test", List.of(), List.of());
    }
}
