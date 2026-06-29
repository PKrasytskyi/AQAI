package ua.demo.agentlab.reporting;

import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;

public class ConsoleReportPrinter {

    public void printWorkflowFailure(WorkflowState result) {
        System.out.println("Workflow failed: " + result.getFailureReason());
    }

    public void printWorkflowSummary(WorkflowState result) {
        printAudit(result);
        printFindings(result);
        printTestPlanSummary(result.getTestPlan());
        printCanonicalTestCaseSummary(result.getCanonicalTestCaseBundle());
        printDiscoverySummary(result);
        printUiTestPlanSummary(result.getUiTestPlan());
        printAiArtifactsSummary(result);
        printGeneratedFilesSummary(result);
        printGeneratedUiContractValidationSummary(result);
        printGeneratedCodeValidationSummary(result);
        printGeneratedCodeReviewSummary(result);
    }

    private void printAudit(WorkflowState result) {
        System.out.println("\n=== Audit ===");
        result.getAuditTrail().forEach(item -> System.out.println("- " + item));
    }

    private void printFindings(WorkflowState result) {
        System.out.println("\n=== Findings ===");
        if (result.getFindings().isEmpty()) {
            System.out.println("- No findings");
            return;
        }

        result.getFindings().forEach(item -> System.out.println("- " + item));
    }

    private void printTestPlanSummary(TestPlan testPlan) {
        if (testPlan == null) {
            return;
        }

        System.out.println("\n=== Test Plan Summary ===");
        System.out.println("Objective: " + testPlan.objective());
        System.out.println("Source: " + testPlan.source());
        System.out.println("Functional areas: " + testPlan.functionalAreas().size());
        System.out.println("Scenarios: " + testPlan.scenarios().size());
        System.out.println("Assumptions: " + testPlan.assumptions().size());
        System.out.println("Risks: " + testPlan.risks().size());

        System.out.println("\n=== Functional Areas ===");
        if (testPlan.functionalAreas().isEmpty()) {
            System.out.println("- No functional areas");
        } else {
            testPlan.functionalAreas().forEach(area ->
                    System.out.println("- " + area.name() + " | " + area.description())
            );
        }

        System.out.println("\n=== Scenarios ===");
        if (testPlan.scenarios().isEmpty()) {
            System.out.println("- No scenarios");
        } else {
            testPlan.scenarios().forEach(scenario ->
                    System.out.println("- " + scenario.id()
                            + " | " + scenario.title()
                            + " | " + scenario.type()
                            + " | " + scenario.priority()
                            + " | " + scenario.sourceReference())
            );
        }

        System.out.println("\n=== Assumptions ===");
        if (testPlan.assumptions().isEmpty()) {
            System.out.println("- No assumptions");
        } else {
            testPlan.assumptions().forEach(item -> System.out.println("- " + item));
        }

        System.out.println("\n=== Risks ===");
        if (testPlan.risks().isEmpty()) {
            System.out.println("- No risks");
        } else {
            testPlan.risks().forEach(item -> System.out.println("- " + item));
        }
    }

    private void printUiTestPlanSummary(UiTestPlan uiTestPlan) {
        if (uiTestPlan == null) {
            return;
        }

        System.out.println("\n=== UI Test Plan Summary ===");
        System.out.println("Source test plan: " + uiTestPlan.sourceTestPlan());
        System.out.println("Primary page: " + uiTestPlan.targetPage());
        System.out.println("Pages: " + String.join(", ", uiTestPlan.pageNames()));
        System.out.println("UI scenarios: " + uiTestPlan.scenarios().size());

        System.out.println("\n=== UI Scenarios ===");
        if (uiTestPlan.scenarios().isEmpty()) {
            System.out.println("- No UI scenarios");
            return;
        }

        uiTestPlan.scenarios().forEach(scenario ->
                System.out.println("- " + scenario.id()
                        + " | " + scenario.title()
                        + " | " + scenario.canonicalFlowType()
                        + " | source=" + scenario.sourcePageName()
                        + " | " + scenario.pageName()
                        + " | " + scenario.route()
                        + " | auth=" + scenario.prerequisite().authenticationRequired()
                        + " | assertions=" + scenario.assertionProfile()
                        + " | " + scenario.sourceReference())
        );
    }

    private void printCanonicalTestCaseSummary(CanonicalTestCaseBundle bundle) {
        if (bundle == null) {
            return;
        }

        System.out.println("\n=== Canonical Test Case Summary ===");
        System.out.println("Source: " + bundle.source());
        System.out.println("Primary page: " + bundle.primaryPage());
        System.out.println("Pages: " + String.join(", ", bundle.pageNames()));
        System.out.println("Test cases: " + bundle.testCases().size());

        if (bundle.testCases().isEmpty()) {
            return;
        }

        System.out.println("\n=== Canonical Test Cases ===");
        bundle.testCases().forEach(testCase ->
                System.out.println("- " + testCase.id()
                        + " | " + testCase.title()
                        + " | refs=" + testCase.requirementRefs()
                        + " | targetPages=" + testCase.targetPages()
                        + " | " + testCase.sourceReference())
        );
    }

