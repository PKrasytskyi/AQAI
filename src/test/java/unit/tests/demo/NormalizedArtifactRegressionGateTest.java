package unit.tests.demo;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.demo.snapshot.NormalizedArtifactRegressionGate;

import java.nio.file.Files;
import java.nio.file.Path;

public class NormalizedArtifactRegressionGateTest {

    @Test
    public void ignoresRunMetadataAndCollectionOrderButDetectsBehaviorChanges() throws Exception {
        Path directory = Files.createTempDirectory("normalized-artifact-gate");
        Path expected = directory.resolve("expected.json");
        Path equivalent = directory.resolve("equivalent.json");
        Path changed = directory.resolve("changed.json");
        Files.writeString(expected, "{\"runId\":\"one\",\"actions\":[\"CLICK\",\"TYPE\"],\"route\":\"/login\"}");
        Files.writeString(equivalent, "{\"route\":\"/login\",\"actions\":[\"CLICK\",\"TYPE\"],\"runId\":\"two\"}");
        Files.writeString(changed, "{\"route\":\"/dashboard\",\"actions\":[\"TYPE\",\"CLICK\"]}");

        NormalizedArtifactRegressionGate gate = new NormalizedArtifactRegressionGate();
        Assert.assertTrue(gate.compare(expected, equivalent).passed());
        Assert.assertFalse(gate.compare(expected, changed).passed());
    }
}
