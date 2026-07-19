package ua.demo.agentlab.ui.testcontract.writer;

import java.util.List;

public record UiTestSourceMap(
        String schemaVersion,
        List<UiTestSourceMapEntry> entries
) {
    public UiTestSourceMap {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? "ui-test-source-map.v1"
                : schemaVersion.trim();
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
