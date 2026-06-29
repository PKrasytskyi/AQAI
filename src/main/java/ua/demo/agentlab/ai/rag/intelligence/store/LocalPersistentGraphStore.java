package ua.demo.agentlab.ai.rag.intelligence.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntity;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LocalPersistentGraphStore implements GraphStore {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public Path store(List<RepositoryGraphEntity> entities, List<RepositoryGraphRelation> relations, Path outputDirectory) {
        try {
            Path graphDirectory = outputDirectory.toAbsolutePath().normalize().resolve("graph-store");
            Files.createDirectories(graphDirectory);
            objectMapper.writeValue(graphDirectory.resolve("entities.json").toFile(), entities);
            objectMapper.writeValue(graphDirectory.resolve("relations.json").toFile(), relations);
            objectMapper.writeValue(graphDirectory.resolve("graph-metadata.json").toFile(), Map.of(
                    "entityCount", entities == null ? 0 : entities.size(),
                    "relationCount", relations == null ? 0 : relations.size()
            ));
            return graphDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist graph store", exception);
        }
    }
}
