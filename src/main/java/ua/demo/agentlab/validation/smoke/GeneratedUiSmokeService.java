package ua.demo.agentlab.validation.smoke;

import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.review.GeneratedCodeReviewFinding;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.ReviewSeverity;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;

import java.util.ArrayList;
import java.util.List;

public class GeneratedUiSmokeService {

    public GeneratedUiSmokeResult smoke(
            GeneratedUiSources sources,
            List<String> persistedFiles,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport
    ) {
        List<GeneratedUiSmokeIssue> issues = new ArrayList<>();
        List<GeneratedSourceFile> pageObjects = sources == null ? List.of() : sources.pageObjectFiles();
        List<String> persisted = persistedFiles == null ? List.of() : persistedFiles;
        if (pageObjects.isEmpty()) {
            issues.add(issue("BLOCKER", "SMOKE_PAGE_OBJECT_SOURCES_PRESENT", "", "No generated page object sources were available for smoke validation"));
        }
        if (persisted.isEmpty()) {
            issues.add(issue("BLOCKER", "SMOKE_PERSISTED_SOURCES_PRESENT", "", "No persisted generated source files were available"));
        }
        if (compileResult == null) {
            issues.add(issue("BLOCKER", "SMOKE_COMPILE_RESULT_PRESENT", "", "Compile result is missing"));
        } else if (!compileResult.isPassed()) {
            issues.add(issue("BLOCKER", "SMOKE_COMPILE_PASSED", "", "Generated code compile gate did not pass: " + compileResult.summary()));
        }
        if (reviewReport == null) {
            issues.add(issue("WARNING", "SMOKE_REVIEW_RESULT_PRESENT", "", "Review result is missing"));
        } else {
            for (GeneratedCodeReviewFinding finding : reviewReport.findings()) {
                if (finding.severity() == ReviewSeverity.CRITICAL) {
                    issues.add(issue("BLOCKER", "SMOKE_REVIEW_NO_ERRORS", finding.filePath(), finding.message()));
                }
            }
        }
        for (GeneratedSourceFile file : pageObjects) {
            validatePageObject(file, issues);
        }
        boolean hasBlocker = issues.stream().anyMatch(issue -> "BLOCKER".equalsIgnoreCase(issue.severity()));
        GeneratedUiSmokeStatus status = hasBlocker
                ? GeneratedUiSmokeStatus.FAILED
                : pageObjects.isEmpty() ? GeneratedUiSmokeStatus.SKIPPED : GeneratedUiSmokeStatus.PASSED;
        String summary = switch (status) {
            case PASSED -> "Generated UI smoke validation passed for " + pageObjects.size() + " page object file(s)";
            case FAILED -> "Generated UI smoke validation failed with " + issues.size() + " issue(s)";
            case SKIPPED -> "Generated UI smoke validation skipped because no page object sources were available";
        };
        return new GeneratedUiSmokeResult(status, summary, pageObjects.size(), issues);
    }

    private void validatePageObject(GeneratedSourceFile file, List<GeneratedUiSmokeIssue> issues) {
        if (file == null) {
            return;
        }
        String content = file.content() == null ? "" : file.content();
        if (!content.contains("package ")) {
            issues.add(issue("BLOCKER", "SMOKE_PACKAGE_DECLARATION_PRESENT", file.relativePath(), "Generated page object is missing a package declaration"));
        }
        if (!content.contains("class " + file.className())) {
            issues.add(issue("BLOCKER", "SMOKE_CLASS_DECLARATION_PRESENT", file.relativePath(), "Generated page object class declaration does not match metadata"));
        }
        if (!content.contains("extends BasePage")) {
            issues.add(issue("BLOCKER", "SMOKE_EXTENDS_BASE_PAGE", file.relativePath(), "Generated page object must extend BasePage"));
        }
        if (!hasSupportedConstructor(content, file.className())) {
            issues.add(issue("BLOCKER", "SMOKE_WEBDRIVER_CONSTRUCTOR_PRESENT", file.relativePath(), "Generated page object must expose WebDriver constructor"));
        }
        if (content.contains("return !getCurrentUrl().isBlank()")) {
            issues.add(issue("BLOCKER", "SMOKE_NO_WEAK_URL_NONBLANK_ASSERTION", file.relativePath(), "Generated page object contains weak nonblank URL assertion"));
        }
    }

    private boolean hasSupportedConstructor(String content, String className) {
        if (content == null || className == null || className.isBlank()) {
            return false;
        }
        String compact = content.replaceAll("\\s+", " ");
        String simpleConstructor = "public " + className + "(WebDriver driver)";
        String runtimeConstructor = "public " + className + "(WebDriver driver, UiRuntimeConfig runtimeConfig)";
        return compact.contains(simpleConstructor) || compact.contains(runtimeConstructor);
    }

    private GeneratedUiSmokeIssue issue(String severity, String ruleId, String filePath, String message) {
        return new GeneratedUiSmokeIssue(severity, ruleId, filePath, message);
    }
}
