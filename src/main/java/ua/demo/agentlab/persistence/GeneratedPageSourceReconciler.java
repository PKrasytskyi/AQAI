package ua.demo.agentlab.persistence;

import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Removes obsolete files only from the framework-owned ignored generated source directories. */
public class GeneratedPageSourceReconciler {

    public List<String> removeStalePageSources(List<GeneratedSourceFile> pageFiles) {
        Map<Path, Set<Path>> expectedByDirectory = expectedByDirectory(pageFiles);
        List<String> removed = new java.util.ArrayList<>();
        for (Map.Entry<Path, Set<Path>> entry : expectedByDirectory.entrySet()) {
            Path directory = entry.getKey();
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(this::isJavaSource).toList()) {
                    if (entry.getValue().contains(file.toAbsolutePath().normalize())) {
                        continue;
                    }
                    Files.deleteIfExists(file);
                    removed.add(file.toString());
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot reconcile generated page sources in " + directory, exception);
            }
        }
        return List.copyOf(removed);
    }

    private Map<Path, Set<Path>> expectedByDirectory(List<GeneratedSourceFile> pageFiles) {
        Map<Path, Set<Path>> expected = new LinkedHashMap<>();
        for (GeneratedSourceFile pageFile : pageFiles == null ? List.<GeneratedSourceFile>of() : pageFiles) {
            Path file = Path.of(pageFile.relativePath()).toAbsolutePath().normalize();
            Path directory = file.getParent();
            if (directory == null || !isFrameworkGeneratedDirectory(directory)) {
                continue;
            }
            expected.computeIfAbsent(directory, ignored -> new LinkedHashSet<>()).add(file);
        }
        return expected;
    }

    private boolean isJavaSource(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java");
    }

    private boolean isFrameworkGeneratedDirectory(Path directory) {
        return directory.toString().replace('\\', '/').contains("/generated/");
    }
}
