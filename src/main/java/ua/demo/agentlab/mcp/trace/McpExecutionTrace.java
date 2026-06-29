package ua.demo.agentlab.mcp.trace;

import java.util.ArrayList;
import java.util.List;

public class McpExecutionTrace {

    private final List<McpExecutionTraceEntry> entries = new ArrayList<>();

    public synchronized void record(McpExecutionTraceEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry cannot be null");
        }
        entries.add(entry);
    }

    public synchronized List<McpExecutionTraceEntry> entries() {
        return List.copyOf(entries);
    }

    public synchronized void clear() {
        entries.clear();
    }
}
