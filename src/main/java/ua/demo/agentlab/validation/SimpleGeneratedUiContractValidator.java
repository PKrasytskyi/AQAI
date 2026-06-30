package ua.demo.agentlab.validation;

import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleGeneratedUiContractValidator implements GeneratedUiContractValidator {

    private static final Set<String> BASE_PAGE_PUBLIC_METHODS = Set.of(
            "getTitle",
            "getCurrentUrl",
            "getPageSource",
            "open",
            "openAbsolute"
    );
    private static final List<String> FORBIDDEN_TEST_TOKENS = List.of(
            "ScenarioData.from(",
            "ScenarioData.fromKey(",
            "driver.",
            "findElement(",
            "findElements(",
            "By.",
            "WebElement ",
            ".elements",
            ".locators",
            ".waits",
            "JavascriptExecutor"
    );
    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bpublic\\s+class\\s+(\\w+)");
    private static final Pattern PUBLIC_METHOD_PATTERN =
            Pattern.compile("\\bpublic\\s+(?:static\\s+)?[\\w<>\\[\\], ?]+\\s+(\\w+)\\s*\\(");
    private static final Pattern FIELD_VARIABLE_PATTERN =
            Pattern.compile("\\bprivate\\s+(\\w+)\\s+(\\w+)\\s*;");
    private static final Pattern LOCAL_VARIABLE_PATTERN =
            Pattern.compile("\\b(\\w+)\\s+(\\w+)\\s*=\\s*new\\s+\\1\\s*\\(");
    private static final Pattern METHOD_CALL_PATTERN =
            Pattern.compile("\\b(\\w+)\\.(\\w+)\\s*\\(");
    private static final Pattern MEMBER_ACCESS_PATTERN =
            Pattern.compile("\\b(\\w+)\\.(\\w+)\\b");
    private static final Pattern INLINE_BY_IN_PAGE_METHOD_PATTERN =
            Pattern.compile("elements\\.(?:click|isVisible|clearAndType|sendKeys|text|attribute|findAll)\\s*\\(\\s*By\\.");

    @Override
    public GeneratedUiContractValidationResult validate(GeneratedUiSources sources) {
        if (sources == null) {
            return new GeneratedUiContractValidationResult(
                    ValidationStatus.UNAVAILABLE,
                    "Generated UI sources are not available for UI contract validation",
                    List.of()
            );
        }

        Set<String> violations = new LinkedHashSet<>();
        Map<String, Set<String>> pageMethodsByClass = parsePageContracts(sources.pageObjectFiles(), violations);

        for (GeneratedSourceFile testFile : sources.uiTestFiles()) {
            validateTestFile(testFile, pageMethodsByClass, violations);
        }

        List<String> uniqueViolations = new ArrayList<>(violations);

        if (uniqueViolations.isEmpty()) {
            return new GeneratedUiContractValidationResult(
                    ValidationStatus.PASSED,
                    "Generated UI tests match generated page object method contracts",
                    List.of()
            );
        }

        return new GeneratedUiContractValidationResult(
                ValidationStatus.FAILED,
                "Detected %d UI contract issue(s) between generated tests and page objects".formatted(uniqueViolations.size()),
                List.copyOf(uniqueViolations)
        );
    }

    private Map<String, Set<String>> parsePageContracts(
            List<GeneratedSourceFile> pageObjectFiles,
            Set<String> violations
    ) {
        Map<String, Set<String>> contracts = new LinkedHashMap<>();
        for (GeneratedSourceFile file : pageObjectFiles) {
            validatePageObjectFile(file, violations);
            String className = extractClassName(file.content());
            if (className == null || className.isBlank()) {
                className = file.className();
            }
            contracts.put(className, extractPublicMethods(file.content(), className, file.relativePath(), violations));
        }
        return contracts;
    }

    private void validatePageObjectFile(GeneratedSourceFile pageObjectFile, Set<String> violations) {
        String content = pageObjectFile.content();
        if (content.contains("elements.type(")) {
            violations.add("%s uses elements.type(), but generated Page Objects must use elements.sendKeys() or elements.clearAndType()"
                    .formatted(pageObjectFile.relativePath()));
        }
        Matcher inlineByMatcher = INLINE_BY_IN_PAGE_METHOD_PATTERN.matcher(content);
        if (inlineByMatcher.find()) {
            violations.add("%s repeats inline By.* locators inside methods; method bodies must reuse declared private locator fields"
                    .formatted(pageObjectFile.relativePath()));
        }
        if (content.contains("return !getCurrentUrl().isBlank();")) {
            violations.add("%s uses weak current-url non-blank assertion; generated Page Objects must assert route fragments, attributes, text, or visible page-specific elements"
                    .formatted(pageObjectFile.relativePath()));
        }
    }

    private void validateTestFile(
            GeneratedSourceFile testFile,
            Map<String, Set<String>> pageMethodsByClass,
            Set<String> violations
    ) {
        for (String forbiddenToken : FORBIDDEN_TEST_TOKENS) {
            if (testFile.content().contains(forbiddenToken)) {
                violations.add("%s uses unsupported framework helper '%s'"
                        .formatted(testFile.relativePath(), forbiddenToken.replace("(", "")));
            }
        }
        Map<String, String> pageVariables = extractPageVariables(testFile.content(), pageMethodsByClass.keySet());
        Matcher matcher = METHOD_CALL_PATTERN.matcher(testFile.content());
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String methodName = matcher.group(2);
            String pageClassName = pageVariables.get(variableName);
            if (pageClassName == null) {
                continue;
            }

            Set<String> availableMethods = pageMethodsByClass.get(pageClassName);
            if (availableMethods == null) {
                violations.add("%s references page class %s via variable '%s', but no generated page object exists"
                        .formatted(testFile.relativePath(), pageClassName, variableName));
                continue;
            }

            if (!availableMethods.contains(methodName)) {
                violations.add("%s calls %s.%s(), but %s does not declare that public method"
                        .formatted(testFile.relativePath(), variableName, methodName, pageClassName));
            }
        }

        Matcher memberMatcher = MEMBER_ACCESS_PATTERN.matcher(testFile.content());
        while (memberMatcher.find()) {
            String variableName = memberMatcher.group(1);
            String memberName = memberMatcher.group(2);
            String pageClassName = pageVariables.get(variableName);
            if (pageClassName == null) {
                continue;
            }

            int nextIndex = memberMatcher.end();
            if (nextIndex < testFile.content().length() && testFile.content().charAt(nextIndex) == '(') {
                continue;
            }

            violations.add("%s accesses %s.%s directly, but tests may use only public page-object methods or inherited BasePage methods"
                    .formatted(testFile.relativePath(), variableName, memberName));
        }
    }

    private Map<String, String> extractPageVariables(String content, Set<String> knownPageClasses) {
        Map<String, String> variables = new LinkedHashMap<>();

        Matcher fieldMatcher = FIELD_VARIABLE_PATTERN.matcher(content);
        while (fieldMatcher.find()) {
            String className = fieldMatcher.group(1);
            String variableName = fieldMatcher.group(2);
            if (knownPageClasses.contains(className)) {
                variables.put(variableName, className);
            }
        }

        Matcher localMatcher = LOCAL_VARIABLE_PATTERN.matcher(content);
        while (localMatcher.find()) {
            String className = localMatcher.group(1);
            String variableName = localMatcher.group(2);
            if (knownPageClasses.contains(className)) {
                variables.put(variableName, className);
            }
        }

        return variables;
    }

    private Set<String> extractPublicMethods(
            String content,
            String className,
            String relativePath,
            Set<String> violations
    ) {
        Set<String> methods = new LinkedHashSet<>();
        methods.addAll(BASE_PAGE_PUBLIC_METHODS);
        Matcher matcher = PUBLIC_METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (!methodName.equals(className)) {
                if (!methods.add(methodName)) {
                    violations.add("%s declares duplicate public method %s()"
                            .formatted(relativePath, methodName));
                }
            }
        }
        return methods;
    }

    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
