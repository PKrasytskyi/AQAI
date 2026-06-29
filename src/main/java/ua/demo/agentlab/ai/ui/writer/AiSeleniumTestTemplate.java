package ua.demo.agentlab.ai.ui.writer;

import ua.demo.agentlab.ai.ui.model.AiUiTestSpec;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class AiSeleniumTestTemplate {

    public String render(String testPackage, String pagePackage, AiUiTestSpec spec) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("org.testng.annotations.BeforeMethod");
        imports.add("org.testng.annotations.Test");
        imports.add("ua.demo.agentlab.core.ui.BaseTest");
        imports.add("ua.demo.agentlab.core.ui.assertions.UiAssertions");
        imports.add(pagePackage + "." + spec.pageClassName());
        if (!spec.sourcePageClassName().isBlank() && !spec.sourcePageClassName().equals(spec.pageClassName())) {
            imports.add(pagePackage + "." + spec.sourcePageClassName());
        }
        imports.addAll(spec.additionalImports());

        String importsBlock = imports.stream()
                .filter(value -> value != null && !value.isBlank())
                .sorted(Comparator.naturalOrder())
                .map(value -> "import " + value + ";")
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");

        String sourceFieldBlock = spec.sourcePageClassName().isBlank() || spec.sourcePageClassName().equals(spec.pageClassName())
                ? ""
                : "private %s %s;".formatted(spec.sourcePageClassName(), spec.sourcePageVariableName()) + System.lineSeparator();

        String sourceSetupBlock = spec.sourcePageClassName().isBlank() || spec.sourcePageClassName().equals(spec.pageClassName())
                ? ""
                : "%s = new %s(driver, runtimeConfig);".formatted(spec.sourcePageVariableName(), spec.sourcePageClassName())
                + System.lineSeparator();

        return """
                package %s;

                %s

                public class %s extends BaseTest {

                    private %s %s;
                %s
                    @BeforeMethod(alwaysRun = true)
                    public void setUpPageObject() {
                        %s = new %s(driver, runtimeConfig);
                %s
                    }

                    @Test(description = "%s")
                    public void %s() {
                %s

                %s
                    }
                }
                """.formatted(
                testPackage,
                importsBlock,
                spec.className(),
                spec.pageClassName(),
                spec.pageVariableName(),
                indent(sourceFieldBlock, 1),
                spec.pageVariableName(),
                spec.pageClassName(),
                indent(sourceSetupBlock, 1),
                escapeJava(spec.testDescription()),
                spec.testMethodName(),
                indent(spec.actionBody(), 2),
                indent(spec.assertionBody(), 2)
        );
    }

    private String indent(String block, int level) {
        if (block == null || block.isBlank()) {
            return "";
        }
        String prefix = "    ".repeat(level);
        return block.stripTrailing().lines()
                .map(line -> line.isBlank() ? "" : prefix + line)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String escapeJava(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
