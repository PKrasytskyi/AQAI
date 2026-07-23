package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.catalog.*;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTraceAssembler;
import ua.demo.agentlab.ui.discovery.interaction.persistence.InteractionGraphProjectionResult;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceAssembler;

import java.util.List;

public class EvidenceProjectionTraceAssemblerTest {

    @Test
    public void reportsCatalogAndPersistenceAsProjectionsOfCanonicalEvidence() {
        var canonical = new CanonicalInteractionEvidenceAssembler().assemble(null, null, null, List.of());
        ConfirmedUiCatalog catalog = new ConfirmedUiCatalog(ConfirmedUiCatalog.SCHEMA_VERSION, "", false,
                List.of(), List.of());
        var trace = new EvidenceProjectionTraceAssembler().assemble(canonical, catalog,
                new InteractionGraphProjectionResult(false, 0, 0, 0, "disabled", List.of()));

        Assert.assertEquals(trace.rawCandidates(), 0);
        Assert.assertEquals(trace.persisted(), 0);
        Assert.assertTrue(trace.findings().contains("single-source=canonical-interaction-evidence"));
    }
}
