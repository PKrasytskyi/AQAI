package ua.demo.agentlab.templates.ui;

import ua.demo.agentlab.templates.assertions.AssertionTemplateLibrary;

import java.util.Objects;

public class SeleniumTestNgTemplate {

    private final UiActionTemplateLibrary actionLibrary;
    private final AssertionTemplateLibrary assertionLibrary;

    public SeleniumTestNgTemplate() {
        this(new UiActionTemplateLibrary(), new AssertionTemplateLibrary());
    }

    public SeleniumTestNgTemplate(
            UiActionTemplateLibrary actionLibrary,
            AssertionTemplateLibrary assertionLibrary
    ) {
        this.actionLibrary = Objects.requireNonNull(actionLibrary, "actionLibrary must not be null");
        this.assertionLibrary = Objects.requireNonNull(assertionLibrary, "assertionLibrary must not be null");
    }

    public String render(TestClassTemplateModel model) {
        Objects.requireNonNull(model, "model must not be null");

        String rawActionBlock = actionLibrary.buildActionBlock(
                model.operationType(),
                model.templateContext()
        );
        String actionBlock = indent(rawActionBlock, 2);

        String assertionBlock = indent(assertionLibrary.buildAssertionBlock(
                model.operationType(),
                model.templateContext()
        ), 2);

        String supportImports = buildSupportImports(rawActionBlock + System.lineSeparator() + assertionBlock);

        return """
                package %s;

                import org.testng.annotations.BeforeMethod;
                import org.testng.annotations.Test;
                import %s;
                %s
                import ua.demo.agentlab.core.ui.BaseTest;
                import ua.demo.agentlab.core.ui.assertions.UiAssertions;
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
                model.packageName(),
                model.pageClassFqn(),
                renderSourcePageImport(model),
                supportImports,
                model.className(),
                model.pageClassName(),
                model.pageVariableName(),
                indent(renderSourcePageField(model), 1),
                model.pageVariableName(),
                model.pageClassName(),
                indent(renderSourcePageSetup(model), 1),
                escapeJava(model.testDescription()),
                model.testMethodName(),
                actionBlock,
                assertionBlock
        );
    }

    private String buildSupportImports(String actionBlock) {
        StringBuilder builder = new StringBuilder();

        if (actionBlock.contains("ScenarioData ")) {
            builder.append("import ua.demo.agentlab.core.data.ScenarioData;")
                    .append(System.lineSeparator());
        }
        if (actionBlock.contains("UserCredentials ")) {
            builder.append("import ua.demo.agentlab.core.data.UserCredentials;")
                    .append(System.lineSeparator());
        }

        return builder.toString().stripTrailing();
    }

    private String renderSourcePageImport(TestClassTemplateModel model) {
        if (!model.hasDistinctSourcePage()) {
            return "";
        }
        return "import " + model.sourcePageClassFqn() + ";" + System.lineSeparator();
    }

    private String renderSourcePageField(TestClassTemplateModel model) {
        if (!model.hasDistinctSourcePage()) {
            return "";
        }
        return "private %s %s;".formatted(
                model.sourcePageClassName(),
                model.sourcePageVariableName()
        );
    }

    private String renderSourcePageSetup(TestClassTemplateModel model) {
        if (!model.hasDistinctSourcePage()) {
            return "";
        }
        return "%s = new %s(driver, runtimeConfig);".formatted(
                model.sourcePageVariableName(),
                model.sourcePageClassName()
        );
    }

    private String indent(String block, int level) {
        String indent = "    ".repeat(level);
        String normalized = block == null ? "" : block.stripTrailing();
        if (normalized.isBlank()) {
            return "";
        }

        return normalized.lines()
                .map(line -> line.isBlank() ? "" : indent + line)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String escapeJava(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    public record TestClassTemplateModel(
            String packageName,
            String className,
            String sourcePageClassFqn,
            String sourcePageVariableName,
            String pageClassFqn,
            String pageVariableName,
            String testMethodName,
            String testDescription,
            UiOperationType operationType,
            UiScenarioTemplateContext templateContext
    ) {
        public TestClassTemplateModel {
            require(packageName, "packageName");
            require(className, "className");
            require(pageClassFqn, "pageClassFqn");
            require(pageVariableName, "pageVariableName");
            require(testMethodName, "testMethodName");
            require(testDescription, "testDescription");
            sourcePageClassFqn = blankToNull(sourcePageClassFqn);
            sourcePageVariableName = defaultValue(sourcePageVariableName, "sourcePage");
            Objects.requireNonNull(operationType, "operationType must not be null");
            Objects.requireNonNull(templateContext, "templateContext must not be null");
        }

        public String pageClassName() {
            int lastDot = pageClassFqn.lastIndexOf('.');
            return lastDot >= 0 ? pageClassFqn.substring(lastDot + 1) : pageClassFqn;
        }

        public boolean hasDistinctSourcePage() {
            return sourcePageClassFqn != null && !sourcePageClassFqn.equals(pageClassFqn);
        }

        public String sourcePageClassName() {
            if (sourcePageClassFqn == null) {
                return pageClassName();
            }
            int lastDot = sourcePageClassFqn.lastIndexOf('.');
            return lastDot >= 0 ? sourcePageClassFqn.substring(lastDot + 1) : sourcePageClassFqn;
        }

        private static void require(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank");
            }
        }

        private static String defaultValue(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
