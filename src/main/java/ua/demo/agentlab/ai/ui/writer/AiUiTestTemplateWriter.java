package ua.demo.agentlab.ai.ui.writer;

import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiUiTestTemplateWriter {

    private static final Pattern STATIC_SCENARIO_DATA_CALL =
            Pattern.compile("ScenarioData\\s+([A-Za-z0-9_]+)\\s*=\\s*ScenarioData\\.(?:from|fromKey)\\(\"([^\"]+)\"\\);");

    private final String pagePackage;
    private final String testPackage;
    private final AiSeleniumTestTemplate template = new AiSeleniumTestTemplate();

    public AiUiTestTemplateWriter(String pagePackage, String testPackage) {
        if (pagePackage == null || pagePackage.isBlank()) {
            throw new IllegalArgumentException("pagePackage cannot be blank");
        }
        if (testPackage == null || testPackage.isBlank()) {
            throw new IllegalArgumentException("testPackage cannot be blank");
        }
        this.pagePackage = pagePackage.trim();
        this.testPackage = testPackage.trim();
    }

    public List<GeneratedSourceFile> write(List<AiUiTestSpec> specs) {
        List<GeneratedSourceFile> files = new ArrayList<>();
        for (AiUiTestSpec spec : specs) {
            String className = sanitizeClassName(spec.className(), "GeneratedUiTest");
            AiUiTestSpec normalized = new AiUiTestSpec(
                    spec.scenarioId(),
                    className,
                    sanitizeClassName(spec.sourcePageClassName(), ""),
                    sanitizeVariableName(spec.sourcePageVariableName(), "sourcePage"),
                    sanitizeClassName(spec.pageClassName(), "GeneratedPage"),
                    sanitizeVariableName(spec.pageVariableName(), "page"),
                    sanitizeMethodName(spec.testMethodName(), "shouldExecuteScenario"),
                    spec.testDescription().isBlank() ? className : spec.testDescription(),
                    sanitizeJavaBlock(spec.actionBody()),
                    sanitizeJavaBlock(spec.assertionBody()),
                    spec.additionalImports()
            );
            files.add(new GeneratedSourceFile(
                    testPackage,
                    className,
                    "src/test/java/" + testPackage.replace('.', '/') + "/" + className + ".java",
                    template.render(testPackage, pagePackage, normalized)
            ));
        }
        return files;
    }

    private String sanitizeClassName(String value, String fallback) {
        String normalized = toTypeName(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String sanitizeMethodName(String value, String fallback) {
        String candidate = sanitizeVariableName(value, fallback);
        return candidate.isBlank() ? fallback : candidate;
    }

    private String sanitizeVariableName(String value, String fallback) {
        String typeName = toTypeName(value);
        if (typeName.isBlank()) {
            return fallback;
        }
        return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
    }

    private String toTypeName(String value) {
        String[] tokens = (value == null ? "" : value).split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private String sanitizeJavaBlock(String block) {
        String normalized = block == null ? "" : block.strip();
        if (normalized.isBlank()) {
            return normalized;
        }
        Matcher matcher = STATIC_SCENARIO_DATA_CALL.matcher(normalized);
        StringBuffer rewritten = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String dataSetName = matcher.group(2);
            matcher.appendReplacement(
                    rewritten,
                    Matcher.quoteReplacement(
                            "ScenarioData " + variableName + " = scenarioData(\"" + dataSetName + "\");"
                    )
            );
        }
        matcher.appendTail(rewritten);
        return rewritten.toString().replace("ScenarioData.fromKey(", "scenarioData(")
                .replace("ScenarioData.from(", "scenarioData(");
    }
}
