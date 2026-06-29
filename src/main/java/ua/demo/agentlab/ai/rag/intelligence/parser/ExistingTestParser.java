package ua.demo.agentlab.ai.rag.intelligence.parser;

import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstMethod;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstType;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExistingTestParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([A-Za-z0-9_]+)");
    private static final Pattern TEST_METHOD_PATTERN = Pattern.compile("@Test[\\s\\S]*?(public|protected|private)\\s+void\\s+([A-Za-z0-9_]+)\\s*\\(");

    public List<ExistingTestDefinition> parse(List<SourceDocument> documents) {
        return parse(documents, List.of());
    }

    public List<ExistingTestDefinition> parse(List<SourceDocument> documents, List<JavaAstParseResult> astResults) {
        List<ExistingTestDefinition> tests = new ArrayList<>();
        Map<String, JavaAstParseResult> astByPath = new HashMap<>();
        for (JavaAstParseResult astResult : astResults) {
            astByPath.put(astResult.relativePath(), astResult);
        }
        for (SourceDocument document : documents) {
            if (!"java".equalsIgnoreCase(document.language())) {
                continue;
            }
            String content = document.content();
            String lowerPath = document.relativePath().toLowerCase();
            if (!lowerPath.contains("/test/") && !content.contains("@Test") && !lowerPath.endsWith("test.java")) {
                continue;
            }
            JavaAstParseResult astResult = astByPath.get(document.relativePath());
            if (astResult != null && astResult.parsed()) {
                tests.addAll(parseFromAst(document.relativePath(), content, astResult));
                continue;
            }
            List<String> methods = new ArrayList<>();
            Matcher methodMatcher = TEST_METHOD_PATTERN.matcher(content);
            while (methodMatcher.find()) {
                methods.add(methodMatcher.group(2));
            }
            String framework = content.contains("org.testng") ? "TestNG"
                    : content.contains("org.junit") ? "JUnit"
                    : content.contains("io.cucumber") ? "Cucumber"
                    : "Unknown";
            tests.add(new ExistingTestDefinition(
                    extract(CLASS_PATTERN, content, 1),
                    extract(PACKAGE_PATTERN, content, 1),
                    document.relativePath(),
                    framework,
                    methods
            ));
        }
        return tests;
    }

    private List<ExistingTestDefinition> parseFromAst(String relativePath, String content, JavaAstParseResult astResult) {
        List<ExistingTestDefinition> tests = new ArrayList<>();
        String framework = detectFramework(content, astResult);
        for (JavaAstType type : astResult.types()) {
            List<String> methods = type.methods().stream()
                    .filter(JavaAstMethod::testMethod)
                    .map(JavaAstMethod::name)
                    .toList();
            if (!methods.isEmpty() || type.name().endsWith("Test")) {
                tests.add(new ExistingTestDefinition(
                        type.name(),
                        astResult.packageName(),
                        relativePath,
                        framework,
                        methods
                ));
            }
        }
        return tests;
    }

    private String detectFramework(String content, JavaAstParseResult astResult) {
        if (content.contains("org.testng") || astResult.imports().stream().anyMatch(value -> value.contains("org.testng"))) {
            return "TestNG";
        }
        if (content.contains("org.junit") || astResult.imports().stream().anyMatch(value -> value.contains("org.junit"))) {
            return "JUnit";
        }
        if (content.contains("io.cucumber") || astResult.imports().stream().anyMatch(value -> value.contains("io.cucumber"))) {
            return "Cucumber";
        }
        return "Unknown";
    }

    private String extract(Pattern pattern, String content, int group) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(group) : "";
    }
}
