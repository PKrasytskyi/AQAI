package unit.tests.ui.discovery.evidence.funnel;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelAssembler;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelInput;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelReport;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBinding;

import java.util.List;
import java.util.Map;

public class UiEvidenceFunnelAssemblerTest {

    @Test
    public void acceptsMultiPageAndBrowserCapabilitiesOutsideSpaFixture() {
        for (String capability : List.of(
                "SELECTION", "DYNAMIC_CONTROL", "DYNAMIC_LOADING", "PASSWORD_RECOVERY",
                "HOVER", "DATA_ENTRY", "SLIDER_INTERACTION", "JAVASCRIPT_DIALOG",
                "FILE_UPLOAD", "WINDOW_MANAGEMENT")) {
            StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                    "REQ-" + capability, capability, List.of("exercise capability"), List.of(),
                    Map.of(), "targetRoute: /fixture", true, List.of()
            );
            UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                    "run-universal", List.of(requirement),
                    SpaInventoryBundle.empty(SpaDiscoveryMode.TARGETED, "test:no-pages"),
                    List.of(), SpaLiveTargetedVerificationResult.skipped(null, "not-run"),
                    emptyTargets(), emptyContext()
            ));

            Assert.assertNotEquals(report.requirements().get(0).stoppedAt(), "CAPABILITY_CLASSIFICATION",
                    capability + " must be recognized by the universal registry");
        }
    }

    @Test
    public void noDbRunProducesExplicitStopInsteadOfThrowing() {
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-NO-DB", "MODULE_NAVIGATION", List.of("open module"), List.of(),
                Map.of(), "pageCapability: AUTHENTICATED_AREA", true, List.of()
        );
        AiContextPackage context = new AiContextPackage(
                "", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                PromptUiEvidence.empty("test:no-db-prompt-evidence")
        );
        UiEvidenceFunnelInput input = new UiEvidenceFunnelInput(
                "run-without-db",
                List.of(requirement),
                SpaInventoryBundle.empty(SpaDiscoveryMode.TARGETED, "test:no-pages"),
                List.of(),
                SpaLiveTargetedVerificationResult.skipped(null, "test:no-live-evidence"),
                emptyTargets(),
                context
        );

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(input);

        Assert.assertTrue(report.completenessPassed());
        Assert.assertFalse(report.pomReadinessPassed());
        Assert.assertEquals(report.metrics().dbStableLocators(), 0);
        Assert.assertFalse(report.requirements().get(0).confirmedEvidencePath());
        Assert.assertEquals(report.requirements().get(0).stoppedAt(), "DISCOVERY");
        Assert.assertTrue(report.requirements().get(0).hasExplicitStop());
    }

    @Test
    public void preservesExactBindingReasonWhenDiscoveryCannotResolvePage() {
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-MISSING", "RECORD_LIST", List.of("open vacancies"), List.of(),
                Map.of(), "pageCapability: RECORD_LIST", true, List.of()
        );
        BoundSpaBehaviorContract binding = new BoundSpaBehaviorContract(
                "REQ-MISSING", "RECORD_LIST", "", "", "", List.of(), List.of(), List.of(),
                Map.of(), false, List.of("No current-run page matches targetContext route /vacancies")
        );
        AiContextPackage context = new AiContextPackage(
                "", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                PromptUiEvidence.empty("test:missing-page")
        );

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                "run-missing", List.of(requirement),
                SpaInventoryBundle.empty(SpaDiscoveryMode.TARGETED, "test:no-pages"),
                List.of(binding), SpaLiveTargetedVerificationResult.skipped(null, "not-run"), emptyTargets(), context
        ));

        Assert.assertEquals(report.requirements().get(0).stoppedAt(), "DISCOVERY");
        Assert.assertEquals(report.requirements().get(0).reason(),
                "No current-run page matches targetContext route /vacancies");
    }

    @Test
    public void keepsRawLiveDbAndPromptCountsAtSeparateBoundaries() {
        CandidateLocatorEvidence locator = new CandidateLocatorEvidence(
                "search-input", "filter-panel", "search", "css", "[data-test='search']", 0.94,
                true, 1, 1, true, true, true, LocatorEvidenceType.CONFIRMED_LOCATOR,
                SpaEvidenceStatus.CONFIRMED, List.of()
        );
        SemanticComponentInventory component = new SemanticComponentInventory(
                "filter-panel", "FilterPanel", ComponentType.FILTER_PANEL, "css", "[data-test='filters']",
                "", List.of("search"), 0.95, List.of(locator), List.of(), List.of(), List.of()
        );
        SpaPageInventory page = new SpaPageInventory(
                "record-list", "RecordListPage", "/records", "RECORD_LIST|FILTER", "fingerprint", null,
                List.of(component), List.of()
        );
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-001", "FILTER", List.of("filter records"), List.of(), Map.of(), "record list", true, List.of()
        );
        BoundSpaBehaviorContract binding = new BoundSpaBehaviorContract(
                "REQ-001", "FILTER", "record-list", "/records", "flow-1", List.of("filter-panel"),
                List.of(new BoundSpaBehaviorStep("TYPE", "filter-records", "search-input", "query", "")),
                List.of(), Map.of(), true, List.of()
        );
        TargetedLocatorVerification verification = new TargetedLocatorVerification(
                "record-list", "/records", "fingerprint", "filter-panel", "search-input", "search",
                "css", "[data-test='search']", 0.94, true, "unique in component", List.of("REQ-001")
        );
        PromptLocatorEvidence promptLocator = new PromptLocatorEvidence(
                "searchInput", "search", "css", "[data-test='search']", "input", "", "", true,
                0.94, "FilterPanel", "FILTER_PANEL", 1, 1, true,
                LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of("spa-locator-id:search-input", "db-stable-locator:search-input")
        );
        AiContextPackage context = new AiContextPackage(
                "", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(promptLocator), List.of(promptLocator),
                new PromptUiEvidence("RecordListPage", "/records", List.of("REQ-001"),
                        List.of(), List.of(), List.of(), List.of(), List.of(),
                        List.of("test:exact-owned-prompt-scope"), 0.94d)
        );
        UiEvidenceFunnelInput input = new UiEvidenceFunnelInput(
                "run-1", List.of(requirement),
                new SpaInventoryBundle(SpaInventoryBundle.SCHEMA_VERSION, SpaDiscoveryMode.TARGETED,
                        List.of(page), List.of()),
                List.of(binding),
                new SpaLiveTargetedVerificationResult(
                        SpaLiveTargetedVerificationResult.SCHEMA_VERSION, null, true, true,
                        List.of(verification), List.of(), List.of()),
                emptyTargets(),
                context
        );

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(input);

        Assert.assertTrue(report.completenessPassed());
        Assert.assertTrue(report.pomReadinessPassed());
        Assert.assertEquals(report.metrics().rawLocatorCandidates(), 1);
        Assert.assertEquals(report.metrics().liveVerifiedLocators(), 1);
        Assert.assertEquals(report.metrics().dbStableLocators(), 1);
        Assert.assertEquals(report.metrics().contextPromptAllowedLocators(), 0);
        Assert.assertEquals(report.metrics().requirementScopedPromptAllowedLocators(), 1);
        Assert.assertEquals(report.metrics().requirementBoundPages(), 1);
        Assert.assertEquals(report.metrics().promptEligiblePages(), 1);
        Assert.assertTrue(report.requirements().get(0).confirmedEvidencePath());
    }

    @Test
    public void locatorIdAloneCannotClaimPromptOwnership() {
        CandidateLocatorEvidence locator = new CandidateLocatorEvidence(
                "search-input", "filter-panel", "search", "css", "#search", 0.94,
                true, 1, 1, true, true, true, LocatorEvidenceType.CONFIRMED_LOCATOR,
                SpaEvidenceStatus.CONFIRMED, List.of()
        );
        SemanticComponentInventory component = new SemanticComponentInventory(
                "filter-panel", "FilterPanel", ComponentType.FILTER_PANEL, "css", "#filters", "",
                List.of("search"), 0.95, List.of(locator), List.of(), List.of(), List.of()
        );
        SpaPageInventory page = new SpaPageInventory(
                "records", "RecordsPage", "/records", "FILTER", "fp", null, List.of(component), List.of()
        );
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-1", "FILTER", List.of("filter records"), List.of(), Map.of(),
                "targetRoute: /records", true, List.of()
        );
        BoundSpaBehaviorContract binding = new BoundSpaBehaviorContract(
                "REQ-1", "FILTER", "records", "/records", "flow", List.of("filter-panel"),
                List.of(new BoundSpaBehaviorStep("TYPE", "filter", "search-input", "query", "")),
                List.of(), Map.of(), true, List.of()
        );
        PromptLocatorEvidence promptLocator = new PromptLocatorEvidence(
                "search", "search", "css", "#search", "input", "", "", true, 0.94,
                "FilterPanel", "FILTER_PANEL", 1, 1, true, LocatorEvidenceType.CONFIRMED_LOCATOR,
                List.of("spa-locator-id:search-input")
        );
        AiContextPackage context = new AiContextPackage(
                "", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(promptLocator),
                PromptUiEvidence.empty("test:no-owned-prompt-scope")
        );

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                "run", List.of(requirement), new SpaInventoryBundle(SpaInventoryBundle.SCHEMA_VERSION,
                SpaDiscoveryMode.TARGETED, List.of(page), List.of()), List.of(binding),
                new SpaLiveTargetedVerificationResult(SpaLiveTargetedVerificationResult.SCHEMA_VERSION,
                        null, true, true, List.of(new TargetedLocatorVerification(
                        "records", "/records", "fp", "filter-panel", "search-input", "search",
                        "css", "#search", 0.94, true, "verified", List.of("REQ-1"))), List.of(), List.of()),
                emptyTargets(), context
        ));

        Assert.assertFalse(report.requirements().get(0).promptEligible());
        Assert.assertEquals(report.requirements().get(0).stoppedAt(), "PROMPT_OWNERSHIP");
    }

    @Test
    public void reportsDataReadinessBeforeMissingTargetPage() {
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-DATA", "FILTER", List.of("filter vacancies"), List.of(), Map.of("dataset", "vacancy-filter"),
                "targetPage: Vacancies page", false, List.of());
        BoundSpaBehaviorContract binding = new BoundSpaBehaviorContract(
                "REQ-DATA", "FILTER", "", "", "", List.of(), List.of(), List.of(), Map.of(), false,
                List.of("Missing data value for 'JOB_TITLE'.", "Scenario dataset 'vacancy-filter' is unavailable"));

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                "run-data", List.of(requirement), SpaInventoryBundle.empty(SpaDiscoveryMode.TARGETED, "none"),
                List.of(binding), SpaLiveTargetedVerificationResult.skipped(null, "not-run"), emptyTargets(), emptyContext()));

        Assert.assertEquals(report.requirements().get(0).stoppedAt(), "DATA_READINESS");
        Assert.assertTrue(report.requirements().get(0).reason().contains("JOB_TITLE"));
    }

    @Test
    public void doesNotBorrowConfirmedTargetFromAnotherRequirement() {
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-FILTER", "FILTER", List.of("Inspect vacancy filters"), List.of(), Map.of(),
                "targetPage: discovery-confirmed Vacancies page", true, List.of());
        TargetStateBinding target = new TargetStateBinding("REQ-NAV", "MODULE_NAVIGATION", "recruitment",
                "/recruitment/viewCandidates", "state-vacancies", "/recruitment/viewJobVacancy",
                "open-vacancies", "vacancies-link", true, List.of());
        TargetStateBindingBundle targets = new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION,
                null, List.of(target), List.of(), List.of());

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                "run-target", List.of(requirement), SpaInventoryBundle.empty(SpaDiscoveryMode.TARGETED, "none"),
                List.of(), SpaLiveTargetedVerificationResult.skipped(null, "not-run"), targets, emptyContext()));

        Assert.assertEquals(report.requirements().get(0).stoppedAt(), "DISCOVERY");
        Assert.assertFalse(report.requirements().get(0).reason().contains("/recruitment/viewJobVacancy"));
    }

    @Test
    public void classifiesMissingTargetAssertionsAfterConfirmedNavigationAsTargetMapping() {
        SpaPageInventory sourcePage = new SpaPageInventory("recruitment", "RecruitmentPage",
                "/recruitment/viewCandidates", "MODULE_NAVIGATION", "fp", null, List.of(), List.of());
        StructuredBehaviorContract requirement = new StructuredBehaviorContract(
                "REQ-NAV", "MODULE_NAVIGATION", List.of("Open Vacancies"), List.of(), Map.of(),
                "targetPage: Vacancies page", true, List.of());
        BoundSpaBehaviorContract binding = new BoundSpaBehaviorContract(
                "REQ-NAV", "MODULE_NAVIGATION", "recruitment", "/recruitment/viewCandidates", "flow",
                List.of(), List.of(), List.of(), Map.of(), false,
                List.of("No confirmed locator binding for assertion target 'vacancyFilterPanel'."));
        TargetStateBinding target = new TargetStateBinding("REQ-NAV", "MODULE_NAVIGATION", "recruitment",
                "/recruitment/viewCandidates", "state-vacancies", "/recruitment/viewJobVacancy",
                "open-vacancies", "vacancies-link", true, List.of());

        UiEvidenceFunnelReport report = new UiEvidenceFunnelAssembler().assemble(new UiEvidenceFunnelInput(
                "run-nav", List.of(requirement), new SpaInventoryBundle(SpaInventoryBundle.SCHEMA_VERSION,
                SpaDiscoveryMode.TARGETED, List.of(sourcePage), List.of()), List.of(binding),
                SpaLiveTargetedVerificationResult.skipped(null, "not-run"),
                new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, null, List.of(target), List.of(), List.of()),
                emptyContext()));

        Assert.assertEquals(report.requirements().get(0).stoppedAt(), "TARGET_STATE_MAPPING");
        Assert.assertTrue(report.requirements().get(0).reason().contains("vacancyFilterPanel"));
    }

    private AiContextPackage emptyContext() {
        return new AiContextPackage("", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), PromptUiEvidence.empty("test:empty"));
    }

    private TargetStateBindingBundle emptyTargets() {
        return new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, null, List.of(), List.of(), List.of());
    }
}
