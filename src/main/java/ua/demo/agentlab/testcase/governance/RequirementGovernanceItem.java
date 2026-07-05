package ua.demo.agentlab.testcase.governance;

import java.util.List;

public record RequirementGovernanceItem(
        String requirementId,
        String title,
        String statement,
        RequirementGovernanceCategory category,
        boolean testCaseEligible,
        String governanceTarget,
        String rationale,
        String sourceReference,
        List<String> tags
) {
    public RequirementGovernanceItem {
        requirementId = safe(requirementId);
        title = safe(title);
        statement = safe(statement);
        category = category == null ? RequirementGovernanceCategory.CONTEXT_RULE : category;
        governanceTarget = safe(governanceTarget);
        rationale = safe(rationale);
        sourceReference = safe(sourceReference);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
