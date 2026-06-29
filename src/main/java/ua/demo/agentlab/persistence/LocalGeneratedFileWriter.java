package ua.demo.agentlab.persistence;

import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalGeneratedFileWriter implements GeneratedFileWriter {

    @Override
    public void write(GeneratedSourceFile file) {
        try {
            Path path = Path.of(file.relativePath());
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.content());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write generated file: " + file.relativePath(), e);
        }
    }
}
