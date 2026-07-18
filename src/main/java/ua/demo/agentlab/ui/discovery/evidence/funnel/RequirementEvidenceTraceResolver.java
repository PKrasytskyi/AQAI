package ua.demo.agentlab.ui.discovery.evidence.funnel;

import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.RequirementStateTransition;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBinding;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateTransition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Resolves the BW-04 trace strictly from the existing binding and live-transition artifacts. */
public final class RequirementEvidenceTraceResolver {

    public UiEvidenceRequirementTrace resolve(
            UiEvidenceFunnelInput input,
            StructuredBehaviorContract requirement,
            BoundSpaBehaviorContract behaviorBinding
    ) {
        if (input == null || requirement == null) {
            return UiEvidenceRequirementTrace.empty();
        }
        SourceStateBinding sourceBinding = sourceBinding(input, requirement.requirementId());
        RequirementStateTransition transition = transition(input, requirement.requirementId());
        TargetStateBinding targetBinding = targetBinding(input, requirement.requirementId());

        SpaPageInventory sourcePage = sourcePage(input, sourceBinding, behaviorBinding, transition);
        SpaPageInventory targetPage = targetPage(input, targetBinding, transition, behaviorBinding);
        String sourceStateId = sourceStateId(input, sourceBinding, transition, sourcePage);
        String targetStateId = targetStateId(targetBinding, transition, targetPage);

        UiEvidenceStateReference source = new UiEvidenceStateReference(
                firstNonBlank(value(sourceBinding, SourceStateBinding::sourcePageId),
                        value(transition, RequirementStateTransition::sourcePageId), pageId(sourcePage)),
                pageName(sourcePage),
                firstNonBlank(value(sourceBinding, SourceStateBinding::sourceRoute),
                        value(transition, RequirementStateTransition::sourceRoute), route(sourcePage)),
                sourceStateId,
                sourceRouteSource(requirement, sourceBinding, transition, sourcePage),
                sourceBinding != null && sourceBinding.liveVerificationEligible()
                        || transition != null && transition.confirmed()
        );
        String resolvedTargetRoute = firstNonBlank(targetRoute(targetBinding, transition), route(targetPage),
                behaviorBinding == null ? "" : behaviorBinding.route());
        boolean targetConfirmed = targetBinding != null && targetBinding.transitionConfirmed()
                || transition != null && transition.confirmed()
                || source.confirmed() && normalizeRoute(source.route()).equals(normalizeRoute(resolvedTargetRoute));
        UiEvidenceStateReference target = new UiEvidenceStateReference(
                firstNonBlank(pageId(targetPage), pageIdForRoute(input, resolvedTargetRoute)),
                pageName(targetPage),
                resolvedTargetRoute,
                targetStateId,
                targetRouteSource(requirement, targetBinding, transition, targetPage),
                targetConfirmed
        );

        return new UiEvidenceRequirementTrace(
                source,
                componentCapabilities(input, requirement, sourceBinding, behaviorBinding),
                componentIds(sourceBinding, behaviorBinding),
                actionIntents(input, requirement, sourceBinding, behaviorBinding),
                target,
                !target.route().isBlank() ? target.routeSource() : source.routeSource()
        );
    }

    private SourceStateBinding sourceBinding(UiEvidenceFunnelInput input, String requirementId) {
        return input.sourceStateBindings().bindings().stream()
                .filter(binding -> binding.requirementId().equalsIgnoreCase(requirementId))
                .findFirst().orElse(null);
    }

    private RequirementStateTransition transition(UiEvidenceFunnelInput input, String requirementId) {
        return input.liveTransitionDiscovery().transitions().stream()
                .filter(candidate -> candidate.requirementId().equalsIgnoreCase(requirementId))
                .findFirst().orElse(null);
    }

    private TargetStateBinding targetBinding(UiEvidenceFunnelInput input, String requirementId) {
        return input.targetStateBindings().targets().stream()
                .filter(candidate -> candidate.requirementId().equalsIgnoreCase(requirementId))
                .findFirst().orElse(null);
    }

    private SpaPageInventory sourcePage(
            UiEvidenceFunnelInput input,
            SourceStateBinding source,
            BoundSpaBehaviorContract behavior,
            RequirementStateTransition transition
    ) {
        return findPage(input, firstNonBlank(
                value(source, SourceStateBinding::sourcePageId),
                value(transition, RequirementStateTransition::sourcePageId),
                behavior == null ? "" : behavior.pageId()), firstNonBlank(
                value(source, SourceStateBinding::sourceRoute),
                value(transition, RequirementStateTransition::sourceRoute),
                behavior == null ? "" : behavior.route()));
    }

