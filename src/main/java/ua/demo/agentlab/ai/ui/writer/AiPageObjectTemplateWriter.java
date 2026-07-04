package ua.demo.agentlab.ai.ui.writer;

import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.core.ui.locators.SeleniumLocatorMapper;
import ua.demo.agentlab.templates.ui.SeleniumPageObjectTemplate;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AiPageObjectTemplateWriter {

    private final String pagePackage;
    private final SeleniumPageObjectTemplate template = new SeleniumPageObjectTemplate();
    private final SeleniumLocatorMapper locatorMapper = new SeleniumLocatorMapper();

    public AiPageObjectTemplateWriter(String pagePackage) {
        if (pagePackage == null || pagePackage.isBlank()) {
            throw new IllegalArgumentException("pagePackage cannot be blank");
        }
        this.pagePackage = pagePackage.trim();
    }

    public String pagePackage() {
        return pagePackage;
    }

    public List<GeneratedSourceFile> write(List<AiPageObjectSpec> specs) {
        List<GeneratedSourceFile> files = new ArrayList<>();
        for (AiPageObjectSpec spec : specs) {
            String pageName = sanitizeClassName(spec.pageName(), "GeneratedPage");
            String route = normalizeRoute(spec.route());
            String openMethodName = sanitizeMethodName(spec.openMethodName(), "openPage");
            SeleniumPageObjectTemplate.PageObjectTemplateModel model =
                    new SeleniumPageObjectTemplate.PageObjectTemplateModel(
                            pagePackage,
                            pageName,
                            route,
                            openMethodName,
                            buildLocators(spec.locators()),
                            buildMethods(spec.methods(), openMethodName, route, buildLocatorFieldMap(spec.locators()))
                    );
            files.add(new GeneratedSourceFile(
                    pagePackage,
                    pageName,
                    toRelativePath(pagePackage, pageName),
                    template.render(model)
            ));
        }
        return files;
    }

    private List<SeleniumPageObjectTemplate.LocatorFieldTemplateModel> buildLocators(List<AiLocatorSpec> locators) {
        List<SeleniumPageObjectTemplate.LocatorFieldTemplateModel> fields = new ArrayList<>();
        for (AiLocatorSpec locator : locators) {
            LocatorHint locatorHint = toNormalizedLocatorHint(locator);
            try {
                fields.add(new SeleniumPageObjectTemplate.LocatorFieldTemplateModel(
                        sanitizeFieldName(locator.fieldName()),
                        locatorMapper.toByExpression(locatorHint)
                ));
            } catch (Exception ignored) {
                // Skip unsupported locator candidates from AI output.
            }
        }
        return fields;
    }

    private List<SeleniumPageObjectTemplate.PageMethodTemplateModel> buildMethods(
            List<AiMethodSpec> methods,
            String openMethodName,
            String route,
            Map<String, String> locatorFieldsByExpression
    ) {
        List<SeleniumPageObjectTemplate.PageMethodTemplateModel> rendered = new ArrayList<>();
        Set<String> declaredMethodNames = new LinkedHashSet<>();
        for (AiMethodSpec method : methods) {
            String methodName = sanitizeMethodName(method.methodName(), "performAction");
            if (!declaredMethodNames.add(methodName)) {
                continue;
            }
            if (methodName.equals(openMethodName)) {
                continue;
            }
            String body = sanitizeMethodBody(method.methodName(), method.body(), route, locatorFieldsByExpression);
            if (body.isBlank()) {
                continue;
            }
            String returnType = defaultIfBlank(method.returnType(), "void");
            if (returnType.equalsIgnoreCase("String") && methodName.toLowerCase(Locale.ROOT).startsWith("has")) {
                returnType = "boolean";
                body = coerceStringPresenceBodyToBoolean(body);
            }
            rendered.add(new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                    returnType,
                    methodName,
                    method.parameters().stream()
                            .map(parameter -> new SeleniumPageObjectTemplate.MethodParameterTemplateModel(
                                    defaultIfBlank(parameter.type(), "String"),
                                    sanitizeVariableName(parameter.name(), "value")
                            ))
                            .toList(),
                    body,
                    method.requiredImports().stream().distinct().toList()
            ));
        }
        return rendered;
    }

    private Map<String, String> buildLocatorFieldMap(List<AiLocatorSpec> locators) {
        Map<String, String> fieldsByExpression = new LinkedHashMap<>();
        for (AiLocatorSpec locator : locators) {
            LocatorHint locatorHint = toNormalizedLocatorHint(locator);
            try {
                String fieldName = sanitizeFieldName(locator.fieldName());
                fieldsByExpression.putIfAbsent(locatorMapper.toByExpression(locatorHint), fieldName);
                LocatorHint originalHint = new LocatorHint(
                        locator.elementName().isBlank() ? locator.fieldName() : locator.elementName(),
                        locator.strategy(),
                        locator.value()
                );
                fieldsByExpression.putIfAbsent(locatorMapper.toByExpression(originalHint), fieldName);
            } catch (Exception ignored) {
                // Skip unsupported locator candidates from AI output.
            }
        }
        return fieldsByExpression;
    }

    private LocatorHint toNormalizedLocatorHint(AiLocatorSpec locator) {
        String elementName = locator.elementName().isBlank() ? locator.fieldName() : locator.elementName();
        String strategy = locator.strategy();
        String value = locator.value();
        String normalizedRole = (locator.fieldName() + " " + locator.elementName()).toLowerCase(Locale.ROOT);
        String normalizedValue = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (normalizedRole.contains("authenticationentrypoint")
                || normalizedRole.contains("authentication entry point")) {
            if (normalizedValue.contains("auth") && !normalizedValue.contains("login")) {
                strategy = "css";
                value = "a[href*='login']";
            }
        }
        return new LocatorHint(elementName, strategy, value);
    }

    private String sanitizeMethodBody(
            String methodName,
            String body,
            String route,
            Map<String, String> locatorFieldsByExpression
    ) {
        String normalized = body == null ? "" : body.strip();
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.contains("open(route);")) {
            return "open(\"" + escapeJava(route) + "\");";
        }
        normalized = normalized.replace("elements.type(", "elements.clearAndType(");
        normalized = replaceInlineLocatorExpressions(normalized, locatorFieldsByExpression);
        normalized = strengthenWeakRouteAssertion(methodName, normalized, route, locatorFieldsByExpression);
        return normalized;
    }

    private String replaceInlineLocatorExpressions(String body, Map<String, String> locatorFieldsByExpression) {
        if (body == null || body.isBlank() || locatorFieldsByExpression == null || locatorFieldsByExpression.isEmpty()) {
            return body;
        }
        String normalized = body;
        for (Map.Entry<String, String> entry : locatorFieldsByExpression.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized;
    }

    private String strengthenWeakRouteAssertion(
            String methodName,
            String body,
            String route,
            Map<String, String> locatorFieldsByExpression
    ) {
        if (body == null || body.isBlank()) {
            return body;
        }
        String normalizedMethod = methodName == null ? "" : methodName.toLowerCase(Locale.ROOT);
        String compactBody = body.replaceAll("\\s+", "");
        boolean weakCurrentUrlCheck = compactBody.equals("return!getCurrentUrl().isBlank();")
                || compactBody.equals("returngetCurrentUrl()!=null&&!getCurrentUrl().isBlank();");
        if (!weakCurrentUrlCheck) {
            return body;
        }
        String authenticationEntryPoint = firstFieldContaining(locatorFieldsByExpression, "authenticationEntryPoint");
        if (authenticationEntryPoint != null) {
            return "return elements.attribute(" + authenticationEntryPoint + ", \"href\").contains(\"/login\");";
        }
        if (route == null || route.isBlank()) {
            return body;
        }
        if ("/".equals(route)) {
            String firstField = firstLocatorField(locatorFieldsByExpression);
            if (firstField != null) {
                return "return elements.isVisible(" + firstField + ");";
            }
            return body;
        }
        if (normalizedMethod.contains("success")
                || normalizedMethod.contains("route")
                || normalizedMethod.contains("opened")
                || normalizedMethod.contains("visible")
                || normalizedMethod.contains("authenticated")) {
            return "return getCurrentUrl().contains(\"" + escapeJava(route) + "\");";
        }
        return body;
    }

    private String coerceStringPresenceBodyToBoolean(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        String stripped = body.strip();
        if (stripped.startsWith("return elements.text(") && stripped.endsWith(");")) {
            String expression = stripped.substring("return elements.text(".length(), stripped.length() - 2);
            return "return !elements.text(" + expression + ").isBlank();";
        }
        return body;
    }

    private String firstFieldContaining(Map<String, String> locatorFieldsByExpression, String expectedText) {
        if (locatorFieldsByExpression == null || locatorFieldsByExpression.isEmpty()) {
            return null;
        }
        String normalizedExpected = expectedText == null ? "" : expectedText.toLowerCase(Locale.ROOT);
        for (String fieldName : locatorFieldsByExpression.values()) {
            if (fieldName.toLowerCase(Locale.ROOT).contains(normalizedExpected)) {
                return fieldName;
            }
        }
        return null;
    }

    private String firstLocatorField(Map<String, String> locatorFieldsByExpression) {
        if (locatorFieldsByExpression == null || locatorFieldsByExpression.isEmpty()) {
            return null;
        }
        return locatorFieldsByExpression.values().iterator().next();
    }

    private String toRelativePath(String packageName, String className) {
        return "src/test/java/" + packageName.replace('.', '/') + "/" + className + ".java";
    }

    private String normalizeRoute(String route) {
        if (route == null || route.isBlank()) {
            return "";
        }
        return route.startsWith("/") ? route : "/" + route;
    }

    private String sanitizeClassName(String value, String fallback) {
        String normalized = toTypeName(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String sanitizeFieldName(String value) {
        return sanitizeVariableName(value, "locator");
    }

    private String sanitizeMethodName(String value, String fallback) {
        String candidate = sanitizeVariableName(value, fallback);
        if (candidate.isBlank()) {
            return fallback;
        }
        return candidate;
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

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String escapeJava(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
