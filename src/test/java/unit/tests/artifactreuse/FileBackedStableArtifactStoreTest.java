package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.artifactreuse.store.FileBackedStableArtifactStore;
import ua.demo.agentlab.artifactreuse.store.StableArtifactLookup;
import ua.demo.agentlab.artifactreuse.store.StableArtifactWriteResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileBackedStableArtifactStoreTest {

    @Test
    public void writesAndReadsPomContractByFingerprint() throws Exception {
        Path root = Files.createTempDirectory("artifact-store-test");
        FileBackedStableArtifactStore store = new FileBackedStableArtifactStore(root);

        StableArtifactWriteResult write = store.writePomContract("LoginPage", "abc123", contract());
        StableArtifactLookup lookup = store.findPomContract("LoginPage", "abc123");

        Assert.assertTrue(write.success());
        Assert.assertTrue(lookup.hit());
        Assert.assertEquals(lookup.pomContract().page().name(), "LoginPage");
        Assert.assertTrue(write.path().toString().endsWith("LoginPage.abc123.pom-contract.json"));
    }

    @Test
    public void requiresLifecycleValidationMarkerForLocalReuse() throws Exception {
        Path root = Files.createTempDirectory("artifact-store-marker-test");
        FileBackedStableArtifactStore store = new FileBackedStableArtifactStore(root);
        StableArtifactWriteResult write = store.writePomContract("LoginPage", "abc123", contract());

        Assert.assertFalse(store.findValidatedPomContract("LoginPage", "abc123").hit());
        Assert.assertTrue(store.markPomContractValidated(write.path()).success());
        Assert.assertTrue(store.findValidatedPomContract("LoginPage", "abc123").hit());
    }

    private PomContractSpec contract() {
        return new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/auth/login", "AUTHENTICATION", "openLogin"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
