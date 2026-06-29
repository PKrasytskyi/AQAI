package ua.demo.agentlab.ui.writer;

import ua.demo.agentlab.ui.UiTestPlan;

import java.util.List;

public interface PageObjectWriter {

    List<GeneratedSourceFile> write(UiTestPlan uiTestPlan);
}
