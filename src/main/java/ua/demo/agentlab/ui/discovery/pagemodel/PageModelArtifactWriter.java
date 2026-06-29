package ua.demo.agentlab.ui.discovery.pagemodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PageModelArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public PageModelArtifactWriter() {
        this(Path.of("target", "discovery", "page-model"));
    }

    public PageModelArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> write(PageModelBundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle cannot be null");
        }
        try {
            Files.createDirectories(outputDirectory);
            Path pagesDirectory = outputDirectory.resolve("pages");
            Files.createDirectories(pagesDirectory);
            deleteJsonFiles(pagesDirectory);
            List<String> writtenFiles = new ArrayList<>();
            writtenFiles.add(writeJson("page-models.json", bundle));
            for (PageModel page : bundle.pages()) {
                writtenFiles.add(writeJson("pages/" + safeFileName(page.pageId()) + ".json", page));
            }
            return writtenFiles;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write page model artifacts to: " + outputDirectory, exception);
        }
    }

    private String writeJson(String fileName, Object value) throws IOException {
        Path outputFile = outputDirectory.resolve(fileName);
        objectMapper.writeValue(outputFile.toFile(), value);
        return outputFile.toString();
    }

    private String safeFileName(String value) {
        String normalized = value == null ? "" : value.trim();
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("(^[.\\-]+|[.\\-]+$)", "");
        return normalized.isBlank() ? "page" : normalized;
    }

    private void deleteJsonFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }
}
