package ua.demo.agentlab.requirements.normalization;

import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

public interface RequirementNormalizer {

    NormalizedRequirementBundle normalize(RequirementDocument document);

}
