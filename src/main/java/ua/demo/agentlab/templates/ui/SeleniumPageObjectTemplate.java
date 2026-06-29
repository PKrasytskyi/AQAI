package ua.demo.agentlab.templates.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SeleniumPageObjectTemplate {

    public String render(PageObjectTemplateModel model) {
        Objects.requireNonNull(model, "model must not be null");

        String importsBlock = renderImports(model);
        String locatorsBlock = indent(renderLocators(model.locators()), 1);
        String constructorBlock = indent(renderConstructor(model.className()), 1);
        String openMethodBlock = indent(renderOpenMethod(model.openMethodName(), model.route()), 1);
        String methodsBlock = indent(renderMethods(model.methods()), 1);

        return """
                package %s;

                %s
                public class %s extends BasePage {

                %s
                %s
                %s
                %s
                }
                """.formatted(
                model.packageName(),
                importsBlock,
                model.className(),
                joinClassSections(locatorsBlock),
                constructorBlock,
                openMethodBlock,
                methodsBlock
        );
    }

    private String renderImports(PageObjectTemplateModel model) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("org.openqa.selenium.By");
        imports.add("org.openqa.selenium.WebDriver");
        imports.add("ua.demo.agentlab.core.ui.BasePage");
        imports.add("ua.demo.agentlab.core.config.UiRuntimeConfig");

        for (PageMethodTemplateModel method : model.methods()) {
            imports.addAll(method.requiredImports());
        }

        List<String> sortedImports = imports.stream()
                .filter(value -> value != null && !value.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();

        String rendered = sortedImports.stream()
                .map(importName -> "import " + importName + ";")
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");

        return rendered.isBlank() ? "" : rendered + System.lineSeparator();
    }

    private String renderLocators(List<LocatorFieldTemplateModel> locators) {
        if (locators == null || locators.isEmpty()) {
            return "";
        }

        return locators.stream()
                .map(locator -> "private final By %s = %s;".formatted(
                        locator.fieldName(),
                        locator.byExpression()
                ))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String renderConstructor(String className) {
        return """
                public %s(WebDriver driver, UiRuntimeConfig runtimeConfig) {
                    super(driver, runtimeConfig);
                }
                """.formatted(className);
    }

    private String renderOpenMethod(String openMethodName, String route) {
        if (route == null || route.isBlank()) {
            return "";
        }

        return """
                public void %s() {
                    open("%s");
                }
                """.formatted(openMethodName, escapeJava(route));
    }

    private String renderMethods(List<PageMethodTemplateModel> methods) {
        if (methods == null || methods.isEmpty()) {
            return "";
        }

        return methods.stream()
                .map(this::renderMethod)
                .reduce((left, right) -> left + System.lineSeparator() + System.lineSeparator() + right)
                .orElse("");
    }

    private String renderMethod(PageMethodTemplateModel method) {
        String parameters = method.parameters().stream()
                .map(parameter -> parameter.type() + " " + parameter.name())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        String methodBody = indent(method.body().stripTrailing(), 1);

        return """
                public %s %s(%s) {
                %s
                }
                """.formatted(
                method.returnType(),
                method.methodName(),
                parameters,
                methodBody
        );
    }

    private String joinClassSections(String... sections) {
        List<String> nonBlankSections = new ArrayList<>();
        for (String section : sections) {
            if (section != null && !section.isBlank()) {
                nonBlankSections.add(section);
            }
        }
        return String.join(System.lineSeparator() + System.lineSeparator(), nonBlankSections);
    }

    private String indent(String block, int level) {
        if (block == null || block.isBlank()) {
            return "";
        }

        String indent = "    ".repeat(level);
        return block.lines()
                .map(line -> line.isBlank() ? "" : indent + line)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record PageObjectTemplateModel(
            String packageName,
            String className,
            String route,
            String openMethodName,
            List<LocatorFieldTemplateModel> locators,
            List<PageMethodTemplateModel> methods
    ) {
        public PageObjectTemplateModel {
            require(packageName, "packageName");
            require(className, "className");

            openMethodName = defaultValue(openMethodName, "openPage");
            locators = List.copyOf(defaultList(locators));
            methods = List.copyOf(defaultList(methods));
        }
    }

    public record LocatorFieldTemplateModel(
            String fieldName,
            String byExpression
    ) {
        public LocatorFieldTemplateModel {
            require(fieldName, "fieldName");
            require(byExpression, "byExpression");
        }
    }

    public record PageMethodTemplateModel(
            String returnType,
            String methodName,
            List<MethodParameterTemplateModel> parameters,
            String body,
            List<String> requiredImports
    ) {
        public PageMethodTemplateModel {
            require(returnType, "returnType");
            require(methodName, "methodName");
            require(body, "body");

            parameters = List.copyOf(defaultList(parameters));
            requiredImports = List.copyOf(defaultList(requiredImports));
        }
    }

    public record MethodParameterTemplateModel(
            String type,
            String name
    ) {
        public MethodParameterTemplateModel {
            require(type, "type");
            require(name, "name");
        }
    }

    private static void require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