    private SpaPageInventory targetPage(
            UiEvidenceFunnelInput input,
            TargetStateBinding target,
            RequirementStateTransition transition,
            BoundSpaBehaviorContract behavior
    ) {
        String targetRoute = targetRoute(target, transition);
        SpaPageInventory page = findPage(input, "", targetRoute);
        if (page != null) {
            return page;
        }
        return behavior == null ? null : findPage(input, behavior.pageId(), behavior.route());
    }

    private SpaPageInventory findPage(UiEvidenceFunnelInput input, String pageId, String route) {
        if (input.inventory() == null) {
            return null;
        }
        return input.inventory().pages().stream()
                .filter(page -> !pageId.isBlank() && page.pageId().equalsIgnoreCase(pageId)
                        || !route.isBlank() && normalizeRoute(page.route()).equals(normalizeRoute(route)))
                .findFirst().orElse(null);
    }

    private String pageIdForRoute(UiEvidenceFunnelInput input, String route) {
        SpaPageInventory page = findPage(input, "", route);
        return page == null ? "" : page.pageId();
    }

    private String sourceStateId(
            UiEvidenceFunnelInput input,
            SourceStateBinding source,
            RequirementStateTransition requirementTransition,
            SpaPageInventory page
    ) {
        String actionId = requirementTransition == null ? "" : requirementTransition.actionId();
        if (input.liveVerification() != null) {
            String stateId = input.liveVerification().stateGraph().transitions().stream()
                    .filter(transition -> !actionId.isBlank() && transition.actionId().equalsIgnoreCase(actionId))
                    .map(UiStateTransition::fromStateId)
                    .filter(value -> !value.isBlank())
                    .findFirst().orElse("");
            if (!stateId.isBlank()) {
                return stateId;
            }
            String pageId = firstNonBlank(value(source, SourceStateBinding::sourcePageId), pageId(page));
            return input.liveVerification().stateGraph().states().stream()
                    .filter(state -> !pageId.isBlank() && state.pageId().equalsIgnoreCase(pageId))
                    .map(UiStateSnapshot::stateId)
                    .filter(value -> !value.isBlank())
                    .findFirst().orElse(page == null ? "" : page.pageFingerprintHash());
        }
        return page == null ? "" : page.pageFingerprintHash();
    }

    private String targetStateId(
            TargetStateBinding target,
            RequirementStateTransition transition,
            SpaPageInventory page
    ) {
        return firstNonBlank(value(target, TargetStateBinding::targetStateId),
                value(transition, RequirementStateTransition::targetStateId),
                page == null ? "" : page.pageFingerprintHash());
    }

    private List<String> componentIds(SourceStateBinding source, BoundSpaBehaviorContract behavior) {
        Set<String> values = new LinkedHashSet<>();
        if (source != null) {
            values.addAll(source.componentIds());
        }
        if (behavior != null) {
            values.addAll(behavior.componentIds());
        }
        return List.copyOf(values);
    }

    private List<String> componentCapabilities(
            UiEvidenceFunnelInput input,
            StructuredBehaviorContract requirement,
            SourceStateBinding source,
            BoundSpaBehaviorContract behavior
    ) {
        Set<String> capabilities = new LinkedHashSet<>(
                contextValues(requirement.targetContext(), "componentCapability"));
        Set<String> selectedComponentIds = new LinkedHashSet<>(componentIds(source, behavior));
        if (input.inventory() != null && !selectedComponentIds.isEmpty()) {
            input.inventory().pages().stream()
                    .flatMap(page -> page.components().stream())
                    .filter(component -> selectedComponentIds.stream()
                            .anyMatch(id -> id.equalsIgnoreCase(component.componentId())))
                    .map(SemanticComponentInventory::type)
                    .filter(type -> type != null && type != ua.demo.agentlab.ui.discovery.component.model.ComponentType.UNKNOWN)
                    .map(Enum::name)
                    .forEach(capabilities::add);
        }
        return List.copyOf(capabilities);
    }

    private List<String> actionIntents(
            UiEvidenceFunnelInput input,
            StructuredBehaviorContract requirement,
            SourceStateBinding source,
            BoundSpaBehaviorContract behavior
    ) {
        Set<String> intents = new LinkedHashSet<>();
        requirement.actions().stream().map(this::classifyAction).filter(value -> !value.isBlank()).forEach(intents::add);
        if (behavior != null) {
            behavior.steps().stream().map(step -> normalizeIntent(step.kind())).filter(value -> !value.isBlank())
                    .forEach(intents::add);
        }
        if (source != null && input.inventory() != null) {
            Set<String> candidateIds = Set.copyOf(source.candidateActionIds());
            input.inventory().pages().stream().flatMap(page -> page.components().stream())
                    .flatMap(component -> component.actions().stream())
                    .filter(action -> candidateIds.contains(action.actionId()))
                    .map(CandidateActionEvidence::intent)
                    .map(this::normalizeIntent)
                    .filter(value -> !value.isBlank())
                    .forEach(intents::add);
        }
        return List.copyOf(intents);
    }

