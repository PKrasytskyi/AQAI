package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

public interface PageKnowledgeWriter {

    PageKnowledgeWriteResult write(MappedUiKnowledge knowledge);
}
