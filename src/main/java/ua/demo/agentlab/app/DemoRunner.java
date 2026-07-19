package ua.demo.agentlab.app;

import ua.demo.agentlab.app.workflow.AppBootstrap;
import ua.demo.agentlab.app.workflow.WorkflowDefinition;
import ua.demo.agentlab.orchestration.AgentOrchestrator;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.reporting.ConsoleReportPrinter;

public class DemoRunner {

    private static final ConsoleReportPrinter REPORT_PRINTER = new ConsoleReportPrinter();

    public static void main(String[] args) {
        WorkflowDefinition workflow = new AppBootstrap().createWorkflow(args);
        WorkflowState result = new AgentOrchestrator(workflow.agents()).run(workflow.initialState());

        if (result.isFailed()) {
            REPORT_PRINTER.printWorkflowFailure(result);
            throw new IllegalStateException(result.getFailureReason());
        }

        REPORT_PRINTER.printWorkflowSummary(result);
    }
}
