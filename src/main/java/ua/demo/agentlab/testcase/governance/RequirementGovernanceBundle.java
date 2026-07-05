package ua.demo.agentlab.testcase.governance;

import java.util.List;

public record RequirementGovernanceBundle(
        String source,
        int totalRequirements,
        int canonicalTestCaseRequirements,
        List<RequirementGovernanceItem> items
) {
    public RequirementGovernanceBundle {
        source = source == null ? "" : source.trim();
        totalRequirements = Math.max(0, totalRequirements);
        canonicalTestCaseRequirements = Math.max(0, canonicalTestCaseRequirements);
        items = items == null ? List.of() : List.copyOf(items);
    }

    public List<RequirementGovernanceItem> testCaseRequirements() {
        return items.stream().filter(RequirementGovernanceItem::testCaseEligible).toList();
    }

    public List<RequirementGovernanceItem> governanceRequirements() {
        return items.stream().filter(item -> !item.testCaseEligible()).toList();
    }
}
