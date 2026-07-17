package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;
import ua.demo.agentlab.ui.discovery.spa.model.*;
import java.util.*;

/** Converts inventory action evidence into small, typed, non-business-specific component flow candidates. */
public final class TypedComponentFlowBuilder {
    public TypedComponentFlowBundle build(SpaInventoryBundle inventory) {
        if (inventory == null || inventory.pages().isEmpty()) return new TypedComponentFlowBundle(null, List.of(), List.of("no SPA inventory"));
        List<TypedComponentFlow> flows = new ArrayList<>();
        for (SpaPageInventory page : inventory.pages()) {
            Map<ComponentType, List<SemanticComponentInventory>> byType = new EnumMap<>(ComponentType.class);
            page.components().forEach(component -> byType.computeIfAbsent(component.type(), ignored -> new ArrayList<>()).add(component));
            flows.addAll(navigation(page, byType.getOrDefault(ComponentType.NAVIGATION, List.of())));
            flows.addAll(filter(page, byType));
            flows.addAll(table(page, byType));
            flows.addAll(modal(page, byType.getOrDefault(ComponentType.MODAL, List.of())));
        }
        return new TypedComponentFlowBundle(inventory.pages().get(0).runMetadata(), List.copyOf(flows),
                List.of("typed component flows are candidates until live verification and smoke feedback"));
    }
    private List<TypedComponentFlow> navigation(SpaPageInventory page, List<SemanticComponentInventory> components) {
        return components.stream().flatMap(c -> c.actions().stream()).filter(a -> "CLICK".equals(a.intent()) || "OPEN_RECORD".equals(a.intent()))
                .map(a -> flow(page, ComponentFlowType.MODULE_NAVIGATION, List.of(a.componentId()), List.of(a), "")).toList();
    }
    private List<TypedComponentFlow> filter(SpaPageInventory page, Map<ComponentType,List<SemanticComponentInventory>> byType) {
        List<CandidateActionEvidence> actions = actions(byType.getOrDefault(ComponentType.FILTER_PANEL,List.of()), Set.of("FILTER","SELECT","CLICK"));
        return actions.isEmpty() || results(byType).isEmpty()?List.of():List.of(flow(page, ComponentFlowType.FILTER_RESULTS, ids(byType.get(ComponentType.FILTER_PANEL), results(byType)), actions, ""));
    }
    private List<TypedComponentFlow> table(SpaPageInventory page, Map<ComponentType,List<SemanticComponentInventory>> byType) {
        List<SemanticComponentInventory> tables = results(byType);
        List<TypedComponentFlow> out = new ArrayList<>();
        List<CandidateActionEvidence> sort=actions(tables, Set.of("SORT_COLLECTION","CLICK"));
        if(!sort.isEmpty()) out.add(flow(page, ComponentFlowType.TABLE_SORT, ids(tables), sort, ""));
        List<CandidateActionEvidence> paginate=actions(tables, Set.of("PAGINATE"));
        if(!paginate.isEmpty()) out.add(flow(page, ComponentFlowType.TABLE_PAGINATION, ids(tables), paginate, ""));
        return out;
    }
    private List<TypedComponentFlow> modal(SpaPageInventory page, List<SemanticComponentInventory> components) {
        List<TypedComponentFlow> out=new ArrayList<>();
        List<CandidateActionEvidence> confirm=actions(components, Set.of("CONFIRM_ACTION","CLICK"));
        if(!confirm.isEmpty()) out.add(flow(page, ComponentFlowType.MODAL_CONFIRMATION, ids(components), confirm, ""));
        return out;
    }
    private TypedComponentFlow flow(SpaPageInventory page, ComponentFlowType type,List<String> components,List<CandidateActionEvidence> actions,String targetRoute){
        List<String> ids=actions.stream().map(CandidateActionEvidence::actionId).distinct().toList();
        List<String> locators=actions.stream().flatMap(a->a.requiredLocatorIds().stream()).distinct().toList();
        double confidence=actions.stream().mapToDouble(CandidateActionEvidence::confidence).average().orElse(0d);
        return new TypedComponentFlow(page.pageId()+":flow:"+type.name().toLowerCase(Locale.ROOT)+":"+String.join("-",ids),page.pageId(),page.route(),type,components,ids,locators,targetRoute,postconditions(type, locators),confidence,SpaEvidenceStatus.CANDIDATE,List.of("spa-inventory:typed-component-flow"));
    }
    private List<FlowPostconditionContract> postconditions(ComponentFlowType type, List<String> locatorIds) {
        if (type == ComponentFlowType.MODULE_NAVIGATION) {
            return List.of(new FlowPostconditionContract(FlowPostconditionType.PAGE_CHANGED, List.of(), "route transition", true, "verified by live SPA route transition"));
        }
        if (locatorIds.isEmpty()) {
            return List.of(new FlowPostconditionContract(FlowPostconditionType.RESULTS_CHANGED, List.of(), "", false, "no confirmed result or modal locator is available"));
        }
        return switch (type) {
            case FILTER_RESULTS -> List.of(new FlowPostconditionContract(FlowPostconditionType.RESULTS_CHANGED, locatorIds, "", false, "requirement must define expected filtered data, count, or visible row"));
            case TABLE_SORT -> List.of(new FlowPostconditionContract(FlowPostconditionType.SORT_ORDER_CHANGED, locatorIds, "", false, "requirement must define sortable column and expected order"));
            case TABLE_PAGINATION -> List.of(new FlowPostconditionContract(FlowPostconditionType.PAGE_CHANGED, locatorIds, "", false, "requirement must define target page or changed rows"));
            case MODAL_CONFIRMATION, MODAL_CANCELLATION -> List.of(new FlowPostconditionContract(FlowPostconditionType.MODAL_CLOSED, locatorIds, "", false, "requirement must define expected state after the modal action"));
            default -> List.of();
        };
    }
    private List<SemanticComponentInventory> results(Map<ComponentType,List<SemanticComponentInventory>> map){ List<SemanticComponentInventory> r=new ArrayList<>();r.addAll(map.getOrDefault(ComponentType.RESULTS_COLLECTION,List.of()));r.addAll(map.getOrDefault(ComponentType.TABLE,List.of()));return r; }
    private List<CandidateActionEvidence> actions(List<SemanticComponentInventory> c, Set<String> intents){return c.stream().flatMap(x->x.actions().stream()).filter(a->intents.contains(a.intent())).toList();}
    private List<String> ids(List<SemanticComponentInventory>... groups){return Arrays.stream(groups).filter(Objects::nonNull).flatMap(Collection::stream).map(SemanticComponentInventory::componentId).distinct().toList();}
}
