package ua.demo.agentlab.orchestration.pipeline;

import java.util.ArrayList;
import java.util.List;

public class AuditTrail {

    private final List<String> entries;

    public AuditTrail() {
        this(new ArrayList<>());
    }

    private AuditTrail(List<String> entries) {
        this.entries = entries;
    }

    public static AuditTrail from(List<String> entries) {
        return new AuditTrail(entries == null ? new ArrayList<>() : new ArrayList<>(entries));
    }

    public void add(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        entries.add(message);
    }

    public List<String> entries() {
        return entries;
    }

    public AuditTrail snapshot() {
        return new AuditTrail(new ArrayList<>(entries));
    }
}
