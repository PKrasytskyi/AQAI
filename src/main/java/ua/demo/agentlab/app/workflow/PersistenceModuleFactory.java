package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.persistence.LocalFilePersistenceAgent;
import ua.demo.agentlab.persistence.LocalGeneratedFileWriter;

public class PersistenceModuleFactory {

    public PersistenceModule create() {
        LocalGeneratedFileWriter generatedFileWriter = new LocalGeneratedFileWriter();
        return new PersistenceModule(
                generatedFileWriter,
                new LocalFilePersistenceAgent(generatedFileWriter)
        );
    }
}
