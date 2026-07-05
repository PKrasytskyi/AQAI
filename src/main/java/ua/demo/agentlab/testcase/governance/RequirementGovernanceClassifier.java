package ua.demo.agentlab.testcase.governance;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RequirementGovernanceClassifier {

    public RequirementGovernanceItem classify(NormalizedRequirement requirement) {
        if (requirement == null) {
            return new RequirementGovernanceItem(
                    "",
                    "",
                    "",
                    RequirementGovernanceCategory.CONTEXT_RULE,
                    false,
                    "context",
                    "Missing requirement",
                    "",
                    List.of()
            );
        }
        Set<String> tags = normalizedTags(requirement.tags());
        String text = normalize(requirement.title() + " " + requirement.statement());
        RequirementGovernanceCategory category;
        boolean eligible;
        String target;
        String rationale;

        if (tags.contains("functional-requirements")) {
            category = RequirementGovernanceCategory.EXECUTABLE_FUNCTIONAL;
            eligible = true;
            target = "canonical-test-case";
            rationale = "Functional UI behavior can be converted into an executable canonical test case.";
        } else if (tags.contains("assertion-requirements")) {
            category = RequirementGovernanceCategory.ASSERTION_REQUIREMENT;
            eligible = true;
            target = "canonical-test-case/assertion-contract";
            rationale = "Assertion requirement provides executable verification intent.";
        } else if (tags.contains("ui-expectations")
                && containsAny(text, "route matches", "route contains", "configured project login route",
                "configured project authenticated route")) {
            category = RequirementGovernanceCategory.ROUTE_EXPECTATION;
            eligible = true;
            target = "canonical-test-case/assertion-contract";
            rationale = "Route expectation is deterministic and executable.";
        } else if (tags.contains("ui-expectations") && containsAny(text, "username", "password", "test data", "credentials")) {
            category = RequirementGovernanceCategory.TEST_DATA_RULE;
            eligible = false;
            target = "scenario-data-policy";
            rationale = "Test data rules configure scenario inputs but are not standalone UI test cases.";
        } else if (tags.contains("ui-expectations")) {
            category = RequirementGovernanceCategory.CONTEXT_RULE;
            eligible = false;
            target = "prompt-context";
            rationale = "UI expectation is contextual guidance and should not become a standalone test case.";
        } else if (tags.contains("runtime-evidence-expectations")) {
            category = RequirementGovernanceCategory.RUNTIME_EVIDENCE_RULE;
            eligible = false;
            target = "runtime-evidence-policy";
            rationale = "Runtime evidence rules guide mapper validation and must not become UI tests.";
        } else if (tags.contains("page-ownership-expectations")) {
            category = RequirementGovernanceCategory.PAGE_OWNERSHIP_RULE;
            eligible = false;
            target = "page-ownership-policy";
            rationale = "Ownership rules constrain prompt scope and Page Object boundaries.";
        } else if (tags.contains("quality-expectations")) {
            category = RequirementGovernanceCategory.QUALITY_RULE;
            eligible = false;
            target = "quality-gate-policy";
            rationale = "Quality rules are executable gates or lint policy, not product UI scenarios.";
        } else if (tags.contains("non-functional-requirements")) {
            category = RequirementGovernanceCategory.NON_FUNCTIONAL_RULE;
            eligible = false;
            target = "locator-quality-policy";
            rationale = "Non-functional locator stability requirements guide scoring and quality gates.";
        } else if (tags.contains("out-of-scope")) {
            category = RequirementGovernanceCategory.OUT_OF_SCOPE;
            eligible = false;
            target = "scope-control";
            rationale = "Out-of-scope items explicitly prevent test generation.";
        } else {
            category = RequirementGovernanceCategory.CONTEXT_RULE;
            eligible = false;
            target = "prompt-context";
            rationale = "Requirement was not classified as executable UI behavior.";
        }

        return new RequirementGovernanceItem(
                requirement.id(),
                requirement.title(),
                requirement.statement(),
                category,
                eligible,
                target,
                rationale,
                sourceReference(requirement),
                requirement.tags()
        );
    }

    private String sourceReference(NormalizedRequirement requirement) {
        if (requirement.sourceReference() == null) {
            return "";
        }
        var reference = requirement.sourceReference();
        if (reference.startLine() > 0) {
            return reference.source() + " [L" + reference.startLine() + "]";
        }
        return reference.source();
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private Set<String> normalizedTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.stream()
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }
}
