package ua.demo.agentlab.persistence;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

public class LocalFilePersistenceAgent implements WorkflowAgent {

    private final GeneratedFileWriter generatedFileWriter;

    public LocalFilePersistenceAgent(GeneratedFileWriter generatedFileWriter){
        this.generatedFileWriter = generatedFileWriter;
    }

    @Override
    public String name() {
        return "file-persistence-agent";
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return !state.getPageObjectFiles().isEmpty() || !state.getUiTestFiles().isEmpty();
    }

    @Override
    public void execute(WorkflowState state) {

        for (GeneratedSourceFile file : state.getPageObjectFiles()) {
            generatedFileWriter.write(file);
            state.addWrittenFile(file.relativePath());
        }

        for (GeneratedSourceFile file : state.getUiTestFiles()) {
            generatedFileWriter.write(file);
            state.addWrittenFile(file.relativePath());
        }

        state.addArtifact("generated.file.written", String.valueOf(state.getWrittenFiles().size()));
        state.addFinding("Generated file persisted: " + state.getWrittenFiles().size());
    }
}