    private String classifyAction(String action) {
        String value = action == null ? "" : action.toLowerCase(Locale.ROOT);
        if (value.contains("username") || value.contains("password") || value.contains("enter ") || value.contains("type ")) {
            return "ENTER_TEXT";
        }
        if (value.contains("submit") || value.contains("authenticate") || value.contains("log in") || value.contains("login")) {
            return "AUTHENTICATE";
        }
        if (value.contains("user menu") || value.contains("account menu")) {
            return "OPEN_USER_MENU";
        }
        if (value.contains("logout") || value.contains("sign out")) {
            return "LOGOUT";
        }
        if (value.contains("filter")) return "FILTER";
        if (value.contains("search")) return "SEARCH";
        if (value.contains("select")) return "SELECT_OPTION";
        if (value.contains("modal")) return "OPEN_MODAL";
        if (value.contains("module") || value.contains("navigation")) return "MODULE_NAVIGATION";
        if (value.contains("inspect") || value.contains("visible")) return "INSPECT_PAGE_CONTENT";
        if (value.contains("open") || value.contains("navigate")) return "OPEN_ROUTE";
        if (value.contains("click")) return "CLICK";
        return "";
    }

    private String normalizeIntent(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.equals("TYPE") || normalized.equals("CLEAR_AND_TYPE") || normalized.equals("SEND_KEYS")) {
            return "ENTER_TEXT";
        }
        if (normalized.equals("OPEN_MODAL") && value.toLowerCase(Locale.ROOT).contains("menu")) {
            return "OPEN_USER_MENU";
        }
        return normalized;
    }

    private String sourceRouteSource(
            StructuredBehaviorContract requirement,
            SourceStateBinding source,
            RequirementStateTransition transition,
            SpaPageInventory page
    ) {
        if (transition != null && transition.confirmed()) return "LIVE_TRANSITION_DISCOVERY";
        if (usesProjectProfileRoute(requirement.targetContext(), "sourceRoute")) return "PROJECT_PROFILE";
        if (source != null && !source.sourceRoute().isBlank()) return "SOURCE_STATE_BINDING";
        if (page != null && !page.route().isBlank()) return "CURRENT_RUN_INVENTORY";
        return "UNRESOLVED";
    }

    private String targetRouteSource(
            StructuredBehaviorContract requirement,
            TargetStateBinding target,
            RequirementStateTransition transition,
            SpaPageInventory page
    ) {
        if (target != null && target.transitionConfirmed() || transition != null && transition.confirmed()) {
            return "LIVE_TRANSITION_DISCOVERY";
        }
        if (usesProjectProfileRoute(requirement.targetContext(), "targetRoute")) return "PROJECT_PROFILE";
        if (page != null && !page.route().isBlank()) return "CURRENT_RUN_INVENTORY";
        return "UNRESOLVED";
    }

    private boolean usesProjectProfileRoute(String context, String key) {
        return contextValue(context, key).toLowerCase(Locale.ROOT).startsWith("project-profile.");
    }

    private List<String> contextValues(String context, String key) {
        String value = contextValue(context, key);
        if (value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("[,|]"))
                .map(this::normalizeIntent)
                .filter(item -> !item.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private String contextValue(String context, String key) {
        if (context == null || context.isBlank()) return "";
        for (String line : context.split("\\R")) {
            String trimmed = line.trim().replaceFirst("^[*-]\\s*", "");
            int separator = trimmed.indexOf(':');
            if (separator > 0 && trimmed.substring(0, separator).trim().equalsIgnoreCase(key)) {
                return trimmed.substring(separator + 1).trim().replace("`", "");
            }
        }
        return "";
    }

    private String targetRoute(TargetStateBinding target, RequirementStateTransition transition) {
        return firstNonBlank(value(target, TargetStateBinding::targetRoute),
                value(transition, RequirementStateTransition::targetRoute));
    }

    private String pageId(SpaPageInventory page) { return page == null ? "" : page.pageId(); }
    private String pageName(SpaPageInventory page) { return page == null ? "" : page.pageName(); }
    private String route(SpaPageInventory page) { return page == null ? "" : page.route(); }

    private String normalizeRoute(String value) {
        String route = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return route.length() > 1 && route.endsWith("/") ? route.substring(0, route.length() - 1) : route;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private <T> String value(T source, java.util.function.Function<T, String> getter) {
        return source == null ? "" : firstNonBlank(getter.apply(source));
    }
}
