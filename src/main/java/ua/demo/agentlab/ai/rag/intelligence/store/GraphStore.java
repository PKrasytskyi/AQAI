package ua.demo.agentlab.ai.rag.intelligence.store;

import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntity;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelation;

import java.nio.file.Path;
import java.util.List;

public interface GraphStore {

    Path store(List<RepositoryGraphEntity> entities, List<RepositoryGraphRelation> relations, Path outputDirectory);
}
