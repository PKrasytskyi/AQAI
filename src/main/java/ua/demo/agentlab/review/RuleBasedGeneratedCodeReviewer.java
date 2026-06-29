package ua.demo.agentlab.review;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedGeneratedCodeReviewer implements GeneratedCodeReviewer {

    @Override
    public GeneratedCodeReviewReport review(WorkflowState state) {
        List<GeneratedCodeReviewFinding> findings = new ArrayList<>();

        List<GeneratedSourceFile> files = new ArrayList<>();
        files.addAll(state.getPageObjectFiles());
        files.addAll(state.getUiTestFiles());

        for (GeneratedSourceFile file : files) {
            findings.addAll(reviewFile(file));
        }

        String summary = findings.isEmpty()
                ? "Generated code review completed without findings"
                : "Generated code review found " + findings.size() + " issue(s)";

        return new GeneratedCodeReviewReport(
                summary,
                files.size(),
                findings.size(),
                findings
        );
    }

    private List<GeneratedCodeReviewFinding> reviewFile(GeneratedSourceFile file) {
        List<GeneratedCodeReviewFinding> findings = new ArrayList<>();
        String content = file.content();

        detectHardcodedBaseUrl(file, content, findings);
        detectPlaceholderComments(file, content, findings);
        detectRepeatedCredentials(file, content, findings);
        detectWeakAssertions(file, content, findings);

        return findings;
    }

    private void detectHardcodedBaseUrl(
            GeneratedSourceFile file,
            String content,
            List<GeneratedCodeReviewFinding> findings
    ) {
        if (content.contains("http://localhost:8080") || content.contains("https://localhost:8080")) {
            findings.add(new GeneratedCodeReviewFinding(
                    file.relativePath(),
                    ReviewSeverity.WARNING,
                    "HARDCODED_BASE_URL",
                    "Generated code contains a hardcoded base URL.",
                    "Move the base URL to config, environment variables, or a shared test fixture."
            ));
        }
    }

    private void detectPlaceholderComments(
            GeneratedSourceFile file,
            String content,
            List<GeneratedCodeReviewFinding> findings
    ) {
        if (content.contains("Replace with")) {
            findings.add(new GeneratedCodeReviewFinding(
                    file.relativePath(),
                    ReviewSeverity.INFO,
                    "PLACEHOLDER_ASSERTION",
                    "Generated code still contains placeholder guidance comments.",
                    "Replace placeholder comments with product-specific assertions."
            ));
        }
    }

    private void detectRepeatedCredentials(
            GeneratedSourceFile file,
            String content,
            List<GeneratedCodeReviewFinding> findings
    ) {
        if (content.contains("user@example.com") || content.contains("Password123")) {
            findings.add(new GeneratedCodeReviewFinding(
                    file.relativePath(),
                    ReviewSeverity.WARNING,
                    "HARDCODED_TEST_DATA",
                    "Generated code contains hardcoded test credentials.",
                    "Move test data to constants, fixtures, files, environments, or a dedicated test data provider."
            ));
        }
    }

    private void detectWeakAssertions(
            GeneratedSourceFile file,
            String content,
            List<GeneratedCodeReviewFinding> findings
    ) {
        boolean reliesOnUrlOnly = content.contains("hasURL(")
                && !content.contains("isVisible()")
                && !content.contains("containsText(")
                && !content.contains("hasText(");

        if (reliesOnUrlOnly) {
            findings.add(new GeneratedCodeReviewFinding(
                    file.relativePath(),
                    ReviewSeverity.INFO,
                    "WEAK_ASSERTION_SET",
                    "Generated test relies mostly on URL assertions.",
                    "Add UI-state assertions such as visible messages, page title, or component state."
            ));
        }
    }
}
