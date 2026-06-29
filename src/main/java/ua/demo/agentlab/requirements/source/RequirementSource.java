package ua.demo.agentlab.requirements.source;

import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;

public interface RequirementSource {

    boolean supports(RequirementInput input);

    RequirementDocument load(RequirementInput input);
}
