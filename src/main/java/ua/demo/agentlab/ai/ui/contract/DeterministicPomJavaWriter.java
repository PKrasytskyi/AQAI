package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.writer.AiPageObjectTemplateWriter;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DeterministicPomJavaWriter {

    private final PomContractQualityGate qualityGate;
    private final AiPageObjectTemplateWriter compatibilityWriter;

    public DeterministicPomJavaWriter(String pagePackage) {
        this(new PomContractQualityGate(), new AiPageObjectTemplateWriter(pagePackage));
    }

    DeterministicPomJavaWriter(
            PomContractQualityGate qualityGate,
            AiPageObjectTemplateWriter compatibilityWriter
    ) {
        if (qualityGate == null || compatibilityWriter == null) {
            throw new IllegalArgumentException("writer dependencies cannot be null");
        }
        this.qualityGate = qualityGate;
        this.compatibilityWriter = compatibilityWriter;
    }

    public List<GeneratedSourceFile> write(List<PomContractSpec> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return List.of();
        }
        List<AiPageObjectSpec> adapted = new ArrayList<>();
        List<GeneratedSourceFile> componentFiles = new ArrayList<>();
        for (PomContractSpec contract : contracts) {
            PomContractQualityReport report = qualityGate.validate(contract);
            if (report.hasBlockingIssues()) {
                throw new IllegalStateException("POM contract quality gate failed for "
                        + report.pageName() + " with " + report.blockingIssueCount() + " blocking issue(s)");
            }
            adapted.add(toAiPageObjectSpec(contract));
            componentFiles.addAll(writeComponents(contract));
        }
        List<GeneratedSourceFile> files = new ArrayList<>(compatibilityWriter.write(adapted));
        files.addAll(componentFiles);
        return files;
    }

    public AiPageObjectSpec toAiPageObjectSpec(PomContractSpec contract) {
        PomPageSpec page = contract.page();
        return new AiPageObjectSpec(
                page.name(),
                page.route(),
                page.openMethod(),
                pageLocators(contract).stream()
                        .map(locator -> new AiLocatorSpec(
                                locator.id(),
                                locator.elementName(),
                                locator.strategy(),
                                locator.value()
                        ))
                        .toList(),
                buildMethods(contract)
        );
    }

    private List<AiMethodSpec> buildMethods(PomContractSpec contract) {
        List<AiMethodSpec> methods = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (PomComponentSpec component : contract.components()) {
            String rootLocatorId = componentRootLocatorId(component);
            if (rootLocatorId.isBlank()) {
                continue;
            }
            String methodName = componentAccessorMethodName(component.name());
            if (!emitted.add(methodName)) {
                continue;
            }
            methods.add(new AiMethodSpec(
                    componentClassName(component.name()),
                    methodName,
                    List.of(),
                    "return new %s(driver, runtimeConfig, %s);".formatted(
                            componentClassName(component.name()),
                            field(rootLocatorId)
                    ),
                    List.of()
            ));
        }
        for (PomActionSpec action : contract.actions()) {
            if (!emitted.add(action.methodName())) {
                continue;
            }
            methods.add(new AiMethodSpec(
                    "void",
                    action.methodName(),
                    action.parameters(),
                    renderActionBody(action),
                    List.of()
            ));
        }
        for (PomAssertionSpec assertion : contract.assertions()) {
            if (!emitted.add(assertion.methodName())) {
                continue;
            }
            RenderedAssertion rendered = renderAssertionBody(assertion);
            methods.add(new AiMethodSpec(
                    assertion.returnType(),
                    assertion.methodName(),
                    List.of(),
                    rendered.body(),
                    rendered.requiredImports()
            ));
        }
        return methods;
    }

    private List<PomLocatorSpec> pageLocators(PomContractSpec contract) {
        Map<String, PomLocatorSpec> locators = new LinkedHashMap<>();
        for (PomLocatorSpec locator : contract.locators()) {
            locators.putIfAbsent(locator.id(), locator);
        }
        for (PomComponentSpec component : contract.components()) {
            String rootLocatorId = componentRootLocatorId(component);
            if (rootLocatorId.isBlank() || locators.containsKey(rootLocatorId)) {
                continue;
            }
            component.locators().stream()
                    .filter(locator -> locator.id().equals(rootLocatorId))
                    .findFirst()
                    .ifPresent(locator -> locators.put(locator.id(), locator));
        }
        return new ArrayList<>(locators.values());
    }

    private List<GeneratedSourceFile> writeComponents(PomContractSpec contract) {
        if (contract.components().isEmpty()) {
            return List.of();
        }
        List<GeneratedSourceFile> files = new ArrayList<>();
        String packageName = compatibilityWriter.pagePackage();
        for (PomComponentSpec component : contract.components()) {
            if (!component.reusable() && component.actions().isEmpty() && component.assertions().isEmpty()) {
                continue;
            }
            String rootLocatorId = componentRootLocatorId(component);
            if (rootLocatorId.isBlank()) {
                continue;
            }
            String className = componentClassName(component.name());
            files.add(new GeneratedSourceFile(
                    packageName,
                    className,
                    "src/test/java/" + packageName.replace('.', '/') + "/" + className + ".java",
                    renderComponent(packageName, className, component, rootLocatorId)
            ));
        }
        return files;
    }

    private String renderComponent(
            String packageName,
            String className,
            PomComponentSpec component,
            String rootLocatorId
    ) {
        String locators = component.locators().stream()
                .filter(locator -> !locator.id().equals(rootLocatorId))
                .map(locator -> "    private final By %s = %s;".formatted(
                        field(locator.id()),
                        byExpression(locator)
                ))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
        String methods = renderComponentMethods(component);
        return """
                package %s;

                import org.openqa.selenium.By;
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.WebElement;
                import ua.demo.agentlab.core.config.UiRuntimeConfig;
                import ua.demo.agentlab.core.ui.BasePage;

                public class %s extends BasePage {

                    private final By rootLocator;
                %s

                    public %s(WebDriver driver, UiRuntimeConfig runtimeConfig, By rootLocator) {
                        super(driver, runtimeConfig);
                        this.rootLocator = rootLocator;
                    }

                    private WebElement root() {
                        return elements.find(rootLocator);
                    }

                    private WebElement child(By locator) {
                        return root().findElement(locator);
                    }
                %s
                }
                """.formatted(
                packageName,
                className,
                locators.isBlank() ? "" : System.lineSeparator() + locators,
                className,
                methods.isBlank() ? "" : System.lineSeparator() + indent(methods, 1)
        );
    }

    private String renderComponentMethods(PomComponentSpec component) {
        List<String> methods = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (PomActionSpec action : component.actions()) {
            if (!emitted.add(action.methodName())) {
                continue;
            }
            methods.add("""
                    public void %s(%s) {
                    %s
                    }""".formatted(
                    sanitizeVariableName(action.methodName()),
                    renderParameters(action.parameters()),
                    indent(renderComponentActionBody(action), 1)
            ));
        }
        for (PomAssertionSpec assertion : component.assertions()) {
            if (!emitted.add(assertion.methodName())) {
                continue;
            }
            methods.add("""
                    public %s %s() {
                    %s
                    }""".formatted(
                    assertion.returnType(),
                    sanitizeVariableName(assertion.methodName()),
                    indent(renderComponentAssertionBody(assertion), 1)
            ));
        }
        return String.join(System.lineSeparator() + System.lineSeparator(), methods);
    }

    private String renderComponentActionBody(PomActionSpec action) {
        return action.steps().stream()
                .map(this::renderComponentStep)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String renderComponentStep(PomStepSpec step) {
        String value = valueExpression(step.valueFrom(), step.literalValue());
        return switch (step.action()) {
            case CLICK -> "child(" + field(step.locator()) + ").click();";
            case CLEAR_AND_TYPE -> """
                    WebElement element = child(%s);
                    element.clear();
                    element.sendKeys(%s);""".formatted(field(step.locator()), value);
            case SEND_KEYS, UPLOAD_FILE -> "child(" + field(step.locator()) + ").sendKeys(" + value + ");";
            case SELECT_BY_VISIBLE_TEXT -> "dropdowns.selectByVisibleText(" + field(step.locator()) + ", " + value + ");";
            case OPEN_ROUTE -> "open(\"" + escapeJava(step.route()) + "\");";
        };
    }

    private String renderComponentAssertionBody(PomAssertionSpec assertion) {
        if ("String".equals(assertion.returnType())) {
            PomCheckSpec check = assertion.checks().isEmpty() ? null : assertion.checks().get(0);
            return check == null || check.locator().isBlank()
                    ? "return \"\";"
                    : "return child(" + field(check.locator()) + ").getText();";
        }
        List<String> expressions = assertion.checks().stream()
                .map(this::renderComponentBooleanCheck)
                .filter(value -> !value.isBlank())
                .toList();
        if (expressions.isEmpty()) {
            return "return false;";
        }
        String operator = "OR".equalsIgnoreCase(assertion.combine()) ? " || " : " && ";
        return "return " + String.join(operator, expressions) + ";";
    }

    private String renderComponentBooleanCheck(PomCheckSpec check) {
        String expected = valueExpression(check.valueFrom(), check.expectedValue());
        return switch (check.check()) {
            case VISIBLE -> "child(" + field(check.locator()) + ").isDisplayed()";
            case TEXT_PRESENT -> "!child(" + field(check.locator()) + ").getText().isBlank()";
            case TEXT_CONTAINS -> "child(" + field(check.locator()) + ").getText().contains(" + expected + ")";
            case TEXT_EQUALS -> "child(" + field(check.locator()) + ").getText().equals(" + expected + ")";
            case ATTRIBUTE_EQUALS -> "child(" + field(check.locator()) + ").getAttribute(\""
                    + escapeJava(check.attribute()) + "\").equals(" + expected + ")";
            case URL_CONTAINS -> "getCurrentUrl().contains(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case URL_EQUALS -> "getCurrentUrl().equals(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case COUNT_GREATER_THAN -> "root().findElements(" + field(check.locator()) + ").size() > "
                    + integerLiteral(check.expectedValue(), "0");
            case LIST_TEXTS -> "!root().findElements(" + field(check.locator()) + ").isEmpty()";
        };
    }

    private String byExpression(PomLocatorSpec locator) {
        return switch (locator.strategy()) {
            case "id" -> "By.id(\"" + escapeJava(locator.value()) + "\")";
            case "name" -> "By.name(\"" + escapeJava(locator.value()) + "\")";
            case "css" -> "By.cssSelector(\"" + escapeJava(locator.value()) + "\")";
            case "xpath" -> "By.xpath(\"" + escapeJava(locator.value()) + "\")";
            case "partialLinkText" -> "By.partialLinkText(\"" + escapeJava(locator.value()) + "\")";
            default -> "By.cssSelector(\"" + escapeJava(locator.value()) + "\")";
        };
    }

    private String renderParameters(List<AiMethodParameterSpec> parameters) {
        return parameters.stream()
                .map(parameter -> parameter.type() + " " + sanitizeVariableName(parameter.name()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String componentRootLocatorId(PomComponentSpec component) {
        if (!component.rootLocatorId().isBlank()) {
            return component.rootLocatorId();
        }
        return component.locators().isEmpty() ? "" : component.locators().get(0).id();
    }

    private String componentClassName(String value) {
        String typeName = typeName(value);
        if (typeName.isBlank()) {
            return "GeneratedComponent";
        }
        return typeName.endsWith("Component") ? typeName : typeName + "Component";
    }

    private String componentAccessorMethodName(String value) {
        String className = componentClassName(value);
        String base = className.endsWith("Component")
                ? className.substring(0, className.length() - "Component".length())
                : className;
        if (base.isBlank()) {
            base = "component";
        }
        return Character.toLowerCase(base.charAt(0)) + base.substring(1) + "Component";
    }

    private String renderActionBody(PomActionSpec action) {
        List<String> lines = new ArrayList<>();
        for (PomStepSpec step : action.steps()) {
            lines.add(renderStep(step));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String renderStep(PomStepSpec step) {
        String value = valueExpression(step.valueFrom(), step.literalValue());
        return switch (step.action()) {
            case CLICK -> "elements.click(" + field(step.locator()) + ");";
            case CLEAR_AND_TYPE -> "elements.clearAndType(" + field(step.locator()) + ", " + value + ");";
            case SEND_KEYS -> "elements.sendKeys(" + field(step.locator()) + ", " + value + ");";
            case SELECT_BY_VISIBLE_TEXT -> "dropdowns.selectByVisibleText(" + field(step.locator()) + ", " + value + ");";
            case UPLOAD_FILE -> "elements.sendKeys(" + field(step.locator()) + ", " + value + ");";
            case OPEN_ROUTE -> "open(\"" + escapeJava(step.route()) + "\");";
        };
    }

    private RenderedAssertion renderAssertionBody(PomAssertionSpec assertion) {
        if ("String".equals(assertion.returnType())) {
            return new RenderedAssertion(renderStringAssertion(assertion), List.of());
        }
        if ("List<String>".equals(assertion.returnType())) {
            return new RenderedAssertion(renderListAssertion(assertion), List.of(
                    "java.util.List",
                    "org.openqa.selenium.WebElement"
            ));
        }
        List<String> expressions = assertion.checks().stream()
                .map(this::renderBooleanCheck)
                .filter(value -> !value.isBlank())
                .toList();
        if (expressions.isEmpty()) {
            return new RenderedAssertion("return false;", List.of());
        }
        String operator = "OR".equalsIgnoreCase(assertion.combine()) ? " || " : " && ";
        return new RenderedAssertion("return " + String.join(operator, expressions) + ";", List.of());
    }

    private String renderStringAssertion(PomAssertionSpec assertion) {
        PomCheckSpec check = assertion.checks().isEmpty() ? null : assertion.checks().get(0);
        if (check == null) {
            return "return \"\";";
        }
        return switch (check.check()) {
            case TEXT_CONTAINS, TEXT_EQUALS, TEXT_PRESENT -> "return elements.text(" + field(check.locator()) + ");";
            case ATTRIBUTE_EQUALS -> "return elements.attribute(" + field(check.locator()) + ", \""
                    + escapeJava(check.attribute()) + "\");";
            case URL_CONTAINS, URL_EQUALS -> "return getCurrentUrl();";
            default -> "return elements.text(" + field(check.locator()) + ");";
        };
    }

    private String renderListAssertion(PomAssertionSpec assertion) {
        PomCheckSpec check = assertion.checks().isEmpty() ? null : assertion.checks().get(0);
        if (check == null || check.locator().isBlank()) {
            return "return List.of();";
        }
        return """
                return elements.findAll(%s).stream()
                        .map(WebElement::getText)
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();""".formatted(field(check.locator()));
    }

    private String renderBooleanCheck(PomCheckSpec check) {
        String expected = valueExpression(check.valueFrom(), check.expectedValue());
        return switch (check.check()) {
            case VISIBLE -> "elements.isVisible(" + field(check.locator()) + ")";
            case TEXT_PRESENT -> "!elements.text(" + field(check.locator()) + ").isBlank()";
            case TEXT_CONTAINS -> "elements.text(" + field(check.locator()) + ").contains(" + expected + ")";
            case TEXT_EQUALS -> "elements.text(" + field(check.locator()) + ").equals(" + expected + ")";
            case URL_CONTAINS -> "getCurrentUrl().contains(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case URL_EQUALS -> "getCurrentUrl().equals(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case ATTRIBUTE_EQUALS -> "elements.attribute(" + field(check.locator()) + ", \""
                    + escapeJava(check.attribute()) + "\").equals(" + expected + ")";
            case COUNT_GREATER_THAN -> "elements.findAll(" + field(check.locator()) + ").size() > "
                    + integerLiteral(check.expectedValue(), "0");
            case LIST_TEXTS -> "!elements.findAll(" + field(check.locator()) + ").isEmpty()";
        };
    }

    private String valueExpression(String valueFrom, String literalValue) {
        if (valueFrom != null && !valueFrom.isBlank()) {
            return sanitizeVariableName(valueFrom);
        }
        return "\"" + escapeJava(literalValue == null ? "" : literalValue) + "\"";
    }

    private String field(String locator) {
        return sanitizeVariableName(locator);
    }

    private String sanitizeVariableName(String value) {
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
        if (builder.isEmpty()) {
            return "value";
        }
        return Character.toLowerCase(builder.charAt(0)) + builder.substring(1);
    }

    private String typeName(String value) {
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

    private String indent(String value, int level) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String prefix = "    ".repeat(Math.max(0, level));
        return value.lines()
                .map(line -> line.isBlank() ? "" : prefix + line)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    private String integerLiteral(String value, String fallback) {
        try {
            return String.valueOf(Integer.parseInt(value == null ? "" : value.trim()));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstNonBlank(String first, String second) {
        String left = first == null ? "" : first.trim();
        return left.isBlank() ? second == null ? "" : second.trim() : left;
    }

    private String escapeJava(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private record RenderedAssertion(
            String body,
            List<String> requiredImports
    ) {
        private RenderedAssertion {
            body = body == null ? "" : body.strip();
            requiredImports = requiredImports == null ? List.of() : List.copyOf(requiredImports);
        }
    }
}
