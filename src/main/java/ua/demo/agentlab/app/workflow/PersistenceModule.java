package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.persistence.GeneratedFileWriter;

public record PersistenceModule(
        GeneratedFileWriter generatedFileWriter,
        WorkflowAgent filePersistenceAgent
) {
}
