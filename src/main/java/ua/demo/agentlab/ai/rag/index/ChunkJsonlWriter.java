package ua.demo.agentlab.ai.rag.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.rag.model.CodeChunk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ChunkJsonlWriter {

    private final ObjectMapper objectMapper;

    public ChunkJsonlWriter() {
        this(new ObjectMapper());
    }

    public ChunkJsonlWriter(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper cannot be null");
        }
        this.objectMapper = objectMapper;
    }

    public Path write(Path outputPath, List<CodeChunk> chunks) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath cannot be null");
        }
        if (chunks == null) {
            throw new IllegalArgumentException("chunks cannot be null");
        }

        try {
            Path parent = outputPath.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            StringBuilder builder = new StringBuilder();
            for (CodeChunk chunk : chunks) {
                builder.append(objectMapper.writeValueAsString(chunk)).append(System.lineSeparator());
            }
            Files.writeString(outputPath, builder.toString(), StandardCharsets.UTF_8);
            return outputPath.toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write chunks JSONL file: " + outputPath, exception);
        }
    }
}
