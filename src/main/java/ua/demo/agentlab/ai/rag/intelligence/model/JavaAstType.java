package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record JavaAstType(
        String name,
        String kind,
        List<String> annotations,
        String superClass,
        List<String> interfaces,
        List<JavaAstMethod> methods
) {
    public JavaAstType {
        name = name == null ? "" : name.trim();
        kind = kind == null ? "" : kind.trim();
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        superClass = superClass == null ? "" : superClass.trim();
        interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
        methods = methods == null ? List.of() : List.copyOf(methods);
    }
}
