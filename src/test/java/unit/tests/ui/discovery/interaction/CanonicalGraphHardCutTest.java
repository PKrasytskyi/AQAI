package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.DbStableLocatorEvidenceService;
import ua.demo.agentlab.ui.catalog.Neo4jStableCapabilityLookupService;
import ua.demo.agentlab.ui.discovery.interaction.persistence.CanonicalInteractionSmokeFeedbackWriter;
import ua.demo.agentlab.ui.discovery.interaction.persistence.CanonicalInteractionGraphProjectionWriter;

import java.lang.reflect.Method;

public class CanonicalGraphHardCutTest {

    @Test
    public void stableLocatorLookupUsesOnlyCanonicalGraphLabels() throws Exception {
        String query = privateString(new DbStableLocatorEvidenceService(null), "queryStatement");

        Assert.assertTrue(query.contains("UiLocatorEvidence"));
        Assert.assertTrue(query.contains("CONFIRMED_LOCATOR"));
        Assert.assertFalse(query.contains("SpaCandidateLocator"));
        Assert.assertFalse(query.contains("UiStableLocator"));
    }

    @Test
    public void stableCapabilityLookupUsesOnlyCanonicalGraphLabels() throws Exception {
        String query = privateString(new Neo4jStableCapabilityLookupService(), "query");

        Assert.assertTrue(query.contains("UiState"));
        Assert.assertTrue(query.contains("UiLocatorEvidence"));
        Assert.assertFalse(query.contains("UiInteractionPage"));
        Assert.assertFalse(query.contains("SpaCandidateLocator"));
    }

    @Test
    public void smokeFeedbackUpdatesCanonicalEvidence() throws Exception {
        String query = privateString(new CanonicalInteractionSmokeFeedbackWriter(null), "feedbackStatement");

        Assert.assertTrue(query.contains("UiLocatorEvidence"));
        Assert.assertTrue(query.contains("UiSemanticAction"));
        Assert.assertFalse(query.contains("SpaCandidateLocator"));
        Assert.assertFalse(query.contains("SpaCandidateAction"));
    }

    @Test
    public void canonicalProjectionOwnsActionStatusAndRetentionTimestamp() throws Exception {
        String query = privateString(new CanonicalInteractionGraphProjectionWriter(null), "projectionQuery");

        Assert.assertTrue(query.contains("a.status=e.status"));
        Assert.assertTrue(query.contains("a.lastSeen=e.updatedAt"));
        Assert.assertTrue(query.contains("UiLocatorEvidence"));
    }

    private String privateString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return String.valueOf(method.invoke(target));
    }
}
