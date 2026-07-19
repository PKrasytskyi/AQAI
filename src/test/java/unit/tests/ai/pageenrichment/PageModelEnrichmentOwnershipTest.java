package unit.tests.ai.pageenrichment;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentAgent;
import ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentInputBundle;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichmentClient;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;

import java.util.List;

public class PageModelEnrichmentOwnershipTest {

    @Test
    public void doesNotCallLlmForPageWithoutPageOwnedRequirementEvidence() {
        CountingClient client = new CountingClient();
        PageModelEnrichmentAgent agent = new PageModelEnrichmentAgent(client);
        MappedPage dashboard = new MappedPage(
                "dashboard", "DashboardPage", "authenticated-area", "https://example.test/dashboard",
                "/dashboard", "Dashboard", List.of(), List.of(), List.of(), List.of(), List.of(), null, "", ""
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(dashboard), List.of(), List.of(), List.of(), List.of()
        );
        PageModelEnrichmentInputBundle input = new PageModelEnrichmentInputBundle(
                null,
                new UiTestPlan("test", "DashboardPage", List.of("DashboardPage"), List.of()),
                new CanonicalTestCaseBundle("test", "NoPages", List.of(), List.of()),
                new PageModelBundle(List.of()),
                knowledge,
                null,
                null
        );

        var output = agent.execute(input, null);

        Assert.assertEquals(client.calls, 0);
        Assert.assertTrue(output.generatedRecords().isEmpty());
        Assert.assertTrue(output.records().isEmpty());
    }

    @Test
    public void sendsOnlyRequirementSpecificModuleLocatorToEnrichment() {
        CountingClient client = new CountingClient();
        PageModelEnrichmentAgent agent = new PageModelEnrichmentAgent(client);
        MappedPage dashboard = new MappedPage(
                "dashboard", "DashboardPage", "authenticated-area", "https://example.test/dashboard",
                "/dashboard", "Dashboard", List.of(), List.of(
                navigationLink("admin", "Admin", "/admin/viewAdminModule"),
                navigationLink("recruitment", "Recruitment", "/recruitment/viewRecruitmentModule"),
                navigationLink("pim", "PIM", "/pim/viewPimModule")
        ), List.of(), List.of(), List.of(), null, "", ""
        );
        CanonicalTestCase testCase = new CanonicalTestCase(
                "REQ-001", "Open Recruitment module", List.of("REQ-001"), List.of(), List.of(), List.of(),
                List.of("DashboardPage"), new UiScenarioPrerequisite("DashboardPage", "/dashboard", true, List.of()),
                "flow-1", "MODULE_NAVIGATION", "DashboardPage", "DashboardPage", "/dashboard", "/dashboard",
                "User is authenticated", UiAssertionProfile.BASIC,
                List.of("Open the Recruitment module from authenticated navigation"),
                List.of("Recruitment module content is visible"), List.of(), "requirements/test.md [L1]"
        );
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(dashboard), List.of(), List.of(), List.of(), List.of()
        );
        PageModelEnrichmentInputBundle input = new PageModelEnrichmentInputBundle(
                null,
                new UiTestPlan("test", "DashboardPage", List.of("DashboardPage"), List.of()),
                new CanonicalTestCaseBundle("test", "DashboardPage", List.of("DashboardPage"), List.of(testCase)),
                new PageModelBundle(List.of()), knowledge, null, null,
                List.of(new BoundSpaBehaviorContract(
                        "REQ-001", "MODULE_NAVIGATION", "dashboard", "/dashboard", "", List.of(),
                        List.of(), List.of(), java.util.Map.of(), true, List.of()
                ))
        );

        agent.execute(input, null);

        Assert.assertEquals(client.calls, 1);
        Assert.assertEquals(client.lastInputs.size(), 1);
        List<String> locators = client.lastInputs.get(0).stableLocators();
        Assert.assertEquals(locators.size(), 1);
        Assert.assertTrue(locators.get(0).contains("recruitment/viewRecruitmentModule"));
        Assert.assertFalse(locators.stream().anyMatch(locator -> locator.contains("Admin") || locator.contains("PIM")));
    }

    private MappedElement navigationLink(String id, String text, String href) {
        LocatorCandidate locator = new LocatorCandidate(
                LocatorStrategy.CSS, "a[href='" + href + "']", 0.90d, "test", "link", text, text,
                href, "example.test", true, true, true, List.of(), LocatorEvidenceType.CONFIRMED_LOCATOR
        );
        return new MappedElement(id, text, "LINK", "link", text, true, true,
                List.of(locator), List.of("click"), 0.90d);
    }

    private static final class CountingClient implements PageModelEnrichmentClient {
        private int calls;
        private List<PageModelEnrichmentInput> lastInputs = List.of();

        @Override
        public List<PageModelEnrichmentRecord> enrich(List<PageModelEnrichmentInput> inputs) {
            calls++;
            lastInputs = List.copyOf(inputs);
            return List.of();
        }
    }
}
