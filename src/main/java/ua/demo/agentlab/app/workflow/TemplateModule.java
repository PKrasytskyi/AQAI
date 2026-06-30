package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.ui.selenium.writer.TemplateDrivenSeleniumWriter;

public record TemplateModule(
        TemplateDrivenSeleniumWriter seleniumWriter,
        WorkflowAgent pageObjectWriterAgent,
        WorkflowAgent layeredUiTestWriterAgent
) {
}
