package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record JavaAstParseResult(
        String relativePath,
        String packageName,
        List<String> imports,
        List<JavaAstType> types,
        List<String> warnings,
        boolean parsed,
        double confidenceScore
) {
    public JavaAstParseResult {
        relativePath = relativePath == null ? "" : relativePath.trim();
        packageName = packageName == null ? "" : packageName.trim();
        imports = imports == null ? List.of() : List.copyOf(imports);
        types = types == null ? List.of() : List.copyOf(types);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        confidenceScore = Math.max(0.0d, Math.min(1.0d, confidenceScore));
    }

    public static JavaAstParseResult failed(String relativePath, String warning) {
        return new JavaAstParseResult(relativePath, "", List.of(), List.of(), List.of(warning), false, 0.0d);
    }
}
