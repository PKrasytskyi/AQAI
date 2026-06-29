package ua.demo.agentlab.ui.writer;

import ua.demo.agentlab.ui.UiTestPlan;

import java.util.List;

public interface UiTestWriter {

    List<GeneratedSourceFile> write(UiTestPlan uiTestPlan);
}
