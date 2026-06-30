package ua.demo.agentlab.orchestration.pipeline;

import java.util.ArrayList;
import java.util.List;

public class Findings {

    private final List<String> entries;

    public Findings() {
        this(new ArrayList<>());
    }

    private Findings(List<String> entries) {
        this.entries = entries;
    }

    public static Findings from(List<String> entries) {
        return new Findings(entries == null ? new ArrayList<>() : new ArrayList<>(entries));
    }

    public void add(String finding) {
        if (finding == null || finding.isBlank()) {
            return;
        }
        entries.add(finding);
    }

    public List<String> entries() {
        return entries;
    }

    public Findings snapshot() {
        return new Findings(new ArrayList<>(entries));
    }
}
