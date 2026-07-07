package ua.demo.agentlab.ai.ui.contract.rule;

import ua.demo.agentlab.ai.ui.contract.PomContractIssue;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.util.List;

public interface PomContractRule {

    void validate(PomContractSpec spec, PomContractValidationContext context, List<PomContractIssue> issues);
}
