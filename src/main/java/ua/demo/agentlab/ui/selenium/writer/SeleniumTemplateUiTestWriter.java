package ua.demo.agentlab.ui.selenium.writer;

import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.writer.UiTestWriter;

import java.util.List;

public class SeleniumTemplateUiTestWriter implements UiTestWriter {

    private final TemplateDrivenSeleniumWriter writer;

    public SeleniumTemplateUiTestWriter(TemplateDrivenSeleniumWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        this.writer = writer;
    }

    @Override
    public List<GeneratedSourceFile> write(UiTestPlan uiTestPlan) {
        return writer.writeTests(uiTestPlan);
    }
}
