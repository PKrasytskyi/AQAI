package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;

import java.util.List;

public class PomRouteRule implements PomContractRule {

    @Override
    public void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues) {
        PomPageSpec page = spec.page();
        if (page == null || page.name().isBlank()) {
            issues.add(context.blocker("POM_PAGE_NAME_PRESENT", "Page name is required", String.valueOf(page)));
        }
        if (page == null || page.openMethod().isBlank()) {
            issues.add(context.blocker("POM_OPEN_METHOD_PRESENT", "Open method is required", String.valueOf(page)));
        }
        if (page == null || page.route().isBlank()) {
            issues.add(context.blocker("POM_ROUTE_PRESENT", "Page route is required", String.valueOf(page)));
        }
    }
}
