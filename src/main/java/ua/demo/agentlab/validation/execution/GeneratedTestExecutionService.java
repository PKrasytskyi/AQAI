package ua.demo.agentlab.validation.execution;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ua.demo.agentlab.persistence.GeneratedSourceKind;
import ua.demo.agentlab.persistence.GeneratedSourceManifest;
import ua.demo.agentlab.persistence.GeneratedSourceManifestEntry;
import ua.demo.agentlab.ui.testcontract.writer.UiTestSourceMap;
import ua.demo.agentlab.ui.testcontract.writer.UiTestSourceMapEntry;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Executes only current-run TestNG classes declared by the generated source manifest. */
public final class GeneratedTestExecutionService {

    private final Path workingDirectory;
    private final Path reportDirectory;

    public GeneratedTestExecutionService(Path workingDirectory) {
        Path root = workingDirectory == null ? Path.of("") : workingDirectory;
        this.workingDirectory = root.toAbsolutePath().normalize();
        this.reportDirectory = this.workingDirectory.resolve(
                Path.of("target", "ai-run", "validation", "generated-test-reports")
        ).normalize();
    }

    public GeneratedTestExecutionResult execute(GeneratedSourceManifest manifest) {
        return execute(manifest, new UiTestSourceMap("ui-test-source-map.v1", List.of()));
    }

    public GeneratedTestExecutionResult execute(GeneratedSourceManifest manifest, UiTestSourceMap sourceMap) {
        if (manifest == null) {
            throw new IllegalArgumentException("generated source manifest cannot be null");
        }
        List<String> testClasses = manifest.files().stream()
                .filter(entry -> entry.kind() == GeneratedSourceKind.UI_TEST)
                .map(entry -> entry.packageName() + "." + entry.className())
                .sorted()
                .toList();
        if (testClasses.isEmpty()) {
            return failed(testClasses, "Current-run manifest contains no generated UI tests", "", List.of());
        }

        long startedAt = System.nanoTime();
        try {
            recreateReportDirectory();
            ProcessBuilder builder = new ProcessBuilder(command(manifest, testClasses));
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            output = compactOutput(output);
            int exitCode = process.waitFor();
            TestCounts counts = readReports();
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            if (exitCode != 0 && unavailable(output)) {
                return new GeneratedTestExecutionResult(
                        GeneratedTestExecutionResult.SCHEMA_VERSION,
                        GeneratedTestExecutionStatus.UNAVAILABLE,
                        counts.total(),
                        counts.passed(),
                        counts.failed(),
                        counts.skipped(),
                        durationMillis,
                        testClasses,
                        testResults(sourceMap, counts.cases()),
                        counts.failures(),
                        relativeReportDirectory(),
                        "Generated test execution is unavailable in the local environment",
                        output
                );
            }
            boolean complete = counts.total() >= testClasses.size();
            GeneratedTestExecutionStatus status = exitCode == 0 && complete && counts.failed() == 0
                    ? GeneratedTestExecutionStatus.PASSED
                    : GeneratedTestExecutionStatus.FAILED;
            String summary = status == GeneratedTestExecutionStatus.PASSED
                    ? "Manifest-owned generated TestNG tests passed"
                    : !complete
                    ? "Generated test reports are incomplete: expected at least " + testClasses.size()
                            + " tests but found " + counts.total()
                    : "Manifest-owned generated TestNG tests failed";
            return new GeneratedTestExecutionResult(
                    GeneratedTestExecutionResult.SCHEMA_VERSION,
                    status,
                    counts.total(),
                    counts.passed(),
                    counts.failed(),
                    counts.skipped(),
                    durationMillis,
                    testClasses,
                    testResults(sourceMap, counts.cases()),
                    counts.failures(),
                    relativeReportDirectory(),
                    summary,
                    output
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failed(testClasses, "Generated test execution was interrupted", exception.getMessage(), List.of());
        } catch (Exception exception) {
            return failed(testClasses, "Generated test execution could not be completed", exception.getMessage(), List.of());
        }
    }

    private List<String> command(GeneratedSourceManifest manifest, List<String> testClasses) {
        Path sourceRoot = commonSourceRoot(manifest.files());
        return List.of(
                isWindows() ? "mvn.cmd" : "mvn",
                "--batch-mode",
                "-Duser.home=.",
                "-Dmaven.repo.local=.m2repo",
                "-Dagentlab.testSourceDirectory=" + sourceRoot,
                "-Dsurefire.excludes=",
                "-Dagentlab.surefireReportsDirectory=" + reportDirectory,
                "-Dtest=" + String.join(",", testClasses),
                "test"
        );
    }

    public Path commonSourceRoot(List<GeneratedSourceManifestEntry> entries) {
        List<Path> parents = entries.stream()
                .map(GeneratedSourceManifestEntry::relativePath)
                .map(workingDirectory::resolve)
                .map(Path::normalize)
                .map(Path::getParent)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (parents.isEmpty()) {
            throw new IllegalArgumentException("Manifest source paths must have parent directories");
        }
        Path common = parents.get(0);
        for (int index = 1; index < parents.size(); index++) {
            common = commonAncestor(common, parents.get(index));
        }
        Path testSourceRoot = workingDirectory.resolve(Path.of("src", "test", "java")).normalize();
        if (!common.startsWith(testSourceRoot) || common.equals(testSourceRoot)) {
            throw new IllegalArgumentException(
                    "Manifest sources must share a project-specific namespace below " + testSourceRoot
            );
        }
        return common;
    }

    private Path commonAncestor(Path left, Path right) {
        Path current = left;
        while (current != null && !right.startsWith(current)) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalArgumentException("Manifest source paths do not share a common directory");
        }
        return current;
    }

