package ua.demo.agentlab.ui.testcontract.writer;

import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public record DeterministicTestNgGenerationResult(
        List<GeneratedSourceFile> files,
        UiTestSourceMap sourceMap
) {
    public DeterministicTestNgGenerationResult {
        files = files == null ? List.of() : List.copyOf(files);
        sourceMap = sourceMap == null ? new UiTestSourceMap(null, List.of()) : sourceMap;
    }
}
