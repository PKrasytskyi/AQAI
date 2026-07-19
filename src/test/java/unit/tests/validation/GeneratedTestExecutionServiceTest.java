package unit.tests.validation;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.persistence.GeneratedSourceKind;
import ua.demo.agentlab.persistence.GeneratedSourceManifestEntry;
import ua.demo.agentlab.validation.execution.GeneratedTestExecutionService;

import java.nio.file.Path;
import java.util.List;

public class GeneratedTestExecutionServiceTest {

    @Test
    public void resolvesOnlyProjectSpecificManifestSourceRoot() {
        Path project = Path.of("").toAbsolutePath().normalize();
        String namespace = "src/test/java/ua/demo/agentlab/ui/generated/orangehrm_12345678";
        var entries = List.of(
                entry(GeneratedSourceKind.PAGE_OBJECT, namespace + "/pages/LoginPage.java", "LoginPage"),
                entry(GeneratedSourceKind.UI_TEST, namespace + "/tests/REQ001Test.java", "REQ001Test")
        );

        Path root = new GeneratedTestExecutionService(project).commonSourceRoot(entries);

        Assert.assertEquals(root, project.resolve(namespace.replace('/', java.io.File.separatorChar)));
    }

    @Test
    public void rejectsSharedTestRootThatCouldExecuteForeignProjectSources() {
        Path project = Path.of("").toAbsolutePath().normalize();
        var entries = List.of(
                entry(GeneratedSourceKind.PAGE_OBJECT, "src/test/java/LoginPage.java", "LoginPage"),
                entry(GeneratedSourceKind.UI_TEST, "src/test/java/REQ001Test.java", "REQ001Test")
        );

        Assert.expectThrows(IllegalArgumentException.class,
                () -> new GeneratedTestExecutionService(project).commonSourceRoot(entries));
    }

    private GeneratedSourceManifestEntry entry(GeneratedSourceKind kind, String path, String name) {
        String packageName = kind == GeneratedSourceKind.PAGE_OBJECT
                ? "ua.demo.agentlab.ui.generated.orangehrm_12345678.pages"
                : "ua.demo.agentlab.ui.generated.orangehrm_12345678.tests";
        return new GeneratedSourceManifestEntry(kind, packageName, name, path, "hash");
    }
}
