package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;
import ua.demo.agentlab.ui.discovery.spa.TypedComponentFlowBuilder;
import ua.demo.agentlab.ui.discovery.spa.model.*;
import java.util.List;

class TypedComponentFlowBuilderTest {
    @Test void derivesNavigationAndTableFlowsFromTypedComponentEvidence() {
        CandidateActionEvidence click = new CandidateActionEvidence("nav-click","nav","CLICK","recruitment",.9,List.of("nav-link"),List.of(),List.of(),List.of(),SpaEvidenceStatus.CANDIDATE);
        CandidateActionEvidence sort = new CandidateActionEvidence("table-sort","table","SORT_COLLECTION","name",.9,List.of("sort"),List.of(),List.of(),List.of(),SpaEvidenceStatus.CANDIDATE);
        CandidateLocatorEvidence locator = new CandidateLocatorEvidence("nav-link","nav","recruitment","css","a[href='/recruitment']",.9,true,1,1,true,true,true,LocatorEvidenceType.CANDIDATE_LOCATOR,SpaEvidenceStatus.CANDIDATE,List.of());
        SemanticComponentInventory nav = new SemanticComponentInventory("nav","Sidebar",ComponentType.NAVIGATION,"css","nav","",List.of("recruitment"),.9,List.of(locator),List.of(click),List.of(),List.of());
        SemanticComponentInventory table = new SemanticComponentInventory("table","Results",ComponentType.TABLE,"css","table","",List.of("name"),.9,List.of(),List.of(sort),List.of(),List.of());
        UiInteractionPage page = new UiInteractionPage("dashboard","DashboardPage","/dashboard","AUTHENTICATED_AREA","fp",null,List.of(nav,table),List.of());
        var flows = new TypedComponentFlowBuilder().build(new UiInteractionInventory("v1", ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode.INVENTORY,List.of(page),List.of())).flows();
        Assert.assertTrue(flows.stream().anyMatch(flow -> flow.type()==ComponentFlowType.MODULE_NAVIGATION));
        Assert.assertTrue(flows.stream().anyMatch(flow -> flow.type()==ComponentFlowType.TABLE_SORT));
    }
}
