package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Produces final behavior bindings only after the live transition stage has completed. */
public final class TargetStateBindingService {
    private final StructuredSpaBehaviorBindingService behaviorBindingService;

    public TargetStateBindingService() {
        this(new StructuredSpaBehaviorBindingService());
    }

    TargetStateBindingService(StructuredSpaBehaviorBindingService behaviorBindingService) {
        this.behaviorBindingService = behaviorBindingService;
    }

    public TargetStateBindingBundle bind(List<StructuredBehaviorContract> contracts, UiInteractionInventory inventory,
                                         SourceStateBindingBundle sources, LiveTransitionDiscovery discovery) {
        if (inventory == null || sources == null || discovery == null || discovery.verification() == null) {
            return new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, null, List.of(), List.of(),
                    List.of("target-state-binding:missing-input"));
        }
        List<TargetedLocatorVerification> liveLocators = new ArrayList<>(
                discovery.verification().locatorVerifications());
        liveLocators.addAll(runtimeVerifiedTargetLocators(inventory, discovery));
        SpaTargetedVerificationResult liveEvidence = new SpaTargetedVerificationResult(
                SpaTargetedVerificationResult.SCHEMA_VERSION, discovery.runMetadata(),
                distinctLocators(liveLocators), discovery.verification().actionVerifications(),
                List.of(), List.of("target-state-binding:live-evidence"));
        List<StructuredBehaviorContract> requestedContracts = contracts == null
                ? List.of() : List.copyOf(contracts);
        List<StructuredBehaviorContract> scopedContracts = requestedContracts.stream()
                .map(contract -> sourceContract(withConfirmedSource(contract, sources)))
                .toList();
        List<BoundSpaBehaviorContract> baseBindings = behaviorBindingService.bind(
                scopedContracts, inventory, liveEvidence, sources);
        Map<String, BoundSpaBehaviorContract> baseByRequirement = new LinkedHashMap<>();
        baseBindings.forEach(binding -> baseByRequirement.put(binding.requirementId().toLowerCase(), binding));
        List<TargetStateBinding> targets = new ArrayList<>();
        List<BoundSpaBehaviorContract> finalBindings = new ArrayList<>();
        for (StructuredBehaviorContract contract : requestedContracts) {
            BoundSpaBehaviorContract binding = baseByRequirement.get(contract.requirementId().toLowerCase());
            if (binding == null) {
                continue;
            }
            SourceStateBinding source = sources.bindings().stream()
                    .filter(candidate -> candidate.requirementId().equalsIgnoreCase(binding.requirementId()))
                    .findFirst().orElse(null);
            RequirementStateTransition transition = discovery.transitions().stream()
                    .filter(candidate -> candidate.requirementId().equalsIgnoreCase(binding.requirementId()))
                    .findFirst().orElse(null);
            List<String> review = new ArrayList<>(binding.reviewReasons());
            boolean transitionRequired = requiresTargetTransition(contract);
            if (transitionRequired && (transition == null || !transition.confirmed())) {
                review.add(transition == null ? "No live target-state transition was discovered."
                        : transition.reason());
            }
            BoundSpaBehaviorContract targetBinding = transitionRequired && transition != null && transition.confirmed()
                    ? targetBinding(contract, transition, inventory, liveEvidence)
                    : null;
            if (targetBinding != null) {
                review.addAll(targetBinding.reviewReasons());
            }
            boolean executable = binding.executable()
                    && (!transitionRequired || transition.confirmed())
                    && (targetBinding == null || targetBinding.executable());
            finalBindings.add(combine(binding, targetBinding, executable, review));
            targets.add(new TargetStateBinding(binding.requirementId(), binding.capability(),
                    source == null ? "" : source.sourcePageId(), source == null ? "" : source.sourceRoute(),
                    transition == null ? "" : transition.targetStateId(), transition == null ? "" : transition.targetRoute(),
                    transition == null ? "" : transition.actionId(), transition == null ? "" : transition.locatorId(),
                    transition != null && transition.confirmed(), transition == null || transition.confirmed()
                    ? List.of() : List.of(transition.reason())));
        }
        return new TargetStateBindingBundle(TargetStateBindingBundle.SCHEMA_VERSION, discovery.runMetadata(), targets,
                finalBindings, List.of("target-state-binding:after-live-transition-discovery"));
    }

    private StructuredBehaviorContract sourceContract(StructuredBehaviorContract contract) {
        if (!requiresTargetTransition(contract)) {
            return contract;
        }
        if (!normalize(contract.capability()).equals("module_navigation")) {
            return new StructuredBehaviorContract(contract.requirementId(), contract.capability(), contract.actions(),
                    List.of(), contract.dataRequirements(), contract.targetContext(), contract.executable(),
                    contract.reviewReasons());
        }
        String sourceRoute = contextValue(contract.targetContext(), "sourceRoute");
        String context = "sourceRoute: " + sourceRoute + "\ncomponentCapability: NAVIGATION";
        return new StructuredBehaviorContract(contract.requirementId(), contract.capability(), contract.actions(),
                List.of(), contract.dataRequirements(), context, contract.executable(), contract.reviewReasons());
    }

    private BoundSpaBehaviorContract targetBinding(
            StructuredBehaviorContract contract,
            RequirementStateTransition transition,
            UiInteractionInventory inventory,
            SpaTargetedVerificationResult evidence
    ) {
        String componentCapabilities = targetComponentCapabilities(contract);
        StringBuilder context = new StringBuilder("targetRoute: ").append(transition.targetRoute());
        appendContext(context, "pageCapability", contextValue(contract.targetContext(), "pageCapability"));
        appendContext(context, "targetPage", contextValue(contract.targetContext(), "targetPage"));
        appendContext(context, "componentCapability", componentCapabilities);
        StructuredBehaviorContract target = new StructuredBehaviorContract(
                contract.requirementId(), "TARGET_STATE", List.of("Inspect target state"), contract.assertions(),
                contract.dataRequirements(), context.toString(), contract.executable(), contract.reviewReasons());
        return behaviorBindingService.bind(List.of(target), inventory, evidence).stream().findFirst().orElse(null);
    }

    private void appendContext(StringBuilder target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.append('\n').append(key).append(": ").append(value.trim());
        }
    }

    private boolean requiresTargetTransition(StructuredBehaviorContract contract) {
        if (contract == null) return false;
        String capability = normalize(contract.capability());
        if (capability.equals("module_navigation")) return true;
        String actions = normalize(String.join(" ", contract.actions()));
        if (actions.contains("open") && actions.contains("menu")) return true;
        if (actions.contains("submit") || actions.contains("logout") || actions.contains("sign out")) return true;
        String source = contextValue(contract.targetContext(), "sourceRoute");
        String target = contextValue(contract.targetContext(), "targetRoute");
        return !source.isBlank() && !target.isBlank() && !normalize(source).equals(normalize(target));
    }

    private String targetComponentCapabilities(StructuredBehaviorContract contract) {
        String capability = normalize(contract.capability());
        String actions = normalize(String.join(" ", contract.actions()));
        String logoutAccessMode = normalize(contextValue(contract.targetContext(), "logoutAccessMode"));
        List<String> sourceOwned = capability.equals("module_navigation") ? List.of("NAVIGATION")
                : capability.equals("authentication") ? List.of("FORM")
                : capability.equals("logout") && (actions.contains("logout") || actions.contains("sign out"))
                ? logoutAccessMode.equals("direct_control")
                    ? List.of("NAVIGATION")
                    : List.of("USER_MENU", "HEADER")
                : capability.equals("logout") ? List.of("HEADER")
                : List.of();
        return java.util.Arrays.stream(contextValue(contract.targetContext(), "componentCapability").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> sourceOwned.stream().noneMatch(owner -> owner.equalsIgnoreCase(value)))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private BoundSpaBehaviorContract combine(
            BoundSpaBehaviorContract source,
            BoundSpaBehaviorContract target,
            boolean executable,
            List<String> review
    ) {
        if (target == null) {
            return new BoundSpaBehaviorContract(source.requirementId(), source.capability(), source.pageId(),
                    source.route(), source.flowId(), source.componentIds(), source.steps(), source.assertions(),
                    source.resolvedData(), executable, List.copyOf(new LinkedHashSet<>(review)));
        }
        List<String> components = new ArrayList<>(source.componentIds());
        components.addAll(target.componentIds());
        Map<String, String> data = new LinkedHashMap<>(source.resolvedData());
        data.putAll(target.resolvedData());
        return new BoundSpaBehaviorContract(source.requirementId(), source.capability(), target.pageId(),
                target.route(), source.flowId(), List.copyOf(new LinkedHashSet<>(components)), source.steps(),
                target.assertions(), Map.copyOf(data), executable, List.copyOf(new LinkedHashSet<>(review)));
    }

    private List<TargetedLocatorVerification> runtimeVerifiedTargetLocators(
            UiInteractionInventory inventory,
            LiveTransitionDiscovery discovery
    ) {
        List<TargetedLocatorVerification> result = new ArrayList<>();
        for (RequirementStateTransition transition : discovery.transitions()) {
            if (!transition.confirmed() || transition.targetRoute().isBlank()) continue;
            inventory.pages().stream()
                    .filter(page -> routeMatches(page.route(), transition.targetRoute()))
                    .findFirst()
                    .ifPresent(page -> page.components().forEach(component -> component.locators().stream()
                            .filter(this::runtimeVerified)
                            .forEach(locator -> result.add(new TargetedLocatorVerification(
                                    page.pageId(), page.route(), page.pageFingerprintHash(), component.componentId(),
                                    locator.locatorId(), locator.elementId(), locator.strategy(), locator.value(),
                                    locator.qualityScore(), true, "live target snapshot runtime count",
                                    List.of(transition.requirementId()))))));
        }
        return result;
    }

    private boolean runtimeVerified(ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence locator) {
        boolean unique = locator.globalMatchCount() == 1 || locator.componentMatchCount() == 1;
        boolean safe = locator.sameOrigin() && locator.risks().stream()
                .map(this::normalize)
                .noneMatch(risk -> risk.contains("external") || risk.contains("absolute")
                        || risk.contains("hidden") || risk.contains("missing") || risk.contains("nth_child"));
        return unique && safe && locator.qualityScore() >= 0.65d;
    }

    private List<TargetedLocatorVerification> distinctLocators(List<TargetedLocatorVerification> locators) {
        Map<String, TargetedLocatorVerification> distinct = new LinkedHashMap<>();
        locators.forEach(locator -> distinct.putIfAbsent(locator.locatorId(), locator));
        return List.copyOf(distinct.values());
    }

    private StructuredBehaviorContract withConfirmedSource(StructuredBehaviorContract contract,
                                                           SourceStateBindingBundle sources) {
        SourceStateBinding source = sources.bindings().stream()
                .filter(candidate -> candidate.requirementId().equalsIgnoreCase(contract.requirementId()))
                .findFirst().orElse(null);
        if (source == null || source.sourceRoute().isBlank()) return contract;
        String context = "sourceRoute: " + source.sourceRoute() + "\n" + contract.targetContext();
        return new StructuredBehaviorContract(contract.requirementId(), contract.capability(), contract.actions(),
                contract.assertions(), contract.dataRequirements(), context, contract.executable(), contract.reviewReasons());
    }

    private String contextValue(String context, String key) {
        return BehaviorTargetContext.parse(context).value(key);
    }

    private boolean routeMatches(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return !a.isBlank() && !b.isBlank() && (a.equals(b) || a.endsWith(b) || b.endsWith(a));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
    }
}
