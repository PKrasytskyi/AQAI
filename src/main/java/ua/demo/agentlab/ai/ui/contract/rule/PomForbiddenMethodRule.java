package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomComponentSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PomForbiddenMethodRule implements PomContractRule {

    private static final Set<String> FORBIDDEN_NAMES = Set.of(
            "getdriver", "findelement", "findelements", "webelement", "by", "threadsleep"
    );

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        validateActions(spec.actions(), context, issues);
        validateAssertions(spec.assertions(), context, issues);
        for (PomComponentSpec component : spec.components()) {
            validateActions(component.actions(), context, issues);
            validateAssertions(component.assertions(), context, issues);
        }
    }

    private void validateActions(List<PomActionSpec> actions, PomContractValidationContext context, List<PomContractIssue> issues) {
        actions.stream().map(PomActionSpec::methodName).forEach(name -> validateName(name, context, issues));
    }

    private void validateAssertions(List<PomAssertionSpec> assertions, PomContractValidationContext context, List<PomContractIssue> issues) {
        assertions.stream().map(PomAssertionSpec::methodName).forEach(name -> validateName(name, context, issues));
    }

    private void validateName(String methodName, PomContractValidationContext context, List<PomContractIssue> issues) {
        String normalized = methodName == null ? "" : methodName.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_NAMES.stream().anyMatch(normalized::contains)) {
            issues.add(context.blocker("POM_METHOD_NO_RAW_SELENIUM",
                    "POM contract methods must not expose raw Selenium internals",
                    methodName));
        }
    }
}
