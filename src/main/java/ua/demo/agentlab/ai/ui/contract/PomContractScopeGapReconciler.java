package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Preserves deterministic scope gaps that are facts of the mapped evidence, not LLM suggestions.
 */
public class PomContractScopeGapReconciler {

    public PomContractSpec reconcile(PomContractSpec contract, PromptReadyPomScope scope) {
        if (contract == null || scope == null || scope.coverageGaps().isEmpty()) {
            return contract;
        }

        LinkedHashSet<String> coverageGaps = new LinkedHashSet<>();
        addNonBlank(coverageGaps, contract.coverageGaps());
        addNonBlank(coverageGaps, scope.coverageGaps());

        return new PomContractSpec(
                contract.schemaVersion(),
                contract.page(),
                contract.locators(),
                contract.components(),
                contract.actions(),
                contract.assertions(),
                List.copyOf(coverageGaps),
                contract.rejectedSuggestions()
        );
    }

    private void addNonBlank(LinkedHashSet<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(target::add);
    }
}
