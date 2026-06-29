package ua.demo.agentlab.ai.rag.intelligence.parser;

import ua.demo.agentlab.ai.rag.intelligence.model.DtoModelDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.DtoModelKind;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstType;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DtoModelParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(class|record)\\s+([A-Za-z0-9_]+)");

    public List<DtoModelDefinition> parse(List<SourceDocument> documents) {
        return parse(documents, List.of());
    }

    public List<DtoModelDefinition> parse(List<SourceDocument> documents, List<JavaAstParseResult> astResults) {
        List<DtoModelDefinition> models = new ArrayList<>();
        Map<String, JavaAstParseResult> astByPath = new HashMap<>();
        for (JavaAstParseResult astResult : astResults) {
            astByPath.put(astResult.relativePath(), astResult);
        }
        for (SourceDocument document : documents) {
            if (!"java".equalsIgnoreCase(document.language())) {
                continue;
            }
            JavaAstParseResult astResult = astByPath.get(document.relativePath());
            if (astResult != null && astResult.parsed()) {
                models.addAll(parseFromAst(document.relativePath(), astResult));
                continue;
            }
            String content = document.content();
            String className = extract(CLASS_PATTERN, content, 2);
            if (className.isBlank()) {
                continue;
            }
            DtoModelKind kind = classify(document.relativePath(), className, content);
            if (kind == DtoModelKind.UNKNOWN) {
                continue;
            }
            models.add(new DtoModelDefinition(
                    className,
                    extract(PACKAGE_PATTERN, content, 1),
                    document.relativePath(),
                    kind
            ));
        }
        return models;
    }

    private List<DtoModelDefinition> parseFromAst(String relativePath, JavaAstParseResult astResult) {
        List<DtoModelDefinition> models = new ArrayList<>();
        for (JavaAstType type : astResult.types()) {
            DtoModelKind kind = classify(relativePath, type.name(), String.join(" ", type.annotations()));
            if (kind != DtoModelKind.UNKNOWN) {
                models.add(new DtoModelDefinition(
                        type.name(),
                        astResult.packageName(),
                        relativePath,
                        kind
                ));
            }
        }
        return models;
    }

    private DtoModelKind classify(String relativePath, String className, String content) {
        String lowerPath = relativePath.toLowerCase();
        if (content.contains("@Entity") || lowerPath.contains("/entity/") || className.endsWith("Entity")) {
            return DtoModelKind.ENTITY;
        }
        if (className.endsWith("Dto") || className.endsWith("DTO") || className.endsWith("Request")
                || className.endsWith("Response") || lowerPath.contains("/dto/") || content.contains("@JsonProperty")) {
            return DtoModelKind.DTO;
        }
        if (className.endsWith("Model") || lowerPath.contains("/model/") || lowerPath.contains("/domain/")) {
            return DtoModelKind.MODEL;
        }
        return DtoModelKind.UNKNOWN;
    }

    private String extract(Pattern pattern, String content, int group) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(group) : "";
    }
}
