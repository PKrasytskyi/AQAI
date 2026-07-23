package ua.demo.agentlab.ui.generation.provenance;

import java.util.List;

/** Neutral provenance contract shared by deterministic generation and runtime feedback. */
public record PomSourceMap(int pages, List<PageEntry> entries) {
    public PomSourceMap {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public record PageEntry(String pageName, String route, String sourceFile,
                            List<FieldEntry> fields, List<MethodEntry> methods) {
        public PageEntry {
            pageName = safe(pageName);
            route = safe(route);
            sourceFile = safe(sourceFile);
            fields = fields == null ? List.of() : List.copyOf(fields);
            methods = methods == null ? List.of() : List.copyOf(methods);
        }
    }

    public record FieldEntry(String fieldName, String pomLocatorId, String strategy, String value, String componentId) {
        public FieldEntry {
            fieldName = safe(fieldName);
            pomLocatorId = safe(pomLocatorId);
            strategy = safe(strategy);
            value = safe(value);
            componentId = safe(componentId);
        }
    }

    public record MethodEntry(String methodName, String componentId, List<String> pomActionIds, List<String> pomLocatorIds) {
        public MethodEntry {
            methodName = safe(methodName);
            componentId = safe(componentId);
            pomActionIds = pomActionIds == null ? List.of() : List.copyOf(pomActionIds);
            pomLocatorIds = pomLocatorIds == null ? List.of() : List.copyOf(pomLocatorIds);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
