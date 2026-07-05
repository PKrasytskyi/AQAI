package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;

public record RequirementUnit(
        NormalizedRequirement requirement,
        RequirementUnitType type,
        RequirementCapability capability,
        RequirementIntent intent,
        String ownerPage,
        String route
) {
    public RequirementUnit {
        ownerPage = ownerPage == null ? "" : ownerPage.trim();
        route = route == null ? "" : route.trim();
        type = type == null ? RequirementUnitType.POLICY : type;
        capability = capability == null ? RequirementCapability.GENERIC : capability;
        intent = intent == null ? RequirementIntent.INSPECT_CONTENT : intent;
    }
}