    private TestCounts readReports() throws Exception {
        if (!Files.isDirectory(reportDirectory)) {
            return TestCounts.empty();
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        int total = 0;
        int failed = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        Map<String, ParsedTestCase> parsedCases = new LinkedHashMap<>();
        try (var paths = Files.list(reportDirectory)) {
            for (Path report : paths.filter(path -> path.getFileName().toString().startsWith("TEST-"))
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .sorted()
                    .toList()) {
                Document document = factory.newDocumentBuilder().parse(report.toFile());
                Element suite = document.getDocumentElement();
                total += integer(suite, "tests");
                failed += integer(suite, "failures") + integer(suite, "errors");
                skipped += integer(suite, "skipped");
                NodeList cases = suite.getElementsByTagName("testcase");
                for (int index = 0; index < cases.getLength(); index++) {
                    Element testCase = (Element) cases.item(index);
                    String className = simpleName(testCase.getAttribute("classname"));
                    boolean skippedCase = testCase.getElementsByTagName("skipped").getLength() > 0;
                    boolean failedCase = testCase.getElementsByTagName("failure").getLength() > 0
                            || testCase.getElementsByTagName("error").getLength() > 0;
                    String failure = failedCase ? failureText(testCase) : "";
                    parsedCases.put(className, new ParsedTestCase(
                            failedCase ? "FAILED" : skippedCase ? "SKIPPED" : "PASSED",
                            millis(testCase.getAttribute("time")),
                            failure,
                            report.toString().replace('\\', '/')
                    ));
                    if (failedCase) {
                        failures.add(testCase.getAttribute("classname") + "#" + testCase.getAttribute("name"));
                    }
                }
            }
        }
        return new TestCounts(total, Math.max(0, total - failed - skipped), failed, skipped, failures, parsedCases);
    }

    private List<GeneratedTestCaseExecutionResult> testResults(
            UiTestSourceMap sourceMap,
            Map<String, ParsedTestCase> parsedCases
    ) {
        if (sourceMap == null) return List.of();
        return sourceMap.entries().stream().map(entry -> {
            ParsedTestCase parsed = parsedCases.getOrDefault(entry.className(), ParsedTestCase.missing());
            List<String> sourceIds = entry.invocations().stream()
                    .flatMap(invocation -> invocation.sourceIds().stream())
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .toList();
            return new GeneratedTestCaseExecutionResult(
                    entry.scenarioId(),
                    entry.requirementIds(),
                    entry.className(),
                    entry.relativePath(),
                    parsed.status(),
                    parsed.durationMillis(),
                    parsed.failure(),
                    sourceIds,
                    parsed.reportPath().isBlank() ? List.of() : List.of(parsed.reportPath())
            );
        }).toList();
    }

    private String simpleName(String className) {
        int separator = className == null ? -1 : className.lastIndexOf('.');
        return separator < 0 ? (className == null ? "" : className) : className.substring(separator + 1);
    }

    private long millis(String seconds) {
        try {
            return Math.round(Double.parseDouble(seconds) * 1000.0d);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String failureText(Element testCase) {
        NodeList failures = testCase.getElementsByTagName("failure");
        if (failures.getLength() == 0) failures = testCase.getElementsByTagName("error");
        if (failures.getLength() == 0) return "";
        String message = ((Element) failures.item(0)).getAttribute("message");
        return message == null || message.isBlank() ? failures.item(0).getTextContent().trim() : message.trim();
    }

    private int integer(Element element, String attribute) {
        try {
            return Integer.parseInt(element.getAttribute(attribute));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void recreateReportDirectory() throws IOException {
        if (Files.exists(reportDirectory)) {
            try (var paths = Files.walk(reportDirectory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(reportDirectory);
    }

    private String relativeReportDirectory() {
        return workingDirectory.relativize(reportDirectory).toString().replace('\\', '/');
    }

    private boolean unavailable(String output) {
        return output != null && (output.contains("Access is denied.") || output.contains("AccessDeniedException"));
    }

    private String compactOutput(String output) {
        String value = output == null ? "" : output;
        int maxChars = 20_000;
        return value.length() <= maxChars
                ? value
                : "[truncated to last " + maxChars + " characters]\n" + value.substring(value.length() - maxChars);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private GeneratedTestExecutionResult failed(
            List<String> testClasses,
            String summary,
            String output,
            List<String> failures
    ) {
        return new GeneratedTestExecutionResult(
                GeneratedTestExecutionResult.SCHEMA_VERSION,
                GeneratedTestExecutionStatus.FAILED,
                0,
                0,
                Math.max(1, failures.size()),
                0,
                0L,
                testClasses,
                List.of(),
                failures,
                relativeReportDirectory(),
                summary,
                output
        );
    }

    private record TestCounts(
            int total,
            int passed,
            int failed,
            int skipped,
            List<String> failures,
            Map<String, ParsedTestCase> cases
    ) {
        private static TestCounts empty() {
            return new TestCounts(0, 0, 0, 0, List.of(), Map.of());
        }
    }

    private record ParsedTestCase(String status, long durationMillis, String failure, String reportPath) {
        private static ParsedTestCase missing() {
            return new ParsedTestCase("MISSING", 0L, "No Surefire report found", "");
        }
    }
}
