package ua.demo.agentlab.review;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.persistence.GeneratedUiSources;

public interface GeneratedCodeReviewer {
    GeneratedCodeReviewReport review(GeneratedUiSources sources);

    default GeneratedCodeReviewReport review(WorkflowState state) {
        return review(state == null
                ? null
                : new GeneratedUiSources(state.getPageObjectFiles(), state.getUiTestFiles()));
    }
}
