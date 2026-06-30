package ua.demo.agentlab.persistence;

import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public record GeneratedUiSources(
        List<GeneratedSourceFile> pageObjectFiles,
        List<GeneratedSourceFile> uiTestFiles
) {
    public GeneratedUiSources {
        pageObjectFiles = pageObjectFiles == null ? List.of() : List.copyOf(pageObjectFiles);
        uiTestFiles = uiTestFiles == null ? List.of() : List.copyOf(uiTestFiles);
    }

    public boolean isEmpty() {
        return pageObjectFiles.isEmpty() && uiTestFiles.isEmpty();
    }

    public List<GeneratedSourceFile> allFiles() {
        return java.util.stream.Stream.concat(pageObjectFiles.stream(), uiTestFiles.stream()).toList();
    }
}
