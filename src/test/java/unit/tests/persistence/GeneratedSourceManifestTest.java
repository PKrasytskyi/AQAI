package unit.tests.persistence;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.GenerationNamespace;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.persistence.GeneratedSourceManifest;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public class GeneratedSourceManifestTest {

    @Test
    public void generationNamespaceIsDeterministicAndProductSpecific() {
        GenerationNamespace orange = GenerationNamespace.resolve(
                "orangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
                "",
                ""
        );
        GenerationNamespace orangeRepeated = GenerationNamespace.resolve(
                "orangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
                null,
                null
        );
        GenerationNamespace internet = GenerationNamespace.resolve(
                "the-internet",
                "https://the-internet.herokuapp.com",
                "",
                ""
        );

        Assert.assertEquals(orange, orangeRepeated);
        Assert.assertNotEquals(orange.namespaceId(), internet.namespaceId());
        Assert.assertNotEquals(orange.pagesPackage(), internet.pagesPackage());
        Assert.assertNotEquals(orange.testsPackage(), internet.testsPackage());
        Assert.assertTrue(orange.pagesPackage().endsWith(".pages"));
        Assert.assertTrue(internet.testsPackage().endsWith(".tests"));
    }

    @Test
    public void identicalClassNamesResolveToDifferentManifestOwnedPaths() {
        ProjectProfile orange = profile(
                "orangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php"
        );
        ProjectProfile internet = profile("the-internet", "https://the-internet.herokuapp.com");

        GeneratedSourceManifest orangeManifest = manifest(orange, "LoginPage", "REQ001AuthenticationTest");
        GeneratedSourceManifest internetManifest = manifest(internet, "LoginPage", "REQ001AuthenticationTest");

        Assert.assertNotEquals(orangeManifest.namespaceId(), internetManifest.namespaceId());
        Assert.assertTrue(orangeManifest.persistedPaths().stream()
                .noneMatch(internetManifest.persistedPaths()::contains));
        Assert.assertEquals(orangeManifest.files().size(), 2);
        Assert.assertEquals(internetManifest.files().size(), 2);
    }

    @Test
    public void manifestRejectsForeignPackageAndModifiedContent() {
        ProjectProfile profile = profile("the-internet", "https://the-internet.herokuapp.com");
        GeneratedSourceManifest manifest = manifest(profile, "LoginPage", "REQ001AuthenticationTest");
        GeneratedSourceFile owned = source(
                profile.outputProfile().generatedPagesPackage(),
                "LoginPage",
                "public class LoginPage {}"
        );
        GeneratedSourceFile modified = new GeneratedSourceFile(
                owned.packageName(),
                owned.className(),
                owned.relativePath(),
                "public class LoginPage { private int changed; }"
        );

        Assert.assertTrue(manifest.owns(owned));
        Assert.assertFalse(manifest.owns(modified));

        GeneratedSourceFile foreign = source("ua.demo.agentlab.ui.generated.foreign.pages",
                "LoginPage", "public class LoginPage {}");
        Assert.expectThrows(IllegalStateException.class, () -> GeneratedSourceManifest.create(
                profile,
                WorkflowRunEnvelope.create("test", null),
                new GeneratedUiSources(List.of(foreign), List.of())
        ));
    }

    private static GeneratedSourceManifest manifest(
            ProjectProfile profile,
            String pageClass,
            String testClass
    ) {
        GeneratedSourceFile page = source(
                profile.outputProfile().generatedPagesPackage(),
                pageClass,
                "public class " + pageClass + " {}"
        );
        GeneratedSourceFile test = source(
                profile.outputProfile().generatedTestsPackage(),
                testClass,
                "public class " + testClass + " {}"
        );
        return GeneratedSourceManifest.create(
                profile,
                WorkflowRunEnvelope.create("test", null),
                new GeneratedUiSources(List.of(page), List.of(test))
        );
    }

    private static ProjectProfile profile(String profileId, String baseUrl) {
        GenerationNamespace namespace = GenerationNamespace.resolve(profileId, baseUrl, "", "");
        return new ProjectProfile(
                profileId,
                profileId,
                baseUrl,
                "/",
                "/login",
                "",
                "/secure",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile(namespace.pagesPackage(), namespace.testsPackage())
        );
    }

    private static GeneratedSourceFile source(String packageName, String className, String content) {
        String relativePath = "src/test/java/" + packageName.replace('.', '/') + "/" + className + ".java";
        return new GeneratedSourceFile(packageName, className, relativePath, content);
    }
}
