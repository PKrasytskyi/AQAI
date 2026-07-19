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

    @Test
    public void reconcilesPageAndTestDirectoriesForCurrentNamespace() throws Exception {
        Path root = Files.createTempDirectory("agentlab-namespace-");
        Path pages = root.resolve("generated/demo/pages");
        Path tests = root.resolve("generated/demo/tests");
        Files.createDirectories(pages);
        Files.createDirectories(tests);
        Path page = pages.resolve("LoginPage.java");
        Path stalePage = pages.resolve("OldPage.java");
        Path test = tests.resolve("REQ001Test.java");
        Path staleTest = tests.resolve("OldTest.java");
        Files.writeString(page, "class LoginPage {}");
        Files.writeString(stalePage, "class OldPage {}");
        Files.writeString(test, "class REQ001Test {}");
        Files.writeString(staleTest, "class OldTest {}");

        List<String> removed = new GeneratedPageSourceReconciler().removeStaleGeneratedSources(List.of(
                new GeneratedSourceFile("generated.demo.pages", "LoginPage", page.toString(), "class LoginPage {}"),
                new GeneratedSourceFile("generated.demo.tests", "REQ001Test", test.toString(), "class REQ001Test {}")
        ));

        Assert.assertTrue(Files.exists(page));
        Assert.assertTrue(Files.exists(test));
        Assert.assertFalse(Files.exists(stalePage));
        Assert.assertFalse(Files.exists(staleTest));
        Assert.assertEquals(removed.size(), 2);
    }
}
