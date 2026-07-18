package unit.tests.architecture;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UiPackageDependencyInvariantTest {

    private static final Path MAIN = Path.of("src", "main", "java");

    @Test
    public void promptCodeDoesNotImportRawDiscoveryDtos() throws IOException {
        assertNoImport(MAIN.resolve(Path.of("ua", "demo", "agentlab", "ai", "ui", "prompt")),
                List.of("ui.discovery.selenium.model.Raw", "ui.discovery.model.UiDiscoverySnapshot"));
    }

    @Test
    public void discoveryDoesNotDependOnPomContracts() throws IOException {
        assertNoImport(MAIN.resolve(Path.of("ua", "demo", "agentlab", "ui", "discovery")),
                List.of("ua.demo.agentlab.ai.ui.contract.Pom"));
    }

    @Test
    public void funnelDoesNotMutateWorkflowState() throws IOException {
        assertNoImport(MAIN.resolve(Path.of("ua", "demo", "agentlab", "ui", "discovery", "evidence", "funnel")),
                List.of("WorkflowState", "PipelineArtifactStore", "StageOutputPublisher"));
    }

    private void assertNoImport(Path root, List<String> forbidden) throws IOException {
        if (!Files.exists(root)) return;
        List<String> violations;
        try (var paths = Files.walk(root)) {
            violations = paths.filter(path -> path.toString().endsWith(".java")).flatMap(path -> {
                try {
                    String source = Files.readString(path);
                    return forbidden.stream().filter(source::contains).map(value -> path + " -> " + value);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
        }
        Assert.assertTrue(violations.isEmpty(), "Forbidden package dependencies: " + violations);
    }
}
