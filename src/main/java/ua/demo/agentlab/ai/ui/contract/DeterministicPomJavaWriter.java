package ua.demo.agentlab.ai.ui.contract;

import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.writer.AiPageObjectTemplateWriter;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DeterministicPomJavaWriter {

    private static final Set<String> COMPONENT_ROOT_ROLES = Set.of(
            "container", "form", "navigation", "header", "sidebar", "modal", "dialog", "section", "table", "list"
    );

    private final PomContractQualityGate qualityGate;
    private final AiPageObjectTemplateWriter javaTemplateWriter;
    private final PomJavaFieldNameResolver fieldNameResolver = new PomJavaFieldNameResolver();

    public DeterministicPomJavaWriter(String pagePackage) {
        this(new PomContractQualityGate(), new AiPageObjectTemplateWriter(pagePackage));
    }

    DeterministicPomJavaWriter(
            PomContractQualityGate qualityGate,
            AiPageObjectTemplateWriter javaTemplateWriter
    ) {
        if (qualityGate == null || javaTemplateWriter == null) {
            throw new IllegalArgumentException("writer dependencies cannot be null");
        }
        this.qualityGate = qualityGate;
        this.javaTemplateWriter = javaTemplateWriter;
    }

    public List<GeneratedSourceFile> write(List<PomContractSpec> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return List.of();
        }
        List<AiPageObjectSpec> renderingSpecs = new ArrayList<>();
        List<GeneratedSourceFile> componentFiles = new ArrayList<>();
        for (PomContractSpec contract : contracts) {
            PomContractQualityReport report = qualityGate.validate(contract);
            if (report.hasBlockingIssues()) {
                throw new IllegalStateException("POM contract quality gate failed for "
                        + report.pageName() + " with " + report.blockingIssueCount() + " blocking issue(s): "
                        + report.issues().stream()
                        .filter(issue -> issue.severity() == PomContractSeverity.BLOCKER)
                        .map(issue -> issue.ruleId() + " - " + issue.message() + " [" + issue.evidence() + "]")
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("no blocking issue details"));
            }
            renderingSpecs.add(toRenderingSpec(contract));
            componentFiles.addAll(writeComponents(contract));
        }
        List<GeneratedSourceFile> files = new ArrayList<>(javaTemplateWriter.write(renderingSpecs));
        files.addAll(componentFiles);
        return files;
    }

    public AiPageObjectSpec toRenderingSpec(PomContractSpec contract) {
        PomPageSpec page = contract.page();
        List<PomLocatorSpec> pageLocators = pageLocators(contract);
        Map<String, String> fieldNames = fieldNames(pageLocators);
        return new AiPageObjectSpec(
                page.name(),
                page.route(),
                page.openMethod(),
                pageLocators.stream()
                        .map(locator -> new AiLocatorSpec(
                                fieldNames.get(locator.id()),
                                locator.elementName(),
                                locator.strategy(),
                                locator.value()
                        ))
                        .toList(),
                rewriteLocatorReferences(buildMethods(contract), fieldNames)
        );
    }

    private Map<String, String> fieldNames(List<PomLocatorSpec> locators) {
        Map<String, String> names = new LinkedHashMap<>();
        locators.forEach(locator -> names.putIfAbsent(locator.id(), fieldNameResolver.resolve(locator)));
        return names;
    }

    private List<AiMethodSpec> rewriteLocatorReferences(
            List<AiMethodSpec> methods,
            Map<String, String> fieldNames
    ) {
        return methods.stream().map(method -> new AiMethodSpec(
                method.returnType(), method.methodName(), method.parameters(),
                rewriteLocatorReferences(method.body(), fieldNames), method.requiredImports())).toList();
    }

    private String rewriteLocatorReferences(String body, Map<String, String> fieldNames) {
        String rewritten = body == null ? "" : body;
        List<Map.Entry<String, String>> entries = fieldNames.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey(
                        Comparator.comparingInt(String::length).reversed()))
                .toList();
        for (Map.Entry<String, String> entry : entries) {
            rewritten = rewritten.replace("this." + field(entry.getKey()), entry.getValue());
        }
        return rewritten;
    }

    private List<AiMethodSpec> buildMethods(PomContractSpec contract) {
        List<AiMethodSpec> methods = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (PomComponentSpec component : contract.components()) {
            if (!isRenderableComponent(contract, component)) {
                continue;
            }
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
                            locatorReference(rootLocatorId)
                    ),
                    List.of()
            ));
        }
        for (PomActionSpec action : effectivePageActions(contract)) {
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
        for (PomAssertionSpec assertion : effectivePageAssertions(contract)) {
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
        Set<String> referencedLocatorIds = referencedLocatorIds(contract);
        Map<String, PomLocatorSpec> locators = new LinkedHashMap<>();
        for (PomLocatorSpec locator : allPageLocators(contract)) {
            if (!referencedLocatorIds.isEmpty() && !referencedLocatorIds.contains(locator.id())) {
                continue;
            }
            locators.putIfAbsent(locator.id(), locator);
        }
        for (PomComponentSpec component : contract.components()) {
            if (!isRenderableComponent(contract, component)) {
                continue;
            }
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

    private Set<String> referencedLocatorIds(PomContractSpec contract) {
        Set<String> ids = new LinkedHashSet<>();
        for (PomActionSpec action : effectivePageActions(contract)) {
            for (PomStepSpec step : action.steps()) {
                if (!step.locator().isBlank()) {
                    ids.add(step.locator());
                }
            }
        }
        for (PomAssertionSpec assertion : effectivePageAssertions(contract)) {
            for (PomCheckSpec check : assertion.checks()) {
                if (!check.locator().isBlank()) {
                    ids.add(check.locator());
                }
            }
        }
        for (PomComponentSpec component : contract.components()) {
            if (!isRenderableComponent(contract, component)) {
                continue;
            }
            String rootLocatorId = componentRootLocatorId(component);
            if (!rootLocatorId.isBlank() && hasComponentBehavior(component)) {
                ids.add(rootLocatorId);
            }
        }
        return ids;
    }

    private List<GeneratedSourceFile> writeComponents(PomContractSpec contract) {
        if (contract.components().isEmpty()) {
            return List.of();
        }
        List<GeneratedSourceFile> files = new ArrayList<>();
        String packageName = javaTemplateWriter.pagePackage();
        for (PomComponentSpec component : contract.components()) {
            if (!isRenderableComponent(contract, component)) {
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

    private boolean hasComponentBehavior(PomComponentSpec component) {
        return component != null
                && (!component.actions().isEmpty() || !component.assertions().isEmpty());
    }

    /**
     * A component can scope descendants only below a page-level container. Invalid LLM component
     * proposals are flattened into the page API so they cannot produce uncompilable nested lookups.
     */
    private boolean isRenderableComponent(PomContractSpec contract, PomComponentSpec component) {
        if (!hasComponentBehavior(component)) {
            return false;
        }
        String rootLocatorId = componentRootLocatorId(component);
        if (rootLocatorId.isBlank()) {
            return false;
        }
        return contract.locators().stream()
                .filter(locator -> locator.id().equals(rootLocatorId))
                .map(locator -> locator.role().toLowerCase(Locale.ROOT))
                .anyMatch(COMPONENT_ROOT_ROLES::contains);
    }

    private List<PomActionSpec> effectivePageActions(PomContractSpec contract) {
        List<PomActionSpec> actions = new ArrayList<>(contract.actions());
        contract.components().stream()
                .filter(component -> !isRenderableComponent(contract, component))
                .forEach(component -> actions.addAll(component.actions()));
        return actions;
    }

    private List<PomAssertionSpec> effectivePageAssertions(PomContractSpec contract) {
        List<PomAssertionSpec> assertions = new ArrayList<>(contract.assertions());
        contract.components().stream()
                .filter(component -> !isRenderableComponent(contract, component))
                .forEach(component -> assertions.addAll(component.assertions()));
        return assertions;
    }

    private List<PomLocatorSpec> allPageLocators(PomContractSpec contract) {
        Map<String, PomLocatorSpec> locators = new LinkedHashMap<>();
        contract.locators().forEach(locator -> locators.putIfAbsent(locator.id(), locator));
        contract.components().stream()
                .filter(component -> !isRenderableComponent(contract, component))
                .flatMap(component -> component.locators().stream())
                .forEach(locator -> locators.putIfAbsent(locator.id(), locator));
        return new ArrayList<>(locators.values());
    }

    private String renderComponent(
            String packageName,
            String className,
            PomComponentSpec component,
            String rootLocatorId
    ) {
        Map<String, String> fieldNames = fieldNames(component.locators());
        String locators = component.locators().stream()
                .filter(locator -> !locator.id().equals(rootLocatorId))
                .map(locator -> "    private final By %s = %s;".formatted(
                        fieldNames.get(locator.id()),
                        byExpression(locator)
                ))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
        String methods = rewriteLocatorReferences(renderComponentMethods(component), fieldNames);
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
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < action.steps().size(); index++) {
            lines.add(renderComponentStep(action.steps().get(index), index));
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String renderComponentStep(PomStepSpec step, int stepIndex) {
        String value = valueExpression(step.valueFrom(), step.literalValue());
        return switch (step.action()) {
            case CLICK -> "child(" + locatorReference(step.locator()) + ").click();";
            case CLEAR_AND_TYPE -> """
                    WebElement element%d = child(%s);
                    element%d.clear();
                    element%d.sendKeys(%s);""".formatted(stepIndex, locatorReference(step.locator()), stepIndex, stepIndex, value);
            case SEND_KEYS, UPLOAD_FILE -> "child(" + locatorReference(step.locator()) + ").sendKeys(" + value + ");";
            case SELECT_BY_VISIBLE_TEXT -> "dropdowns.selectByVisibleText(" + locatorReference(step.locator()) + ", " + value + ");";
            case OPEN_ROUTE -> "open(\"" + escapeJava(step.route()) + "\");";
        };
    }

    private String renderComponentAssertionBody(PomAssertionSpec assertion) {
        if ("String".equals(assertion.returnType())) {
            PomCheckSpec check = assertion.checks().isEmpty() ? null : assertion.checks().get(0);
            return check == null || check.locator().isBlank()
                    ? "return \"\";"
                    : "return child(" + locatorReference(check.locator()) + ").getText();";
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
            case VISIBLE -> "child(" + locatorReference(check.locator()) + ").isDisplayed()";
            case TEXT_PRESENT -> "!child(" + locatorReference(check.locator()) + ").getText().isBlank()";
            case TEXT_CONTAINS -> "child(" + locatorReference(check.locator()) + ").getText().contains(" + expected + ")";
            case TEXT_EQUALS -> "child(" + locatorReference(check.locator()) + ").getText().equals(" + expected + ")";
            case ATTRIBUTE_EQUALS -> "child(" + locatorReference(check.locator()) + ").getAttribute(\""
                    + escapeJava(check.attribute()) + "\").equals(" + expected + ")";
            case URL_CONTAINS -> "getCurrentUrl().contains(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case URL_EQUALS -> "getCurrentUrl().equals(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case COUNT_GREATER_THAN -> "root().findElements(" + locatorReference(check.locator()) + ").size() > "
                    + integerLiteral(check.expectedValue(), "0");
            case LIST_TEXTS -> "!root().findElements(" + locatorReference(check.locator()) + ").isEmpty()";
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
            case CLICK -> "elements.click(" + locatorReference(step.locator()) + ");";
            case CLEAR_AND_TYPE -> "elements.clearAndType(" + locatorReference(step.locator()) + ", " + value + ");";
            case SEND_KEYS -> "elements.sendKeys(" + locatorReference(step.locator()) + ", " + value + ");";
            case SELECT_BY_VISIBLE_TEXT -> "dropdowns.selectByVisibleText(" + locatorReference(step.locator()) + ", " + value + ");";
            case UPLOAD_FILE -> "elements.sendKeys(" + locatorReference(step.locator()) + ", " + value + ");";
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
            case TEXT_CONTAINS, TEXT_EQUALS, TEXT_PRESENT -> "return elements.text(" + locatorReference(check.locator()) + ");";
            case ATTRIBUTE_EQUALS -> "return elements.attribute(" + locatorReference(check.locator()) + ", \""
                    + escapeJava(check.attribute()) + "\");";
            case URL_CONTAINS, URL_EQUALS -> "return getCurrentUrl();";
            default -> "return elements.text(" + locatorReference(check.locator()) + ");";
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
                        .toList();""".formatted(locatorReference(check.locator()));
    }

    private String renderBooleanCheck(PomCheckSpec check) {
        String expected = valueExpression(check.valueFrom(), check.expectedValue());
        return switch (check.check()) {
            case VISIBLE -> "elements.isVisible(" + locatorReference(check.locator()) + ")";
            case TEXT_PRESENT -> "!elements.text(" + locatorReference(check.locator()) + ").isBlank()";
            case TEXT_CONTAINS -> "elements.text(" + locatorReference(check.locator()) + ").contains(" + expected + ")";
            case TEXT_EQUALS -> "elements.text(" + locatorReference(check.locator()) + ").equals(" + expected + ")";
            case URL_CONTAINS -> "getCurrentUrl().contains(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case URL_EQUALS -> "getCurrentUrl().equals(" + valueExpression("", firstNonBlank(check.route(), check.expectedValue())) + ")";
            case ATTRIBUTE_EQUALS -> "elements.attribute(" + locatorReference(check.locator()) + ", \""
                    + escapeJava(check.attribute()) + "\").equals(" + expected + ")";
            case COUNT_GREATER_THAN -> "elements.findAll(" + locatorReference(check.locator()) + ").size() > "
                    + integerLiteral(check.expectedValue(), "0");
            case LIST_TEXTS -> "!elements.findAll(" + locatorReference(check.locator()) + ").isEmpty()";
        };
    }

    private String valueExpression(String valueFrom, String literalValue) {
        if (valueFrom != null && !valueFrom.isBlank()) {
            String trimmed = valueFrom.trim();
            int dot = trimmed.indexOf('.');
            if (dot > 0 && dot < trimmed.length() - 1) {
                String variable = sanitizeVariableName(trimmed.substring(0, dot));
                String key = trimmed.substring(dot + 1).replaceAll("[^A-Za-z0-9._-]+", "");
                if (!variable.isBlank() && !key.isBlank()) {
                    return variable + ".required(\"" + escapeJava(key) + "\")";
                }
            }
            return sanitizeVariableName(valueFrom);
        }
        return "\"" + escapeJava(literalValue == null ? "" : literalValue) + "\"";
    }

    private String field(String locator) {
        return sanitizeVariableName(locator);
    }

    private String locatorReference(String locator) {
        return "this." + field(locator);
    }

    private String sanitizeVariableName(String value) {
        String normalized = typeName(value);
        if (normalized.isBlank()) {
            return "value";
        }
        return Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String typeName(String value) {
        String[] tokens = (value == null ? "" : value).split("[^A-Za-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String normalized = allUpperCase(token) ? token.toLowerCase(Locale.ROOT) : token;
            builder.append(Character.toUpperCase(normalized.charAt(0)));
            if (normalized.length() > 1) {
                builder.append(normalized.substring(1));
            }
        }
        return builder.toString();
    }

    private boolean allUpperCase(String value) {
        return value.chars().anyMatch(Character::isLetter)
                && value.equals(value.toUpperCase(Locale.ROOT));
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