    private void printDiscoverySummary(WorkflowState result) {
        if (result.getUiDiscoverySnapshot() == null) {
            return;
        }

        System.out.println("\n=== UI Discovery Summary ===");
        System.out.println("Discovered pages: " + result.getUiDiscoverySnapshot().pages().size());
        System.out.println("Discovered flows: " + result.getUiDiscoverySnapshot().flows().size());
        if (result.getSeleniumDiscoveryResult() != null) {
            System.out.println("Raw Selenium pages: " + result.getSeleniumDiscoveryResult().pages().size());
            System.out.println("Raw Selenium transitions: " + result.getSeleniumDiscoveryResult().transitions().size());
            long evidencePages = result.getSeleniumDiscoveryResult().pages().stream()
                    .filter(page -> page.evidence() != null)
                    .count();
            System.out.println("Evidence pages: " + evidencePages);
        }

        if (!result.getDiscoveryArtifactFiles().isEmpty()) {
            System.out.println("\n=== UI Discovery Artifacts ===");
            result.getDiscoveryArtifactFiles().forEach(path -> System.out.println("- " + path));
        }
    }

    private void printGeneratedFilesSummary(WorkflowState result) {
        if (result.getPageObjectFiles().isEmpty() && result.getUiTestFiles().isEmpty() && result.getWrittenFiles().isEmpty()) {
            return;
        }

        System.out.println("\n=== Generated Files Summary ===");
        System.out.println("Page object files: " + result.getPageObjectFiles().size());
        System.out.println("UI test files: " + result.getUiTestFiles().size());
        System.out.println("Written files: " + result.getWrittenFiles().size());

        if (!result.getPageObjectFiles().isEmpty()) {
            System.out.println("\n=== Page Object Files ===");
            result.getPageObjectFiles().forEach(file ->
                    System.out.println("- " + file.relativePath())
            );
        }

        if (!result.getUiTestFiles().isEmpty()) {
            System.out.println("\n=== UI Test Files ===");
            result.getUiTestFiles().forEach(file ->
                    System.out.println("- " + file.relativePath())
            );
        }

        if (!result.getWrittenFiles().isEmpty()) {
            System.out.println("\n=== Written Files ===");
            result.getWrittenFiles().forEach(path ->
                    System.out.println("- " + path)
            );
        }
    }

    private void printAiArtifactsSummary(WorkflowState result) {
        if (result.getAiArtifactFiles().isEmpty()) {
            return;
        }

        System.out.println("\n=== AI Artifacts ===");
        result.getAiArtifactFiles().forEach(path -> System.out.println("- " + path));
    }

    private void printGeneratedUiContractValidationSummary(WorkflowState result) {
        if (result.getGeneratedUiContractValidationResult() == null) {
            return;
        }

        var validation = result.getGeneratedUiContractValidationResult();

        System.out.println("\n=== Generated UI Contract Validation ===");
        System.out.println("Status: " + validation.status());
        System.out.println("Summary: " + validation.summary());

        if (validation.violations().isEmpty()) {
            return;
        }

        System.out.println("\n=== UI Contract Violations ===");
        validation.violations().forEach(violation -> System.out.println("- " + violation));
    }

    private void printGeneratedCodeValidationSummary(WorkflowState result) {
        if (result.getGeneratedCodeValidationResult() == null) {
            return;
        }

        var validation = result.getGeneratedCodeValidationResult();

        System.out.println("\n=== Generated Code Validation ===");
        System.out.println("Status: " + validation.status());
        System.out.println("Summary: " + validation.summary());
    }

    private void printGeneratedCodeReviewSummary(WorkflowState result) {
        if (result.getGeneratedCodeReviewReport() == null) {
            return;
        }

        var review = result.getGeneratedCodeReviewReport();

        System.out.println("\n=== Generated Code Review ===");
        System.out.println("Reviewed files: " + review.reviewedFiles());
        System.out.println("Findings: " + review.totalFindings());
        System.out.println("Summary: " + review.summary());

        if (!review.hasFindings()) {
            return;
        }

        System.out.println("\n=== Review Findings ===");
        review.findings().forEach(finding ->
                System.out.println("- " + finding.severity()
                        + " | " + finding.ruleId()
                        + " | " + finding.filePath()
                        + " | " + finding.message())
        );
    }
}
