package ua.demo.agentlab.ai.pageenrichment.service;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;

import java.util.List;

public class RuleBasedPageModelEnrichmentClient implements PageModelEnrichmentClient {

    @Override
    public List<PageModelEnrichmentRecord> enrich(List<PageModelEnrichmentInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        return inputs.stream().map(this::enrichOne).toList();
    }

    private PageModelEnrichmentRecord enrichOne(PageModelEnrichmentInput input) {
        List<String> postconditions = !input.requirementAssertions().isEmpty()
                ? input.requirementAssertions()
                : input.assertionFacts().isEmpty()
                    ? List.of("Route matches " + input.route())
                    : input.assertionFacts();
        List<String> risks = input.stableLocators().isEmpty()
                ? List.of("No stable locator evidence was mapped for this page")
                : List.of();
        List<String> coverageGaps = input.knownGaps().isEmpty()
                ? input.testCaseIds().isEmpty()
                    ? List.of("No canonical test case is mapped to this page")
                    : List.of()
                : input.knownGaps();
        return new PageModelEnrichmentRecord(
                input.pageId(),
                input.pageName(),
                input.route(),
                input.capability().isBlank() ? "Support the required user workflow on this page." : input.capability().toLowerCase(),
                "Mapped page with " + input.actions().size() + " action(s), "
                        + input.stableLocators().size() + " stable locator candidate(s), and "
                        + input.testCaseIds().size() + " relevant canonical test case(s).",
                input.requirementActions().isEmpty() ? input.actions() : input.requirementActions(),
                input.stableLocators(),
                input.preconditions(),
                postconditions,
                risks,
                coverageGaps,
                input.requirementRefs(),
                input.actionsByRequirement(),
                input.postconditionsByRequirement(),
                input.stableLocators().isEmpty() ? 0.55d : 0.82d,
                "rule-based"
        );
    }
}
