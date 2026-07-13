package ua.demo.agentlab.artifactreuse.flow;

import java.util.List;

public record FlowReuseQualityDecision(boolean reusable, List<String> reasons) {
    public FlowReuseQualityDecision {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static FlowReuseQualityDecision accepted() { return new FlowReuseQualityDecision(true, List.of()); }
    public static FlowReuseQualityDecision rejected(String reason) { return new FlowReuseQualityDecision(false, List.of(reason)); }
    public static FlowReuseQualityDecision rejected(List<String> reasons) { return new FlowReuseQualityDecision(false, reasons); }
}
