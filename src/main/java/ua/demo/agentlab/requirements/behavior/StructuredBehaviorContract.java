package ua.demo.agentlab.requirements.behavior;

import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import java.util.List;
import java.util.Map;

/** Executable semantic contract extracted from one capability-first requirement block. */
public record StructuredBehaviorContract(String requirementId, String capability, List<String> actions,
                                         List<StructuredAssertionRequirement> assertions,
                                         Map<String, String> dataRequirements, String targetContext,
                                         boolean executable, List<String> reviewReasons) {
    public StructuredBehaviorContract {
        requirementId=safe(requirementId); capability=safe(capability); actions=actions==null?List.of():List.copyOf(actions);
        assertions=assertions==null?List.of():List.copyOf(assertions); dataRequirements=dataRequirements==null?Map.of():Map.copyOf(dataRequirements);
        targetContext=safe(targetContext); reviewReasons=reviewReasons==null?List.of():List.copyOf(reviewReasons);
    }
    private static String safe(String value){return value==null?"":value.trim();}
}
