package unit.tests.ui.discovery.evidence.funnel;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.evidence.funnel.SpaAcceptanceCapability;
import ua.demo.agentlab.ui.discovery.evidence.funnel.SpaAcceptanceFixture;
import ua.demo.agentlab.ui.discovery.evidence.funnel.SpaAcceptanceFixtureLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SpaAcceptanceFixtureTest {

    @Test
    public void loadsCapabilityOnlyHappyPathFixture() {
        SpaAcceptanceFixture fixture = new SpaAcceptanceFixtureLoader()
                .loadResource("acceptance/orangehrm-spa-acceptance.yaml");

        Assert.assertEquals(fixture.flowCapabilities(), List.of(
                SpaAcceptanceCapability.AUTHENTICATION,
                SpaAcceptanceCapability.MODULE_NAVIGATION,
                SpaAcceptanceCapability.RECORD_LIST,
                SpaAcceptanceCapability.FILTER
        ));
        Assert.assertEquals(fixture.supportedCapabilityContracts(), List.of(
                SpaAcceptanceCapability.AUTHENTICATION,
                SpaAcceptanceCapability.AUTHENTICATED_AREA,
                SpaAcceptanceCapability.MODULE_NAVIGATION,
                SpaAcceptanceCapability.USER_MENU,
                SpaAcceptanceCapability.RECORD_LIST,
                SpaAcceptanceCapability.FILTER,
                SpaAcceptanceCapability.MODAL
        ));
        Assert.assertNotNull(Thread.currentThread().getContextClassLoader()
                .getResource(fixture.projectProfile()));
        Assert.assertTrue(Files.isRegularFile(Path.of(fixture.requirementsFile())));
    }

    @Test
    public void genericCapabilityVocabularyContainsNoProductRoutesOrSelectors() {
        for (SpaAcceptanceCapability capability : SpaAcceptanceCapability.values()) {
            String value = capability.name().toLowerCase();
            Assert.assertFalse(value.contains("orange"));
            Assert.assertFalse(value.contains("dashboard"));
            Assert.assertFalse(value.contains("recruitment"));
            Assert.assertFalse(value.contains("/") || value.contains("#") || value.contains("."));
        }
    }
}
