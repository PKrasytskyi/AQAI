package ua.demo.agentlab.ai.rag.retrieval;

public class RetrievalPolicyResolver {

    public RetrievalPolicy resolve(TaskClassification classification, int requestedArtifacts) {
        if (classification == null) {
            throw new IllegalArgumentException("classification cannot be null");
        }
        int baseArtifacts = requestedArtifacts > 0 ? requestedArtifacts : defaultArtifacts(classification.taskType());
        return switch (classification.taskType()) {
            case FAILURE_ANALYSIS -> new RetrievalPolicy(
                    classification.taskType(),
                    baseArtifacts,
                    4,
                    3,
                    true,
                    true,
                    true
            );
            case CODE_REVIEW, BUG_FIX -> new RetrievalPolicy(
                    classification.taskType(),
                    baseArtifacts,
                    4,
                    2,
                    true,
                    false,
                    true
            );
            case PAGE_OBJECT_GENERATION -> new RetrievalPolicy(
                    classification.taskType(),
                    baseArtifacts,
                    3,
                    2,
                    true,
                    false,
                    false
            );
            case API_TEST_GENERATION, NEGATIVE_TEST_GENERATION, REGRESSION_TEST_GENERATION -> new RetrievalPolicy(
                    classification.taskType(),
                    baseArtifacts,
                    4,
                    2,
                    true,
                    true,
                    true
            );
            case SMOKE_TEST_GENERATION -> new RetrievalPolicy(
                    classification.taskType(),
                    baseArtifacts,
                    3,
                    2,
                    true,
                    false,
                    true
            );
            case UI_TEST_GENERATION, GENERAL_AUTOMATION -> new RetrievalPolicy(
                    classification.taskType(),
                    baseArtifacts,
                    3,
                    2,
                    true,
                    false,
                    true
            );
        };
    }

    private int defaultArtifacts(RagTaskType taskType) {
        return switch (taskType) {
            case FAILURE_ANALYSIS, CODE_REVIEW, BUG_FIX -> 8;
            case API_TEST_GENERATION, NEGATIVE_TEST_GENERATION, REGRESSION_TEST_GENERATION -> 7;
            default -> 6;
        };
    }
}
