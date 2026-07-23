package ua.demo.agentlab.ai.ui.contract;


import ua.demo.agentlab.ui.generation.provenance.PomSourceMap;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds deterministic member-to-contract provenance without parsing generated Java text. */
public final class PomSourceMapBuilder {
    private final PomJavaFieldNameResolver fieldNameResolver = new PomJavaFieldNameResolver();

    public PomSourceMap build(List<PomContractSpec> contracts, List<GeneratedSourceFile> sources) {
        Map<String, GeneratedSourceFile> byClass = new LinkedHashMap<>();
        if (sources != null) sources.forEach(source -> byClass.putIfAbsent(source.className(), source));
        List<PomSourceMap.PageEntry> pages = new ArrayList<>();
        for (PomContractSpec contract : contracts == null ? List.<PomContractSpec>of() : contracts) {
            Map<String, PomLocatorSpec> locators = locators(contract);
            List<PomSourceMap.FieldEntry> fields = locators.values().stream()
                    .map(locator -> new PomSourceMap.FieldEntry(fieldNameResolver.resolve(locator), locator.id(),
                            locator.strategy(), locator.value(), ""))
                    .toList();
            List<PomSourceMap.MethodEntry> methods = new ArrayList<>();
            appendMethods(methods, "", contract.actions());
            for (PomComponentSpec component : contract.components()) appendMethods(methods, component.name(), component.actions());
            GeneratedSourceFile source = byClass.get(contract.page().name());
            pages.add(new PomSourceMap.PageEntry(contract.page().name(), contract.page().route(),
                    source == null ? "" : source.relativePath(), fields, methods));
        }
        return new PomSourceMap(pages.size(), pages);
    }

    private void appendMethods(List<PomSourceMap.MethodEntry> target, String componentId, List<PomActionSpec> actions) {
        for (PomActionSpec action : actions == null ? List.<PomActionSpec>of() : actions) {
            Set<String> locatorIds = new LinkedHashSet<>();
            action.steps().forEach(step -> { if (!step.locator().isBlank()) locatorIds.add(step.locator()); });
            target.add(new PomSourceMap.MethodEntry(action.methodName(), componentId, List.of(action.methodName()), List.copyOf(locatorIds)));
        }
    }

    private Map<String, PomLocatorSpec> locators(PomContractSpec contract) {
        Map<String, PomLocatorSpec> result = new LinkedHashMap<>();
        contract.locators().forEach(locator -> result.putIfAbsent(locator.id(), locator));
        contract.components().forEach(component -> component.locators().forEach(locator -> result.putIfAbsent(locator.id(), locator)));
        return result;
    }
}
