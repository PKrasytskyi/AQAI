package unit.tests.ui.discovery.evidence.funnel;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelAcceptanceGate;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelMetrics;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelReport;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceRequirementResult;

import java.util.List;

public class UiEvidenceFunnelAcceptanceGateTest {

    private final UiEvidenceFunnelAcceptanceGate gate = new UiEvidenceFunnelAcceptanceGate();

    @Test
    public void acceptsConfirmedPathAndExplicitStop() {
        UiEvidenceRequirementResult confirmed = new UiEvidenceRequirementResult(
                "REQ-001", "AUTHENTICATION", "auth-page", "/auth", 3, 3, 0, 3,
                true, true, true, List.of("requirement:REQ-001", "pom-eligible"), "", "", ""
        );
        UiEvidenceRequirementResult stopped = new UiEvidenceRequirementResult(
                "REQ-002", "FILTER", "", "", 0, 0, 0, 0,
                false, false, false, List.of(), "DISCOVERY", "no matching component",
                "Run targeted discovery for FILTER."
        );
        UiEvidenceFunnelReport report = report(List.of(confirmed, stopped));

        Assert.assertTrue(gate.validate(report).isEmpty());
    }

    @Test
    public void blocksSilentEvidenceLoss() {
        UiEvidenceRequirementResult incomplete = new UiEvidenceRequirementResult(
                "REQ-003", "RECORD_LIST", "", "", 4, 0, 0, 0,
                false, false, false, List.of(), "", "", ""
        );

        List<String> issues = gate.validate(report(List.of(incomplete)));

        Assert.assertEquals(issues.size(), 1);
        Assert.assertTrue(issues.get(0).contains("REQ-003"));
    }

    private UiEvidenceFunnelReport report(List<UiEvidenceRequirementResult> requirements) {
        return new UiEvidenceFunnelReport(
                UiEvidenceFunnelReport.SCHEMA_VERSION,
                "run-1",
                true,
                requirements.stream().allMatch(UiEvidenceRequirementResult::confirmedEvidencePath),
                new UiEvidenceFunnelMetrics(4, 3, 0, 3, 3, 1, 1),
                requirements,
                List.of()
        );
    }
}
