package ua.demo.agentlab.policy.model;

import java.util.List;

public record SelectorPolicy(
        List<SelectorStrategy> priorityOrder,
        boolean allowXPath,
        boolean allowCssFallback,
        boolean allowTextFallback,
        boolean requireStableAttributes,
        List<String> preferredTestAttributes
) {

    public SelectorPolicy {
        priorityOrder = List.copyOf(priorityOrder);
        preferredTestAttributes = List.copyOf(preferredTestAttributes);

        if(priorityOrder.isEmpty()){
            throw new IllegalArgumentException("Selector priority order cannot be empty");
        }
    }

    public enum SelectorStrategy{
        DATA_TESTID,
        LABEL,
        ROLE,
        ID,
        NAME,
        CSS,
        XPATH,
        TEXT
    }
}
