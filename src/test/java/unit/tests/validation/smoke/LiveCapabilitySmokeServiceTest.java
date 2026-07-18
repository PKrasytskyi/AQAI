package unit.tests.validation.smoke;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;
import ua.demo.agentlab.validation.smoke.LiveCapabilitySmokeService;
import ua.demo.agentlab.validation.smoke.LiveUiSmokeResult;

import java.util.List;

public class LiveCapabilitySmokeServiceTest {

    @Test
    public void disabledLiveSmokeReturnsSkippedWithoutStartingBrowser() {
        String previous = System.getProperty("ui.live-smoke.enabled");
        System.setProperty("ui.live-smoke.enabled", "false");
        try {
            LiveUiSmokeResult result = new LiveCapabilitySmokeService()
                    .smoke(new GeneratedUiSources(List.of(), List.of()));

            Assert.assertEquals(result.status(), GeneratedUiSmokeStatus.SKIPPED);
            Assert.assertEquals(result.executionMode(), "NOT_EXECUTED");
            Assert.assertTrue(result.summary().contains("disabled"));
            Assert.assertTrue(result.issues().stream()
                    .anyMatch(issue -> "LIVE_SMOKE_DISABLED".equals(issue.ruleId())));
        } finally {
            if (previous == null) {
                System.clearProperty("ui.live-smoke.enabled");
            } else {
                System.setProperty("ui.live-smoke.enabled", previous);
            }
        }
    }
}
