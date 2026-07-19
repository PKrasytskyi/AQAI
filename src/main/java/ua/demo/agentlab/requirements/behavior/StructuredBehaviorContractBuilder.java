package ua.demo.agentlab.requirements.behavior;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import java.util.*;

/** Builds deterministic behavior contracts without asking an LLM to infer required state changes. */
public final class StructuredBehaviorContractBuilder {
    public List<StructuredBehaviorContract> build(List<NormalizedRequirement> requirements) {
        return requirements == null ? List.of() : requirements.stream().filter(this::structured).map(this::build).toList();
    }
    private StructuredBehaviorContract build(NormalizedRequirement requirement) {
        String capability=tag(requirement,"capability-");
        List<String> actions=values(requirement, "action");
        String context=values(requirement, "target context").stream().map(this::cleanMarkdownValue).collect(java.util.stream.Collectors.joining(" "));
        Map<String,String> data=data(values(requirement, "data requirements"));
        List<String> review=new ArrayList<>();
        if(requirement.structuredAssertions().isEmpty()) review.add("No typed Assertion Requirements were declared.");
        if(capability.equals("filter") && changesData(actions) && data.isEmpty()) review.add("FILTER action requires scenario data or an explicit no-data policy.");
        if(capability.equals("filter") && changesData(actions) && requirement.structuredAssertions().stream().noneMatch(a -> a.type().equalsIgnoreCase("RESULTS_CHANGED") || a.type().equalsIgnoreCase("ROW_VISIBLE"))) review.add("FILTER action requires RESULTS_CHANGED or ROW_VISIBLE postcondition.");
        return new StructuredBehaviorContract(requirement.id(),capability,actions,requirement.structuredAssertions(),data,context,review.isEmpty(),review);
    }
    private boolean structured(NormalizedRequirement r){return r!=null&&r.tags()!=null&&r.tags().contains("structured-requirement");}
    private String tag(NormalizedRequirement r,String prefix){return r.tags().stream().filter(t->t.startsWith(prefix)).map(t->t.substring(prefix.length())).findFirst().orElse("");}
    private List<String> values(NormalizedRequirement requirement, String section) {
        List<String> values = requirement.structuredSections().getOrDefault(section, List.of());
        if (!values.isEmpty()) return values;
        return legacySection(requirement.statement(), section + ":");
    }
    private List<String> legacySection(String statement, String marker) {
        if (statement == null || statement.isBlank()) return List.of();
        String lower = statement.toLowerCase(Locale.ROOT);
        int from = lower.indexOf(marker.toLowerCase(Locale.ROOT));
        if (from < 0) return List.of();
        String value = statement.substring(from + marker.length());
        String valueLower = value.toLowerCase(Locale.ROOT);
        int next = Integer.MAX_VALUE;
        for (String heading : List.of(". capability:", ". action:", ". target context:",
                ". assertion requirements:", ". data requirements:", ". expected result:")) {
            int candidate = valueLower.indexOf(heading);
            if (candidate >= 0) next = Math.min(next, candidate);
        }
        if (next != Integer.MAX_VALUE) value = value.substring(0, next);
        return Arrays.stream(value.split("(?<=\\.)\\s+")).map(String::trim).filter(v -> !v.isBlank()).toList();
    }
    private boolean changesData(List<String> actions) {
        return actions.stream().map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("select ") || value.contains("enter ") || value.contains("type ") || value.contains("apply "));
    }
    private Map<String,String> data(List<String> lines){Map<String,String> result=new LinkedHashMap<>();for(String line:lines){int colon=line.indexOf(':');if(colon>0)result.put(cleanMarkdownValue(line.substring(0,colon)).replaceAll("[^A-Za-z0-9_]",""),cleanMarkdownValue(line.substring(colon+1)));}return result;}
    private String cleanMarkdownValue(String value) {
        String cleaned = value == null ? "" : value.trim();
        while (cleaned.length() >= 2 && cleaned.startsWith("`") && cleaned.endsWith("`")) cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        return cleaned.endsWith("`") ? cleaned.substring(0, cleaned.length() - 1).trim() : cleaned;
    }
}
