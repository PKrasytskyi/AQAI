package ua.demo.agentlab.ai.rag.intelligence.store;

import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryKnowledgeSnapshot;

import java.nio.file.Path;
import java.util.Optional;

public interface KnowledgeStore {

    Path store(RepositoryKnowledgeSnapshot snapshot, Path outputDirectory);

    Optional<RepositoryKnowledgeSnapshot> load(Path outputDirectory);
}
