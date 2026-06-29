package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

public interface CanonicalInteractionLayer {

    CanonicalUiInteractionModel build(MappedUiKnowledge knowledge);
}
