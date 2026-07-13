package unit.tests.persistence;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.persistence.GeneratedPageSourceReconciler;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GeneratedPageSourceReconcilerTest {

    @Test
    public void removesOnlyStaleSourcesFromGeneratedDirectory() throws Exception {
        Path root = Files.createTempDirectory("agentlab-persistence-");
        Path directory = root.resolve("generated/pages");
        Files.createDirectories(directory);
        Path expected = directory.resolve("LoginPage.java");
        Path stale = directory.resolve("Form1Component.java");
        Files.writeString(expected, "class LoginPage {}");
        Files.writeString(stale, "class Form1Component {}");

        List<String> removed = new GeneratedPageSourceReconciler().removeStalePageSources(List.of(
                new GeneratedSourceFile("example.generated.pages", "LoginPage", expected.toString(), "class LoginPage {}")
        ));

        Assert.assertTrue(Files.exists(expected));
        Assert.assertFalse(Files.exists(stale));
        Assert.assertEquals(removed, List.of(stale.toString()));
    }
}
