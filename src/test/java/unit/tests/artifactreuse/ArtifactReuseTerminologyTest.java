package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.artifactreuse.model.ArtifactReuseTerminology;

public class ArtifactReuseTerminologyTest {

    @Test
    public void separatesArtifactReuseFromKnowledgeGraphReuse() {
        Assert.assertTrue(ArtifactReuseTerminology.ARTIFACT.contains("POM contract"));
        Assert.assertTrue(ArtifactReuseTerminology.KNOWLEDGE.contains("page"));
        Assert.assertTrue(ArtifactReuseTerminology.ARTIFACT_REGISTRY.contains("fingerprints"));
        Assert.assertTrue(ArtifactReuseTerminology.QA_KNOWLEDGE_GRAPH.contains("flows"));
        Assert.assertTrue(ArtifactReuseTerminology.pomContractReuseBelongsToArtifactLayer());
        Assert.assertTrue(ArtifactReuseTerminology.flowPathReuseBelongsToKnowledgeGraph());
    }
}
