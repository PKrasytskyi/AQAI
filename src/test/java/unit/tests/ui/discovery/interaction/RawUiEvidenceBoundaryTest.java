package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.raw.RawLocatorObservation;
import ua.demo.agentlab.ui.discovery.interaction.raw.RawUiElementEvidence;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RawUiEvidenceBoundaryTest {

    @Test
    public void rawEvidenceDoesNotContainSemanticOrPromotionDecisions() {
        List<String> fields = Arrays.stream(RawUiElementEvidence.class.getRecordComponents())
                .map(RecordComponent::getName).toList();
        Assert.assertFalse(fields.contains("capability"));
        Assert.assertFalse(fields.contains("semanticAction"));
        Assert.assertFalse(fields.contains("evidenceStatus"));
        Assert.assertFalse(fields.contains("promptAllowed"));

        RawUiElementEvidence raw = new RawUiElementEvidence(
                "page", "/route", "state", "container", "raw-1", "button", "submit", "Search",
                "", "", "", "", "button", "", "", true, true, Map.of(),
                List.of(new RawLocatorObservation("obs-1", "css", "button[type='submit']", true, 1, 1, List.of())),
                List.of("selenium"));
        Assert.assertEquals(raw.text(), "Search");
    }
}
