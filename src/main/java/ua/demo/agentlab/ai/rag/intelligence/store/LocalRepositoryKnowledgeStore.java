package ua.demo.agentlab.ai.rag.intelligence.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryKnowledgeSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LocalRepositoryKnowledgeStore implements KnowledgeStore {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public Path store(RepositoryKnowledgeSnapshot snapshot, Path outputDirectory) {
        try {
            Path normalizedOutput = outputDirectory.toAbsolutePath().normalize();
            Files.createDirectories(normalizedOutput);
            write(normalizedOutput.resolve("knowledge-snapshot.json"), snapshot);
            write(normalizedOutput.resolve("framework-detection.json"), snapshot.frameworkDetection());
            write(normalizedOutput.resolve("parsing-diagnostics.json"), snapshot.parsingDiagnostics());
            write(normalizedOutput.resolve("source-inventory.json"), snapshot.sourceInventory());
            write(normalizedOutput.resolve("document-fingerprints.json"), snapshot.documentFingerprints());
            write(normalizedOutput.resolve("java-ast-results.json"), snapshot.javaAstResults());
            write(normalizedOutput.resolve("controller-routes.json"), snapshot.controllerRoutes());
            write(normalizedOutput.resolve("dto-models.json"), snapshot.dtoModels());
            write(normalizedOutput.resolve("layer-components.json"), snapshot.layerComponents());
            write(normalizedOutput.resolve("existing-tests.json"), snapshot.existingTests());
            write(normalizedOutput.resolve("openapi-specifications.json"), snapshot.openApiSpecifications());
            write(normalizedOutput.resolve("endpoint-matches.json"), snapshot.endpointMatches());
            write(normalizedOutput.resolve("graph-entities.json"), snapshot.graphEntities());
            write(normalizedOutput.resolve("graph-relations.json"), snapshot.graphRelations());
            write(normalizedOutput.resolve("knowledge-enrichments.json"), snapshot.knowledgeEnrichments());
            write(normalizedOutput.resolve("vector-summaries.json"), snapshot.vectorSummaries());
            write(normalizedOutput.resolve("initial-test-ideas.json"), snapshot.initialTestIdeas());
            writeSummary(normalizedOutput.resolve("knowledge-summary.md"), snapshot);
            return normalizedOutput;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store repository knowledge snapshot", exception);
        }
    }

    @Override
    public Optional<RepositoryKnowledgeSnapshot> load(Path outputDirectory) {
        try {
            Path snapshotPath = outputDirectory.toAbsolutePath().normalize().resolve("knowledge-snapshot.json");
            if (!Files.exists(snapshotPath)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(snapshotPath.toFile(), RepositoryKnowledgeSnapshot.class));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load repository knowledge snapshot", exception);
        }
    }

    private void write(Path path, Object value) throws IOException {
        objectMapper.writeValue(path.toFile(), value);
    }

    private void writeSummary(Path path, RepositoryKnowledgeSnapshot snapshot) throws IOException {
        String markdown = """
                # Repository Knowledge Summary

                Frameworks: %s
                Scanned files: %d
                Warnings: %d
                Changed documents: %d
                Reused documents: %d
                Controller routes: %d
                DTO/models: %d
                Layer components: %d
                Existing tests: %d
                OpenAPI specifications: %d
                Endpoint matches: %d
                Graph entities: %d
                Graph relations: %d
                Knowledge enrichments: %d
                Vector summaries: %d
                Initial test ideas: %d
                """.formatted(
                snapshot.frameworkDetection().frameworks(),
                snapshot.parsingDiagnostics().scannedFiles(),
                snapshot.parsingDiagnostics().warnings().size(),
                snapshot.parsingDiagnostics().incrementalIndexStats().changedDocuments(),
                snapshot.parsingDiagnostics().incrementalIndexStats().unchangedDocuments(),
                snapshot.controllerRoutes().size(),
                snapshot.dtoModels().size(),
                snapshot.layerComponents().size(),
                snapshot.existingTests().size(),
                snapshot.openApiSpecifications().size(),
                snapshot.endpointMatches().stream().filter(ua.demo.agentlab.ai.rag.intelligence.model.EndpointMatch::matched).count(),
                snapshot.graphEntities().size(),
                snapshot.graphRelations().size(),
                snapshot.knowledgeEnrichments().size(),
                snapshot.vectorSummaries().size(),
                snapshot.initialTestIdeas().size()
        );
        Files.writeString(path, markdown);
    }
}
