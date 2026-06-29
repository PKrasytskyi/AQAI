package ua.demo.agentlab.ui.selenium.writer;

import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.writer.PageObjectWriter;

import java.util.List;

public class SeleniumTemplatePageObjectWriter implements PageObjectWriter {

    private final TemplateDrivenSeleniumWriter writer;

    public SeleniumTemplatePageObjectWriter(TemplateDrivenSeleniumWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        this.writer = writer;
    }

    @Override
    public List<GeneratedSourceFile> write(UiTestPlan uiTestPlan) {
        return writer.writePageObjects(uiTestPlan);
    }
}
