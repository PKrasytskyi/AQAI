package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;

/** A typed assertion with an optional exact locator binding. Route assertions intentionally have no locator. */
public record BoundSpaBehaviorAssertion(StructuredAssertionRequirement assertion, String locatorId, boolean verifiable, String reason) {
    public BoundSpaBehaviorAssertion { locatorId = locatorId == null ? "" : locatorId.trim(); reason = reason == null ? "" : reason.trim(); }
}
