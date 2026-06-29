package ua.demo.agentlab.ai.rag.intelligence.parser;

import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentType;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstType;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceRepositoryParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(class|interface)\\s+([A-Za-z0-9_]+)");

    public List<LayerComponentDefinition> parse(List<SourceDocument> documents) {
        return parse(documents, List.of());
    }

    public List<LayerComponentDefinition> parse(List<SourceDocument> documents, List<JavaAstParseResult> astResults) {
        List<LayerComponentDefinition> components = new ArrayList<>();
        Map<String, JavaAstParseResult> astByPath = new HashMap<>();
        for (JavaAstParseResult astResult : astResults) {
            astByPath.put(astResult.relativePath(), astResult);
        }
        for (SourceDocument document : documents) {
            if (!"java".equalsIgnoreCase(document.language())) {
                continue;
            }
            String content = document.content();
            JavaAstParseResult astResult = astByPath.get(document.relativePath());
            if (astResult != null && astResult.parsed()) {
                components.addAll(parseFromAst(document.relativePath(), astResult));
                continue;
            }
            String className = extract(CLASS_PATTERN, content, 2);
            LayerComponentType type = classify(document.relativePath(), className, content);
            if (type == LayerComponentType.UNKNOWN) {
                continue;
            }
            components.add(new LayerComponentDefinition(
                    className,
                    extract(PACKAGE_PATTERN, content, 1),
                    document.relativePath(),
                    type
            ));
        }
        return components;
    }

    private List<LayerComponentDefinition> parseFromAst(String relativePath, JavaAstParseResult astResult) {
        List<LayerComponentDefinition> components = new ArrayList<>();
        for (JavaAstType type : astResult.types()) {
            LayerComponentType componentType = classify(relativePath, type.name(), String.join(" ", type.annotations()));
            if (componentType != LayerComponentType.UNKNOWN) {
                components.add(new LayerComponentDefinition(
                        type.name(),
                        astResult.packageName(),
                        relativePath,
                        componentType
                ));
            }
        }
        return components;
    }

    private LayerComponentType classify(String relativePath, String className, String content) {
        String lowerPath = relativePath.toLowerCase();
        if (content.contains("@Service") || className.endsWith("Service") || lowerPath.contains("/service/")) {
            return LayerComponentType.SERVICE;
        }
        if (content.contains("@Repository") || className.endsWith("Repository") || lowerPath.contains("/repository/")) {
            return LayerComponentType.REPOSITORY;
        }
        if (className.endsWith("Client") || lowerPath.contains("/client/") || lowerPath.contains("/api/")) {
            return LayerComponentType.API_CLIENT;
        }
        if (content.contains("@Component") || lowerPath.contains("/component/")) {
            return LayerComponentType.COMPONENT;
        }
        return LayerComponentType.UNKNOWN;
    }

    private String extract(Pattern pattern, String content, int group) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(group) : "";
    }
}
