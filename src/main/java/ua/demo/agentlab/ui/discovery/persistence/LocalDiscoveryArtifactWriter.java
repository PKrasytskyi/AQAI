package ua.demo.agentlab.ui.discovery.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LocalDiscoveryArtifactWriter implements DiscoveryArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public LocalDiscoveryArtifactWriter() {
        this(Path.of("target", "discovery"));
    }

    public LocalDiscoveryArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public List<String> write(
            UiDiscoverySnapshot snapshot,
            SeleniumDiscoveryResult seleniumDiscoveryResult,
            MappedUiKnowledge mappedUiKnowledge
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }

        try {
            Files.createDirectories(outputDirectory);
            List<String> writtenFiles = new ArrayList<>();

            writtenFiles.add(writeJson("snapshot.json", snapshot));
            writtenFiles.add(writeJson("flows.json", snapshot.flows()));

            if (seleniumDiscoveryResult != null) {
                writtenFiles.add(writeJson("pages.json", seleniumDiscoveryResult.pages()));
                writtenFiles.add(writeJson("transitions.json", seleniumDiscoveryResult.transitions()));
                writtenFiles.add(writeJson("selenium-discovery.json", seleniumDiscoveryResult));
                writtenFiles.addAll(writeAuthenticationReport(seleniumDiscoveryResult));
                writtenFiles.addAll(writePageMetadataFiles(seleniumDiscoveryResult));
            } else {
                writtenFiles.add(writeJson("pages.json", snapshot.pages()));
                writtenFiles.add(writeJson("transitions.json", List.of()));
            }

            if (mappedUiKnowledge != null) {
                writtenFiles.add(writeJson("mapped-ui-knowledge.json", mappedUiKnowledge));
                writtenFiles.add(writeJson("mapped-pages.json", mappedUiKnowledge.pages()));
                writtenFiles.add(writeJson("mapped-transitions.json", mappedUiKnowledge.transitions()));
                writtenFiles.add(writeJson("graph-nodes.json", mappedUiKnowledge.graphNodes()));
                writtenFiles.add(writeJson("graph-edges.json", mappedUiKnowledge.graphEdges()));
                writtenFiles.add(writeJson("vector-documents.json", mappedUiKnowledge.vectorDocuments()));
                writtenFiles.addAll(writeMappedPageFiles(mappedUiKnowledge.pages()));
            }

            return writtenFiles;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write discovery artifacts to: " + outputDirectory, exception);
        }
    }

    private String writeJson(String fileName, Object value) throws IOException {
        Path outputFile = outputDirectory.resolve(fileName);
        objectMapper.writeValue(outputFile.toFile(), value);
        return outputFile.toString();
    }

    private List<String> writePageMetadataFiles(SeleniumDiscoveryResult seleniumDiscoveryResult) throws IOException {
        List<String> writtenFiles = new ArrayList<>();
        Path pageDirectory = outputDirectory.resolve("pages");
        Files.createDirectories(pageDirectory);
        deleteJsonFiles(pageDirectory);

        for (var page : seleniumDiscoveryResult.pages()) {
            Path outputFile = pageDirectory.resolve(page.pageId() + ".json");
            objectMapper.writeValue(outputFile.toFile(), page);
            writtenFiles.add(outputFile.toString());
        }

        return writtenFiles;
    }

    private List<String> writeAuthenticationReport(SeleniumDiscoveryResult seleniumDiscoveryResult) throws IOException {
        if (seleniumDiscoveryResult.authenticationResults().isEmpty()) {
            return List.of();
        }
        Path authDirectory = outputDirectory.resolve("auth");
        Files.createDirectories(authDirectory);
        Path outputFile = authDirectory.resolve("authentication-report.json");
        objectMapper.writeValue(outputFile.toFile(), seleniumDiscoveryResult.authenticationResults());
        return List.of(outputFile.toString());
    }

    private List<String> writeMappedPageFiles(List<MappedPage> mappedPages) throws IOException {
        List<String> writtenFiles = new ArrayList<>();
        Path pageDirectory = outputDirectory.resolve("mapped-pages");
        Files.createDirectories(pageDirectory);
        deleteJsonFiles(pageDirectory);

        for (MappedPage page : mappedPages) {
            Path outputFile = pageDirectory.resolve(page.pageId() + ".json");
            objectMapper.writeValue(outputFile.toFile(), page);
            writtenFiles.add(outputFile.toString());
        }

        return writtenFiles;
    }

    private void deleteJsonFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }
}
