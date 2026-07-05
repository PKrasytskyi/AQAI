package ua.demo.agentlab.testcase.governance;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import java.util.List;

public class RequirementGovernancePartitioner {

    private final RequirementGovernanceClassifier classifier;

    public RequirementGovernancePartitioner() {
        this(new RequirementGovernanceClassifier());
    }

    RequirementGovernancePartitioner(RequirementGovernanceClassifier classifier) {
        this.classifier = classifier;
    }

    public RequirementGovernanceBundle partition(NormalizedRequirementBundle bundle) {
        if (bundle == null) {
            return new RequirementGovernanceBundle("", 0, 0, List.of());
        }
        List<RequirementGovernanceItem> items = bundle.requirements().stream()
                .map(classifier::classify)
                .toList();
        int canonicalCount = (int) items.stream()
                .filter(RequirementGovernanceItem::testCaseEligible)
                .count();
        return new RequirementGovernanceBundle(
                bundle.source(),
                bundle.requirements().size(),
                canonicalCount,
                items
        );
    }
}
