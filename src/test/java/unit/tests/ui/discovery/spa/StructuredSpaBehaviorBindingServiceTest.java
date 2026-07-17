package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.StructuredSpaBehaviorBindingService;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedActionVerification;

import java.util.List;
import java.util.Map;

public class StructuredSpaBehaviorBindingServiceTest {

    @Test
    public void rejectsFilterBindingWhenSearchEvidenceIsNotConfirmed() {
        CandidateLocatorEvidence search = locator("search", "filters", "searchButton");
        SpaPageInventory page = page("vacancies", "RECORD_LIST", ComponentType.FILTER_PANEL, List.of(search), List.of());
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-1", "filter", List.of(),
                List.of(new StructuredAssertionRequirement("RESULTS_CHANGED", "results", "", null)), Map.of(), "", true, List.of());

        var result = new StructuredSpaBehaviorBindingService().bind(List.of(contract),
                new SpaInventoryBundle("v", SpaDiscoveryMode.TARGETED, List.of(page), List.of()),
                SpaTargetedVerificationResult.empty(null, "x")).get(0);

        Assert.assertFalse(result.executable());
        Assert.assertFalse(result.reviewReasons().isEmpty());
    }

    @Test
    public void bindsModuleNavigationOnlyToConfirmedLocatorAndAction() {
        CandidateLocatorEvidence recruitment = locator("recruitment", "navigation", "recruitmentLink");
        CandidateActionEvidence openRecruitment = new CandidateActionEvidence("openRecruitment", "navigation", "CLICK",
                "recruitmentLink", 0.95d, List.of("recruitment"), List.of(), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE);
        SpaPageInventory page = page("dashboard", "AUTHENTICATED_AREA", ComponentType.NAVIGATION, List.of(recruitment), List.of(openRecruitment));
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-1", "MODULE_NAVIGATION",
                List.of("Open the Recruitment module from the authenticated application navigation."),
                List.of(new StructuredAssertionRequirement("ROUTE_CHANGED", "recruitmentModuleRoute", "discovery-confirmed recruitment route", null)),
                Map.of(), "* `pageCapability: AUTHENTICATED_AREA`\n* `componentCapability: NAVIGATION`\n* `sourceRoute: /dashboard`", true, List.of());
        TargetedLocatorVerification verified = new TargetedLocatorVerification("dashboard", "/dashboard", "fp", "navigation",
                "recruitment", "recruitmentLink", "css", "a[href*='recruitment']", 0.95d, true, "live confirmed", List.of("REQ-1"));
        TargetedActionVerification verifiedAction = new TargetedActionVerification("dashboard", "/dashboard", "fp", "navigation",
                "openRecruitment", "CLICK", "recruitmentLink", 0.95d, true, "live confirmed", List.of("REQ-1"));

        var result = new StructuredSpaBehaviorBindingService().bind(List.of(contract),
                new SpaInventoryBundle("v", SpaDiscoveryMode.TARGETED, List.of(page), List.of()),
                new SpaTargetedVerificationResult("v", null, List.of(verified), List.of(verifiedAction), List.of(), List.of())).get(0);

        Assert.assertTrue(result.executable(), result.reviewReasons().toString());
        Assert.assertEquals(result.steps().size(), 1);
        Assert.assertEquals(result.steps().get(0).locatorId(), "recruitment");
        Assert.assertEquals(result.steps().get(0).actionId(), "openRecruitment");
    }

    @Test
    public void matchesNormalizedCapabilityTokensWithoutSubstringFallback() {
        CandidateLocatorEvidence recruitment = locator("recruitment", "navigation", "recruitmentLink");
        CandidateActionEvidence openRecruitment = new CandidateActionEvidence("openRecruitment", "navigation", "CLICK",
                "recruitmentLink", 0.95d, List.of("recruitment"), List.of(), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE);
        SpaPageInventory page = page("dashboard", "overview|AUTHENTICATED_AREA|MODULE_NAVIGATION", ComponentType.NAVIGATION,
                List.of(recruitment), List.of(openRecruitment));
        StructuredBehaviorContract contract = new StructuredBehaviorContract("REQ-1", "MODULE_NAVIGATION",
                List.of("Open the Recruitment module."),
                List.of(new StructuredAssertionRequirement("ROUTE_CHANGED", "recruitment route", "", null)), Map.of(),
                "`pageCapability: authenticated-area` `componentCapability: NAVIGATION` `sourceRoute: /dashboard`", true, List.of());
        TargetedLocatorVerification verified = new TargetedLocatorVerification("dashboard", "/dashboard", "fp", "navigation",
                "recruitment", "recruitmentLink", "css", "a[href*='recruitment']", 0.95d, true, "live confirmed", List.of("REQ-1"));
        TargetedActionVerification verifiedAction = new TargetedActionVerification("dashboard", "/dashboard", "fp", "navigation",
                "openRecruitment", "CLICK", "recruitmentLink", 0.95d, true, "live confirmed", List.of("REQ-1"));

        var result = new StructuredSpaBehaviorBindingService().bind(List.of(contract),
                new SpaInventoryBundle("v", SpaDiscoveryMode.TARGETED, List.of(page), List.of()),
                new SpaTargetedVerificationResult("v", null, List.of(verified), List.of(verifiedAction), List.of(), List.of())).get(0);

        Assert.assertTrue(result.executable(), result.reviewReasons().toString());
    }

    @Test
    public void doesNotBindAnUnconfirmedNamedTargetToAnotherPageWithTheSameCapability() {
        CandidateLocatorEvidence candidatesTable = locator("candidateRows", "results", "candidateRows");
        SpaPageInventory candidates = new SpaPageInventory(
                "candidates", "CandidatesPage", "/recruitment/candidates", "RECORD_LIST", "fp", null,
                List.of(new SemanticComponentInventory("results", "Results", ComponentType.RESULTS_COLLECTION,
                        "", "", "", List.of(), 0.95d, List.of(candidatesTable), List.of(), List.of(), List.of())),
                List.of()
        );
        StructuredBehaviorContract contract = new StructuredBehaviorContract(
                "REQ-VACANCIES", "RECORD_LIST", List.of("Inspect vacancies"),
                List.of(new StructuredAssertionRequirement("RESULTS_CHANGED", "vacancy rows", "", null)), Map.of(),
                "`pageCapability: RECORD_LIST` `targetPage: discovery-confirmed Vacancies page`", true, List.of()
        );

        var result = new StructuredSpaBehaviorBindingService().bind(List.of(contract),
                new SpaInventoryBundle("v", SpaDiscoveryMode.TARGETED, List.of(candidates), List.of()),
                SpaTargetedVerificationResult.empty(null, "no-live-evidence")).get(0);

        Assert.assertFalse(result.executable());
        Assert.assertTrue(result.pageId().isBlank());
        Assert.assertTrue(result.reviewReasons().stream().anyMatch(reason -> reason.contains("No current-run page")));
    }

    @Test
    public void finalizesSelectionFromProvisionalSourceAndLiveEvidenceWithoutNavigationFlow() {
        CandidateLocatorEvidence checkbox = new CandidateLocatorEvidence(
                "checkbox", "form", "checkboxInput", "css", "input[type='checkbox']", 0.91d,
                true, 1, 1, true, true, true, LocatorEvidenceType.CANDIDATE_LOCATOR,
                SpaEvidenceStatus.CANDIDATE, List.of()
        );
        CandidateActionEvidence check = new CandidateActionEvidence(
                "checkCheckbox", "form", "CHECK", "checkboxInput", 0.92d, List.of("checkbox"),
                List.of(), List.of(), List.of(), SpaEvidenceStatus.CANDIDATE
        );
        SemanticComponentInventory form = new SemanticComponentInventory(
                "form", "Checkboxes", ComponentType.FORM, "css", "#checkboxes", "", List.of("checkboxInput"),
                0.9d, List.of(checkbox), List.of(check), List.of(), List.of()
        );
        SpaPageInventory page = new SpaPageInventory(
                "checkboxes", "CheckboxesPage", "/checkboxes", "form", "fp", null, List.of(form), List.of()
        );
        StructuredBehaviorContract contract = new StructuredBehaviorContract(
                "REQ-6", "selection", List.of("Select an unchecked checkbox."),
                List.of(new StructuredAssertionRequirement("ELEMENT_SELECTED", "checkboxInput", "checked", null)),
                Map.of(), "targetRoute: /checkboxes\npageCapability: FORM_CONTROLS\ncomponentCapability: CHECKBOX",
                true, List.of()
        );
        SourceStateBinding source = new SourceStateBinding(
                "REQ-6", "selection", "checkboxes", "/checkboxes", "checkbox", List.of("form"),
                List.of("checkbox"), List.of("checkCheckbox"), true, List.of()
        );
        SpaTargetedVerificationResult live = new SpaTargetedVerificationResult(
                SpaTargetedVerificationResult.SCHEMA_VERSION, null,
                List.of(new TargetedLocatorVerification("checkboxes", "/checkboxes", "fp", "form", "checkbox",
                        "checkboxInput", "css", "input[type='checkbox']", 0.91d, true, "live", List.of("REQ-6"))),
                List.of(new TargetedActionVerification("checkboxes", "/checkboxes", "fp", "form", "checkCheckbox",
                        "CHECK", "checkboxInput", 0.92d, true, "live", List.of("REQ-6"))), List.of(), List.of()
        );

        var result = new StructuredSpaBehaviorBindingService().bind(
                List.of(contract), new SpaInventoryBundle("v", SpaDiscoveryMode.TARGETED, List.of(page), List.of()), live,
                new SourceStateBindingBundle(SourceStateBindingBundle.SCHEMA_VERSION, null, List.of(source), List.of())
        ).get(0);

        Assert.assertTrue(result.executable(), result.reviewReasons().toString());
        Assert.assertEquals(result.pageId(), "checkboxes");
        Assert.assertEquals(result.steps().get(0).kind(), "CHECK");
        Assert.assertEquals(result.steps().get(0).locatorId(), "checkbox");
        Assert.assertTrue(result.flowId().isBlank(), "Selection does not require a navigation flow");
    }

    private CandidateLocatorEvidence locator(String locatorId, String componentId, String elementId) {
        return new CandidateLocatorEvidence(locatorId, componentId, elementId, "css", "a[href*='recruitment']", 0.95d,
                true, 1, 1, true, true, true, LocatorEvidenceType.CANDIDATE_LOCATOR, SpaEvidenceStatus.CANDIDATE, List.of());
    }

    private SpaPageInventory page(String id, String capability, ComponentType type, List<CandidateLocatorEvidence> locators,
                                  List<CandidateActionEvidence> actions) {
        SemanticComponentInventory component = new SemanticComponentInventory("navigation", "Navigation", type, "", "", "",
                List.of(), 0.95d, locators, actions, List.of(), List.of());
        return new SpaPageInventory(id, "Dashboard", "/dashboard", capability, "fp", null, List.of(component), List.of());
    }
}
