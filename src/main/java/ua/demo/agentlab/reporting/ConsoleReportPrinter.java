package ua.demo.agentlab.reporting;

import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.WorkflowPipelineSnapshot;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;

public class ConsoleReportPrinter {

    public void printWorkflowFailure(WorkflowState result) {
        printWorkflowFailure(WorkflowPipelineSnapshot.from(result));
    }

    public void printWorkflowFailure(WorkflowPipelineSnapshot snapshot) {
        System.out.println("Workflow failed: " + snapshot.runEnvelope().failureReason());
    }

    public void printWorkflowSummary(WorkflowState result) {
        printWorkflowSummary(WorkflowPipelineSnapshot.from(result));
    }

    public void printWorkflowSummary(WorkflowPipelineSnapshot snapshot) {
        printAudit(snapshot);
        printFindings(snapshot);
        printTestPlanSummary(snapshot.requirements().testPlan());
        printCanonicalTestCaseSummary(snapshot.mapping().canonicalTestCaseBundle());
        printDiscoverySummary(snapshot);
        printUiTestPlanSummary(snapshot.mapping().uiTestPlan());
        printAiArtifactsSummary(snapshot);
        printGeneratedFilesSummary(snapshot);
        printGeneratedUiContractValidationSummary(snapshot);
        printGeneratedCodeValidationSummary(snapshot);
        printGeneratedCodeReviewSummary(snapshot);
    }

    private void printAudit(WorkflowPipelineSnapshot snapshot) {
        System.out.println("\n=== Audit ===");
        snapshot.runEnvelope().auditTrail().forEach(item -> System.out.println("- " + item));
    }

    private void printFindings(WorkflowPipelineSnapshot snapshot) {
        System.out.println("\n=== Findings ===");
        if (snapshot.runEnvelope().findingsList().isEmpty()) {
            System.out.println("- No findings");
            return;
        }

        snapshot.runEnvelope().findingsList().forEach(item -> System.out.println("- " + item));
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

    private void printDiscoverySummary(WorkflowPipelineSnapshot snapshot) {
        if (snapshot.discovery().uiDiscoverySnapshot() == null) {
            return;
        }

        System.out.println("\n=== UI Discovery Summary ===");
        System.out.println("Discovered pages: " + snapshot.discovery().uiDiscoverySnapshot().pages().size());
        System.out.println("Discovered flows: " + snapshot.discovery().uiDiscoverySnapshot().flows().size());
        if (snapshot.discovery().seleniumDiscoveryResult() != null) {
            System.out.println("Raw Selenium pages: " + snapshot.discovery().seleniumDiscoveryResult().pages().size());
            System.out.println("Raw Selenium transitions: " + snapshot.discovery().seleniumDiscoveryResult().transitions().size());
            long evidencePages = snapshot.discovery().seleniumDiscoveryResult().pages().stream()
                    .filter(page -> page.evidence() != null)
                    .count();
            System.out.println("Evidence pages: " + evidencePages);
        }

        if (!snapshot.generation().discoveryArtifactFiles().isEmpty()) {
            System.out.println("\n=== UI Discovery Artifacts ===");
            snapshot.generation().discoveryArtifactFiles().forEach(path -> System.out.println("- " + path));
        }
    }

    private void printGeneratedFilesSummary(WorkflowPipelineSnapshot snapshot) {
        if (snapshot.generation().pageObjectFiles().isEmpty()
                && snapshot.generation().uiTestFiles().isEmpty()
                && snapshot.generation().writtenFiles().isEmpty()) {
            return;
        }

        System.out.println("\n=== Generated Files Summary ===");
        System.out.println("Page object files: " + snapshot.generation().pageObjectFiles().size());
        System.out.println("UI test files: " + snapshot.generation().uiTestFiles().size());
        System.out.println("Written files: " + snapshot.generation().writtenFiles().size());

        if (!snapshot.generation().pageObjectFiles().isEmpty()) {
            System.out.println("\n=== Page Object Files ===");
            snapshot.generation().pageObjectFiles().forEach(file ->
                    System.out.println("- " + file.relativePath())
            );
        }

        if (!snapshot.generation().uiTestFiles().isEmpty()) {
            System.out.println("\n=== UI Test Files ===");
            snapshot.generation().uiTestFiles().forEach(file ->
                    System.out.println("- " + file.relativePath())
            );
        }

        if (!snapshot.generation().writtenFiles().isEmpty()) {
            System.out.println("\n=== Written Files ===");
            snapshot.generation().writtenFiles().forEach(path ->
                    System.out.println("- " + path)
            );
        }
    }

    private void printAiArtifactsSummary(WorkflowPipelineSnapshot snapshot) {
        if (snapshot.generation().aiArtifactFiles().isEmpty()) {
            return;
        }

        System.out.println("\n=== AI Artifacts ===");
        snapshot.generation().aiArtifactFiles().forEach(path -> System.out.println("- " + path));
    }

    private void printGeneratedUiContractValidationSummary(WorkflowPipelineSnapshot snapshot) {
        if (snapshot.generation().generatedUiContractValidationResult() == null) {
            return;
        }

        var validation = snapshot.generation().generatedUiContractValidationResult();

        System.out.println("\n=== Generated UI Contract Validation ===");
        System.out.println("Status: " + validation.status());
        System.out.println("Summary: " + validation.summary());

        if (validation.violations().isEmpty()) {
            return;
        }

        System.out.println("\n=== UI Contract Violations ===");
        validation.violations().forEach(violation -> System.out.println("- " + violation));
    }

    private void printGeneratedCodeValidationSummary(WorkflowPipelineSnapshot snapshot) {
        if (snapshot.generation().generatedCodeValidationResult() == null) {
            return;
        }

        var validation = snapshot.generation().generatedCodeValidationResult();

        System.out.println("\n=== Generated Code Validation ===");
        System.out.println("Status: " + validation.status());
        System.out.println("Summary: " + validation.summary());
    }

    private void printGeneratedCodeReviewSummary(WorkflowPipelineSnapshot snapshot) {
        if (snapshot.generation().generatedCodeReviewReport() == null) {
            return;
        }

        var review = snapshot.generation().generatedCodeReviewReport();

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
