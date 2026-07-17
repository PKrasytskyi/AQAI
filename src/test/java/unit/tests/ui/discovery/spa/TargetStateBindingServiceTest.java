package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.spa.TargetStateBindingService;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.util.List;
import java.util.Map;

public class TargetStateBindingServiceTest {

    @Test
    public void createsFinalBindingAfterConfirmedLiveTransition() {
        CandidateLocatorEvidence locator = new CandidateLocatorEvidence("recruitment", "navigation", "recruitmentLink",
                "css", "a[href='/recruitment/viewCandidates']", 0.82d, true, 1, 1, true, true, false,
                LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of());
        CandidateActionEvidence action = new CandidateActionEvidence("openRecruitment", "navigation", "CLICK",
                "recruitmentLink", 0.85d, List.of("recruitment"), List.of(), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE);
        SemanticComponentInventory navigation = new SemanticComponentInventory("navigation", "Navigation",
                ComponentType.NAVIGATION, "css", "nav", "", List.of(), 0.90d, List.of(locator), List.of(action),
                List.of(), List.of());
        SpaInventoryBundle inventory = new SpaInventoryBundle(SpaInventoryBundle.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                List.of(new SpaPageInventory("dashboard", "DashboardPage", "/dashboard", "AUTHENTICATED_AREA", "fp",
                        null, List.of(navigation), List.of())), List.of());
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-1", "MODULE_NAVIGATION",
                List.of("Open the Recruitment module from navigation."),
                List.of(new StructuredAssertionRequirement("ROUTE_CHANGED", "recruitmentRoute",
                        "discovery-confirmed recruitment route", null)), Map.of(),
                "pageCapability: AUTHENTICATED_AREA; componentCapability: NAVIGATION; "
                        + "sourceRoute: project-profile.authenticatedRoute; targetPage: Recruitment page", true, List.of());
        SourceStateBinding source = new SourceStateBinding("REQ-1", "MODULE_NAVIGATION", "dashboard", "/dashboard",
                "Recruitment", List.of("navigation"), List.of("recruitment"), List.of("openRecruitment"), true, List.of());
        TargetedLocatorVerification liveLocator = new TargetedLocatorVerification("dashboard", "/dashboard", "fp", "navigation",
                "recruitment", "recruitmentLink", "css", locator.value(), 0.82d, true, "live", List.of("REQ-1"));
        TargetedActionVerification liveAction = new TargetedActionVerification("dashboard", "/dashboard", "fp", "navigation",
                "openRecruitment", "CLICK", "recruitmentLink", 0.85d, true, "live", List.of("REQ-1"));
        SpaLiveTargetedVerificationResult verification = new SpaLiveTargetedVerificationResult(
                SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null, true, true,
                List.of(liveLocator), List.of(liveAction), List.of());
        LiveTransitionDiscovery discovery = new LiveTransitionDiscovery(LiveTransitionDiscovery.SCHEMA_VERSION, null,
                verification, List.of(new RequirementStateTransition("REQ-1", "dashboard", "/dashboard",
                "openRecruitment", "recruitment", "state-target", "/recruitment/viewCandidates", true, "live")), List.of());

        var result = new TargetStateBindingService().bind(List.of(contract), inventory,
                new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION, null, List.of(source), List.of()), discovery);

        Assert.assertTrue(result.targets().get(0).transitionConfirmed());
        Assert.assertEquals(result.targets().get(0).targetRoute(), "/recruitment/viewCandidates");
        Assert.assertEquals(result.behaviorBindings().get(0).steps().get(0).actionId(), "openRecruitment");
    }
}
