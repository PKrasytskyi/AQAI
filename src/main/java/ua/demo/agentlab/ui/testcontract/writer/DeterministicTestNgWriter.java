package ua.demo.agentlab.ui.testcontract.writer;

import ua.demo.agentlab.ui.testcontract.model.UiTestActionSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSource;
import ua.demo.agentlab.ui.testcontract.model.UiTestArgumentSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestAssertionSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractBundle;
import ua.demo.agentlab.ui.testcontract.model.UiTestContractSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestDataReferenceSpec;
import ua.demo.agentlab.ui.testcontract.model.UiTestDataReferenceType;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DeterministicTestNgWriter {

    private static final String TEST_SOURCE_ROOT = "src/test/java";

    private final String pagePackage;
    private final String testPackage;

    public DeterministicTestNgWriter(String pagePackage, String testPackage) {
        if (pagePackage == null || pagePackage.isBlank() || testPackage == null || testPackage.isBlank()) {
            throw new IllegalArgumentException("pagePackage and testPackage cannot be blank");
        }
        this.pagePackage = pagePackage.trim();
        this.testPackage = testPackage.trim();
    }

    public DeterministicTestNgGenerationResult write(UiTestContractBundle bundle) {
        if (bundle == null || bundle.contracts().isEmpty()) {
            return new DeterministicTestNgGenerationResult(List.of(), new UiTestSourceMap(null, List.of()));
        }
        List<GeneratedSourceFile> files = new ArrayList<>();
        List<UiTestSourceMapEntry> sourceMapEntries = new ArrayList<>();
        for (UiTestContractSpec contract : bundle.contracts().stream()
                .sorted(Comparator.comparing(UiTestContractSpec::scenarioId))
                .toList()) {
            RenderedTest rendered = render(contract);
            String relativePath = TEST_SOURCE_ROOT + "/" + testPackage.replace('.', '/')
                    + "/" + contract.className() + ".java";
            files.add(new GeneratedSourceFile(testPackage, contract.className(), relativePath, rendered.source()));
            sourceMapEntries.add(new UiTestSourceMapEntry(
                    contract.scenarioId(),
                    contract.requirementIds(),
                    contract.className(),
                    relativePath,
                    rendered.testMethodLine(),
                    rendered.invocations()
            ));
        }
        return new DeterministicTestNgGenerationResult(files, new UiTestSourceMap(null, sourceMapEntries));
    }

    private RenderedTest render(UiTestContractSpec contract) {
        List<String> lines = new ArrayList<>();
        Set<String> pages = referencedPages(contract);
        Set<String> imports = imports(contract, pages);
        Map<String, UiTestDataReferenceSpec> references = dataReferences(contract);

        lines.add("package " + testPackage + ";");
        lines.add("");
        imports.stream().sorted().forEach(value -> lines.add("import " + value + ";"));
        lines.add("");
        lines.add("public class " + contract.className() + " extends BaseTest {");
        lines.add("");
        for (String page : pages) {
            lines.add("    private " + page + " " + variableName(page) + ";");
        }
        lines.add("");
        lines.add("    @BeforeMethod(alwaysRun = true)");
        lines.add("    public void setUpPageObjects() {");
        for (String page : pages) {
            lines.add("        " + variableName(page) + " = new " + page + "(driver, runtimeConfig);");
        }
        lines.add("    }");
        lines.add("");
        lines.add("    @Test(description = \"" + escape(contract.description()) + "\")");
        int testMethodLine = lines.size() + 1;
        lines.add("    public void " + contract.testMethodName() + "() {");
        for (UiTestDataReferenceSpec reference : contract.dataReferences()) {
            lines.add("        " + dataReferenceDeclaration(reference));
        }
        if (!contract.dataReferences().isEmpty()) {
            lines.add("");
        }

        List<UiTestSourceMapInvocation> invocations = new ArrayList<>();
        appendActions(lines, contract.preconditions(), references, "PRECONDITION", invocations);
        appendActions(lines, contract.actions(), references, "ACTION", invocations);
        for (UiTestAssertionSpec assertion : contract.assertions()) {
            int line = lines.size() + 1;
            lines.add("        " + renderAssertion(assertion));
            invocations.add(new UiTestSourceMapInvocation(
                    "ASSERTION",
                    assertion.page(),
                    assertion.method(),
                    List.of(assertion.requirementId()),
                    line
            ));
        }
        lines.add("    }");
        lines.add("}");
        return new RenderedTest(String.join(System.lineSeparator(), lines) + System.lineSeparator(), testMethodLine, invocations);
    }

    private void appendActions(
            List<String> lines,
            List<UiTestActionSpec> actions,
            Map<String, UiTestDataReferenceSpec> references,
            String kind,
            List<UiTestSourceMapInvocation> invocations
    ) {
        for (UiTestActionSpec action : actions) {
            int line = lines.size() + 1;
            String arguments = action.arguments().stream()
                    .map(argument -> renderArgument(argument, references))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            lines.add("        " + variableName(action.page()) + "." + action.method() + "(" + arguments + ");");
            invocations.add(new UiTestSourceMapInvocation(
                    kind,
                    action.page(),
                    action.method(),
                    action.sourceOperations(),
                    line
            ));
        }
    }

    private String renderAssertion(UiTestAssertionSpec assertion) {
        String invocation = variableName(assertion.page()) + "." + assertion.method() + "()";
        String expected = "\"" + escape(assertion.expectedValue()) + "\"";
        String message = "\"" + escape(assertion.message()) + "\"";
        return switch (assertion.mode()) {
            case TRUE -> "UiAssertions.assertTrue(" + invocation + ", " + message + ");";
            case FALSE -> "UiAssertions.assertFalse(" + invocation + ", " + message + ");";
            case EQUALS -> "UiAssertions.assertEquals(" + invocation + ", " + expected + ", " + message + ");";
            case CONTAINS -> "UiAssertions.assertContains(" + invocation + ", " + expected + ", " + message + ");";
        };
    }

    private String renderArgument(
            UiTestArgumentSpec argument,
            Map<String, UiTestDataReferenceSpec> references
    ) {
        if (argument.source() == UiTestArgumentSource.LITERAL) {
            return "\"" + escape(argument.literalValue()) + "\"";
        }
        UiTestDataReferenceSpec reference = references.get(argument.referenceId());
        if (reference == null) {
            throw new IllegalStateException("Undeclared test data reference: " + argument.referenceId());
        }
        String variable = variableName(reference.id());
        if (reference.type() == UiTestDataReferenceType.CREDENTIALS) {
            return variable + "." + argument.field() + "()";
        }
        return variable + ".required(\"" + escape(argument.field()) + "\")";
    }

    private String dataReferenceDeclaration(UiTestDataReferenceSpec reference) {
        String variable = variableName(reference.id());
        if (reference.type() == UiTestDataReferenceType.CREDENTIALS) {
            return "UserCredentials " + variable + " = credentials(\"" + escape(reference.key()) + "\");";
        }
        return "ScenarioData " + variable + " = scenarioData(\"" + escape(reference.key()) + "\");";
    }

    private Set<String> imports(UiTestContractSpec contract, Set<String> pages) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("org.testng.annotations.BeforeMethod");
        imports.add("org.testng.annotations.Test");
        imports.add("ua.demo.agentlab.core.ui.BaseTest");
        imports.add("ua.demo.agentlab.core.ui.assertions.UiAssertions");
        for (String page : pages) {
            imports.add(pagePackage + "." + page);
        }
        if (contract.dataReferences().stream().anyMatch(reference -> reference.type() == UiTestDataReferenceType.CREDENTIALS)) {
            imports.add("ua.demo.agentlab.core.data.UserCredentials");
        }
        if (contract.dataReferences().stream().anyMatch(reference -> reference.type() == UiTestDataReferenceType.SCENARIO_DATA)) {
            imports.add("ua.demo.agentlab.core.data.ScenarioData");
        }
        return imports;
    }

    private Set<String> referencedPages(UiTestContractSpec contract) {
        Set<String> pages = new LinkedHashSet<>();
        contract.preconditions().forEach(action -> pages.add(action.page()));
        contract.actions().forEach(action -> pages.add(action.page()));
        contract.assertions().forEach(assertion -> pages.add(assertion.page()));
        return pages;
    }

    private Map<String, UiTestDataReferenceSpec> dataReferences(UiTestContractSpec contract) {
        Map<String, UiTestDataReferenceSpec> references = new LinkedHashMap<>();
        contract.dataReferences().forEach(reference -> references.put(reference.id(), reference));
        return references;
    }

    private String variableName(String typeOrId) {
        String typeName = typeName(typeOrId);
        return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
    }

    private String typeName(String value) {
        StringBuilder result = new StringBuilder();
        for (String token : (value == null ? "" : value).split("[^A-Za-z0-9]+")) {
            if (token.isBlank()) {
                continue;
            }
            result.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                result.append(token.substring(1));
            }
        }
        return result.length() == 0 ? "GeneratedValue" : result.toString();
    }

    private String escape(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private record RenderedTest(
            String source,
            int testMethodLine,
            List<UiTestSourceMapInvocation> invocations
    ) {
    }
}
