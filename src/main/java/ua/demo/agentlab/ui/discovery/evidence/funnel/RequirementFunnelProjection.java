package ua.demo.agentlab.ui.discovery.evidence.funnel;

import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaPageInventory;
import ua.demo.agentlab.ui.discovery.spa.model.TargetedLocatorVerification;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBinding;
import ua.demo.agentlab.ui.capability.UiCapabilityContract;
import ua.demo.agentlab.ui.capability.UiCapabilityRegistry;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTrace;
import ua.demo.agentlab.ui.discovery.interaction.observability.EvidenceProjectionTraceEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Builds a deterministic requirement-by-requirement view of the UI evidence funnel. */
public final class RequirementFunnelProjection {

    private final UiCapabilityRegistry capabilityRegistry;
    private final RequirementEvidenceTraceResolver traceResolver;

    public RequirementFunnelProjection() {
        this(new UiCapabilityRegistry(), new RequirementEvidenceTraceResolver());
    }

    RequirementFunnelProjection(UiCapabilityRegistry capabilityRegistry,
                                RequirementEvidenceTraceResolver traceResolver) {
        this.capabilityRegistry = capabilityRegistry == null ? new UiCapabilityRegistry() : capabilityRegistry;
        this.traceResolver = traceResolver == null ? new RequirementEvidenceTraceResolver() : traceResolver;
    }

    public UiEvidenceRequirementResult project(
            UiEvidenceFunnelInput input,
            StructuredBehaviorContract requirement
    ) {
        Optional<UiCapabilityContract> capability = capabilityRegistry.resolve(requirement.capability());
        BoundSpaBehaviorContract binding = input.bindings().stream()
                .filter(candidate -> candidate.requirementId().equalsIgnoreCase(requirement.requirementId()))
                .findFirst()
                .orElse(null);
        UiEvidenceRequirementTrace trace = traceResolver.resolve(input, requirement, binding);
        SpaPageInventory page = resolvePage(input, requirement, binding);
        int rawCount = page == null ? 0 : pageLocators(page).size();
        int liveCount = liveLocatorCount(input, requirement.requirementId(), binding);
        int promptCount = promptLocatorCount(input, requirement.requirementId(), binding);
        int dbCount = dbLocatorsFor(input, requirement.requirementId(), binding).size();
        boolean requirementBound = binding != null && !binding.pageId().isBlank();
        boolean pomBindable = pomBindable(binding);
        boolean promptOwned = promptOwns(input, requirement.requirementId(), binding);
        boolean verifiedProvenance = liveCount > 0 || dbCount > 0;
        boolean promptEligible = capability.isPresent()
                && requirementBound
                && pomBindable
                && promptOwned
                && promptCount > 0
                && verifiedProvenance;

        if (promptEligible) {
            List<String> path = new ArrayList<>();
            path.add("requirement:" + requirement.requirementId());
            path.add("capability:" + capability.orElseThrow().id());
            appendTracePath(path, trace);
            path.add("page:" + binding.pageId());
            if (liveCount > 0) {
                path.add("live-verified-locators:" + liveCount);
            }
            if (dbCount > 0) {
                path.add("db-stable-locators:" + dbCount);
            }
            path.add("prompt-allowed-locators:" + promptCount);
            path.add("pom-eligible");
            return result(requirement, trace, binding, rawCount, liveCount, dbCount, promptCount,
                    true, true, path, "", "", "");
        }

        Stop stop = stopFor(input, requirement, capability, page, binding, promptOwned, promptCount, verifiedProvenance);
        List<String> partialPath = new ArrayList<>();
        partialPath.add("requirement:" + requirement.requirementId());
        capability.ifPresent(value -> partialPath.add("capability:" + value.id()));
        appendTracePath(partialPath, trace);
        return result(requirement, trace, binding, rawCount, liveCount, dbCount, promptCount,
                requirementBound, false, partialPath, stop.stage(), stop.reason(), stop.remediation());
    }

    /**
     * POM readiness is intentionally weaker than test execution readiness. Unresolved scenario
     * values can block a live test while a typed page method with a parameter is still valid.
     */
    private boolean pomBindable(BoundSpaBehaviorContract binding) {
        if (binding == null || binding.pageId().isBlank() || binding.route().isBlank()) {
            return false;
        }
        boolean hasActionContract = !binding.steps().isEmpty();
        boolean hasAssertionContract = binding.assertions().stream()
                .anyMatch(assertion -> assertion.verifiable());
        if (!hasActionContract && !hasAssertionContract) {
            return false;
        }
        return binding.reviewReasons().stream().noneMatch(this::structuralBindingFailure);
    }

    private boolean structuralBindingFailure(String reason) {
        String value = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        if (value.startsWith("missing data value") || value.startsWith("scenario dataset")) {
            return false;
        }
        return value.contains("no current-run page")
                || value.contains("no confirmed component")
                || value.contains("no confirmed locator binding")
                || value.contains("no confirmed action binding")
                || value.contains("no confirmed executable action")
                || value.contains("assertion targets have no confirmed")
                || value.contains("source state binding was not eligible");
    }

    private UiEvidenceRequirementResult result(
            StructuredBehaviorContract requirement,
            UiEvidenceRequirementTrace trace,
            BoundSpaBehaviorContract binding,
            int rawCount,
            int liveCount,
            int dbCount,
            int promptCount,
            boolean requirementBound,
            boolean promptEligible,
            List<String> path,
            String stoppedAt,
            String reason,
            String remediation
    ) {
        return new UiEvidenceRequirementResult(
                requirement.requirementId(),
                requirement.capability(),
                trace,
                binding == null ? "" : binding.pageId(),
                binding == null ? "" : binding.route(),
                rawCount,
                liveCount,
                dbCount,
                promptCount,
                requirementBound,
                promptEligible,
                promptEligible,
                path,
                stoppedAt,
                reason,
                remediation
        );
    }

    private void appendTracePath(List<String> path, UiEvidenceRequirementTrace trace) {
        if (trace == null) {
            return;
        }
        if (!trace.source().pageId().isBlank() || !trace.source().route().isBlank()) {
            path.add("source-state:" + stateLabel(trace.source()));
        }
        if (!trace.requiredComponentCapabilities().isEmpty()) {
            path.add("required-components:" + String.join(",", trace.requiredComponentCapabilities()));
        }
        if (!trace.expectedActionIntents().isEmpty()) {
            path.add("expected-actions:" + String.join(",", trace.expectedActionIntents()));
        }
        if (!trace.target().pageId().isBlank() || !trace.target().route().isBlank()) {
            path.add("target-state:" + stateLabel(trace.target()));
        }
        if (!trace.discoveryRouteSource().isBlank()) {
            path.add("route-source:" + trace.discoveryRouteSource());
        }
    }

    private String stateLabel(UiEvidenceStateReference state) {
        String identity = !state.pageId().isBlank() ? state.pageId() : state.pageName();
        if (identity.isBlank()) {
            identity = "unresolved-page";
        }
        String route = state.route().isBlank() ? "unresolved-route" : state.route();
        String stateId = state.stateId().isBlank() ? "" : "#" + state.stateId();
        return identity + "@" + route + stateId;
    }

    private Stop stopFor(
            UiEvidenceFunnelInput input,
            StructuredBehaviorContract requirement,
            Optional<UiCapabilityContract> capability,
            SpaPageInventory page,
            BoundSpaBehaviorContract binding,
            boolean promptOwned,
            int promptCount,
            boolean verifiedProvenance
    ) {
        if (capability.isEmpty()) {
            return new Stop("CAPABILITY_CLASSIFICATION", "requirement capability is not registered in the universal UI capability registry",
                    "Register the capability contract and its evidence/execution adapter before discovery.");
        }
        String dataReadinessReason = dataReadinessReason(binding);
        if (!dataReadinessReason.isBlank()) {
            return new Stop("DATA_READINESS", dataReadinessReason,
                    "Provide the required scenario dataset or ENV/system-property values, then rerun targeted verification.");
        }
        if (page == null) {
            TargetStateBinding liveTarget = matchingLiveTarget(input, requirement);
            if (liveTarget != null) {
                return new Stop("TARGET_STATE_MAPPING",
                        "Live browser confirmed target route '" + liveTarget.targetRoute()
                                + "', but that state has not been converted into current-run page/component inventory.",
                        "Capture the live target DOM, build its PageModel/component inventory, and repeat source-state binding for dependent requirements.");
            }
            String detail = exactReason(binding,
                    input.liveVerification() == null ? List.of() : input.liveVerification().sourceTrace(),
                    input.inventory() == null ? List.of() : input.inventory().sourceTrace(),
                    "no inventory page exposes the required capability");
            return new Stop("DISCOVERY", detail,
                    "Run inventory or targeted discovery for " + capability.orElseThrow().id() + ".");
        }
        if (binding == null || binding.pageId().isBlank()) {
            return new Stop("REQUIREMENT_BINDING",
                    bindingReason(binding, "requirement is not bound to a confirmed page/component"),
                    "Confirm a page, component, action, and route owned by the requirement.");
        }
        TargetStateBinding requirementTarget = confirmedTarget(input, requirement.requirementId());
        if (!binding.executable() && requirementTarget != null && binding.reviewReasons().stream()
                .anyMatch(reason -> reason.toLowerCase(Locale.ROOT).contains("assertion target"))) {
            return new Stop("TARGET_STATE_MAPPING",
                    "Live browser confirmed target route '" + requirementTarget.targetRoute()
                            + "', but target-page assertion components have not been mapped into current-run inventory: "
                            + String.join("; ", binding.reviewReasons()),
                    "Capture the live target DOM and bind target-owned assertion components before POM evidence assembly.");
        }
        if (!binding.executable()) {
            String detail = binding.reviewReasons().isEmpty()
                    ? "structured behavior binding is not executable"
                    : String.join("; ", binding.reviewReasons());
            return new Stop("REQUIREMENT_BINDING", detail,
                    "Resolve the binding review reasons and rerun targeted verification.");
        }
        if (!verifiedProvenance) {
            String reason = exactReason(binding,
                    input.liveVerification() == null ? List.of() : input.liveVerification().sourceTrace(),
                    List.of(), "no live-verified or DB-stable locator backs the bound requirement");
            return new Stop("LIVE_OR_DB_VERIFICATION", reason,
                    "Verify the required component locator in a live browser or promote validated runtime evidence in the DB.");
        }
        if (!promptOwned) {
            return new Stop("PROMPT_OWNERSHIP", "PromptUiEvidence does not own this requirement/page",
                    "Re-run page ownership slicing and assemble page-scoped prompt evidence from the current requirement set.");
        }
        if (promptCount <= 0) {
            return new Stop("LOCATOR_SELECTION", "no confirmed locator survived into prompt-allowed evidence",
                    "Inspect locator safety, provenance, and requirement relevance filters for the bound component.");
        }
        return new Stop("PROMPT_ELIGIBILITY", "page did not satisfy the final POM eligibility contract",
                "Inspect the funnel report and page eligibility artifact before invoking the LLM.");
    }

    private SpaPageInventory resolvePage(
            UiEvidenceFunnelInput input,
            StructuredBehaviorContract requirement,
            BoundSpaBehaviorContract binding
    ) {
        if (input.inventory() == null) {
            return null;
        }
        if (binding != null && !binding.pageId().isBlank()) {
            Optional<SpaPageInventory> exact = input.inventory().pages().stream()
                    .filter(page -> page.pageId().equalsIgnoreCase(binding.pageId()))
                    .findFirst();
            if (exact.isPresent()) {
                return exact.get();
            }
        }
        String capability = normalizeCapability(requirement.capability());
        return input.inventory().pages().stream()
                .filter(page -> capabilityTokens(page.capability()).contains(capability))
                .findFirst()
                .orElse(null);
    }

    private Set<String> rawLocators(UiEvidenceFunnelInput input) {
        Set<String> locators = new LinkedHashSet<>();
        if (input.inventory() == null) {
            return locators;
        }
        input.inventory().pages().forEach(page -> locators.addAll(pageLocators(page)));
        return locators;
    }

    private Set<String> pageLocators(SpaPageInventory page) {
        Set<String> locators = new LinkedHashSet<>();
        if (page == null) {
            return locators;
        }
        for (SemanticComponentInventory component : page.components()) {
            component.locators().stream().map(this::candidateKey).forEach(locators::add);
        }
        return locators;
    }

    private Set<String> liveVerifiedLocators(UiEvidenceFunnelInput input) {
        Set<String> locators = new LinkedHashSet<>();
        if (input.liveVerification() == null) {
            return locators;
        }
        input.liveVerification().locatorVerifications().stream()
                .filter(TargetedLocatorVerification::verified)
                .map(this::verificationKey)
                .forEach(locators::add);
        return locators;
    }

    private Set<String> liveLocatorsFor(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        Set<String> locators = new LinkedHashSet<>();
        if (input.liveVerification() == null) {
            return locators;
        }
        input.liveVerification().locatorVerifications().stream()
                .filter(TargetedLocatorVerification::verified)
                .filter(verification -> verification.requirementIds().stream()
                        .anyMatch(requirementId::equalsIgnoreCase)
                        || bindingLocatorIds(binding).contains(verification.locatorId()))
                .map(this::verificationKey)
                .forEach(locators::add);
        return locators;
    }

    private int liveLocatorCount(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        Set<String> projected = projectionEntriesFor(input, requirementId, binding).stream()
                .filter(EvidenceProjectionTraceEntry::liveVerified)
                .map(EvidenceProjectionTraceEntry::locatorEvidenceId)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return projected.isEmpty() ? liveLocatorsFor(input, requirementId, binding).size() : projected.size();
    }

    private Set<String> dbStableLocators(UiEvidenceFunnelInput input) {
        Set<String> locators = new LinkedHashSet<>();
        if (input.aiContext() == null) {
            return locators;
        }
        input.aiContext().dbStableLocatorEvidence().stream()
                .map(this::promptKey)
                .forEach(locators::add);
        return locators;
    }

    private Set<String> dbLocatorsFor(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        if (!promptOwns(input, requirementId, binding)) {
            return Set.of();
        }
        Set<String> db = dbStableLocators(input);
        Set<String> selected = new LinkedHashSet<>(promptLocatorsFor(input, requirementId, binding));
        selected.retainAll(db);
        return selected;
    }

    private Set<String> promptAllowedLocators(UiEvidenceFunnelInput input) {
        Set<String> locators = new LinkedHashSet<>();
        PromptUiEvidence evidence = promptEvidence(input);
        evidence.requiredLocators().stream().map(this::promptKey).forEach(locators::add);
        return locators;
    }

    private Set<String> requirementScopedPromptLocators(UiEvidenceFunnelInput input) {
        Set<String> locators = new LinkedHashSet<>();
        for (StructuredBehaviorContract requirement : input.requirements()) {
            BoundSpaBehaviorContract binding = input.bindings().stream()
                    .filter(candidate -> candidate.requirementId().equalsIgnoreCase(requirement.requirementId()))
                    .findFirst()
                    .orElse(null);
            locators.addAll(promptLocatorsFor(input, requirement.requirementId(), binding));
        }
        return locators;
    }

    private Set<String> promptLocatorsFor(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        if (!promptOwns(input, requirementId, binding)) {
            return Set.of();
        }
        Set<String> locatorIds = bindingLocatorIds(binding);
        if (locatorIds.isEmpty()) {
            return Set.of();
        }
        Set<String> selected = new LinkedHashSet<>();
        promptLocatorCandidates(input).stream()
                .filter(locator -> locator.evidenceType() == ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType.CONFIRMED_LOCATOR)
                .filter(locator -> locatorIds.stream().anyMatch(locatorId -> locator.sourceTrace().stream()
                        .anyMatch(trace -> trace.equalsIgnoreCase("spa-locator-id:" + locatorId)
                                || trace.equalsIgnoreCase("db-stable-locator:" + locatorId))))
                .map(this::promptKey)
                .forEach(selected::add);
        return selected;
    }

    private int promptLocatorCount(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        Set<String> projected = projectionEntriesFor(input, requirementId, binding).stream()
                .filter(EvidenceProjectionTraceEntry::promptAllowed)
                .map(EvidenceProjectionTraceEntry::locatorEvidenceId)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return projected.isEmpty() ? promptLocatorsFor(input, requirementId, binding).size() : projected.size();
    }

    private Set<String> bindingLocatorIds(BoundSpaBehaviorContract binding) {
        Set<String> locatorIds = new LinkedHashSet<>();
        if (binding == null) {
            return locatorIds;
        }
        binding.steps().stream()
                .map(step -> step.locatorId())
                .filter(locatorId -> !locatorId.isBlank())
                .forEach(locatorIds::add);
        binding.assertions().stream()
                .map(assertion -> assertion.locatorId())
                .filter(locatorId -> !locatorId.isBlank())
                .forEach(locatorIds::add);
        return locatorIds;
    }

    private boolean promptOwns(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        if (binding == null) return false;
        if (projectionEntriesFor(input, requirementId, binding).stream()
                .anyMatch(EvidenceProjectionTraceEntry::promptAllowed)) {
            return true;
        }
        Set<String> locatorIds = bindingLocatorIds(binding);
        boolean locatorAndPageProvenance = promptLocatorCandidates(input).stream().anyMatch(locator ->
                locator.sourceTrace().stream().anyMatch(trace -> locatorIds.stream().anyMatch(locatorId ->
                        trace.equalsIgnoreCase("spa-locator-id:" + locatorId)
                                || trace.equalsIgnoreCase("db-stable-locator:" + locatorId)))
                        && locator.sourceTrace().stream().anyMatch(trace -> pageTraceMatches(trace, binding)));
        if (locatorAndPageProvenance) {
            return true;
        }
        if (input.aiContext() == null || input.aiContext().mappedUiKnowledge() == null) return false;
        return input.aiContext().mappedUiKnowledge().pages().stream().anyMatch(page ->
                (!binding.pageId().isBlank() && binding.pageId().equalsIgnoreCase(page.pageId()))
                        || (!binding.route().isBlank()
                        && ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.routeEqualsOrSuffix(
                        binding.route(), page.urlPattern().isBlank() ? page.url() : page.urlPattern())));
    }

    private List<EvidenceProjectionTraceEntry> projectionEntriesFor(
            UiEvidenceFunnelInput input,
            String requirementId,
            BoundSpaBehaviorContract binding
    ) {
        EvidenceProjectionTrace trace = input == null ? null : input.projectionTrace();
        if (trace == null || trace.entries().isEmpty() || requirementId == null || requirementId.isBlank()) {
            return List.of();
        }
        return trace.entries().stream()
                .filter(entry -> entry.requirementIds().stream().anyMatch(requirementId::equalsIgnoreCase))
                .toList();
    }

    private boolean pageTraceMatches(String trace, BoundSpaBehaviorContract binding) {
        if (trace == null || trace.isBlank()) return false;
        String normalized = trace.trim();
        if (!binding.pageId().isBlank() && (normalized.equalsIgnoreCase("spa-page-id:" + binding.pageId())
                || normalized.equalsIgnoreCase("page-id:" + binding.pageId()))) {
            return true;
        }
        int separator = normalized.indexOf(':');
        String value = separator < 0 ? "" : normalized.substring(separator + 1).trim();
        return !binding.route().isBlank()
                && (normalized.toLowerCase(Locale.ROOT).startsWith("spa-route:")
                || normalized.toLowerCase(Locale.ROOT).startsWith("page-route:"))
                && ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer.routeEqualsOrSuffix(binding.route(), value);
    }

    private List<PromptLocatorEvidence> promptLocatorCandidates(UiEvidenceFunnelInput input) {
        List<PromptLocatorEvidence> result = new ArrayList<>();
        result.addAll(promptEvidence(input).requiredLocators());
        if (input.aiContext() != null) {
            result.addAll(input.aiContext().confirmedCatalogLocatorEvidence());
            result.addAll(input.aiContext().dbStableLocatorEvidence());
        }
        return result;
    }

    private String dataReadinessReason(BoundSpaBehaviorContract binding) {
        if (binding == null) return "";
        return binding.reviewReasons().stream()
                .filter(reason -> {
                    String normalized = reason.toLowerCase(Locale.ROOT);
                    return normalized.contains("missing data value")
                            || normalized.contains("scenario dataset")
                            || normalized.contains("data requirement");
                })
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private TargetStateBinding matchingLiveTarget(UiEvidenceFunnelInput input, StructuredBehaviorContract requirement) {
        if (input.targetStateBindings() == null) return null;
        return input.targetStateBindings().targets().stream()
                .filter(TargetStateBinding::transitionConfirmed)
                .filter(target -> target.requirementId().equalsIgnoreCase(requirement.requirementId()))
                .filter(target -> !target.targetRoute().isBlank())
                .findFirst()
                .orElse(null);
    }

    private TargetStateBinding confirmedTarget(UiEvidenceFunnelInput input, String requirementId) {
        if (input.targetStateBindings() == null) return null;
        return input.targetStateBindings().targets().stream()
                .filter(TargetStateBinding::transitionConfirmed)
                .filter(target -> target.requirementId().equalsIgnoreCase(requirementId))
                .findFirst()
                .orElse(null);
    }

    private Set<String> semanticTokens(String value) {
        String splitCamel = value == null ? "" : value.replaceAll("([a-z])([A-Z])", "$1 $2");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : splitCamel.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() < 4 || Set.of("page", "route", "target", "source", "confirmed", "discovery").contains(token)) continue;
            if (token.endsWith("ies") && token.length() > 4) token = token.substring(0, token.length() - 3) + "y";
            else if (token.endsWith("s") && token.length() > 4) token = token.substring(0, token.length() - 1);
            tokens.add(token);
        }
        return tokens;
    }

    private PromptUiEvidence promptEvidence(UiEvidenceFunnelInput input) {
        return input.aiContext() == null
                ? PromptUiEvidence.empty("ui-evidence-funnel:no-ai-context")
                : input.aiContext().promptUiEvidence();
    }

    private int distinctBoundPages(List<UiEvidenceRequirementResult> results) {
        return (int) results.stream()
                .filter(UiEvidenceRequirementResult::requirementBound)
                .map(UiEvidenceRequirementResult::pageId)
                .filter(pageId -> !pageId.isBlank())
                .distinct()
                .count();
    }

    private int distinctPromptEligiblePages(List<UiEvidenceRequirementResult> results) {
        return (int) results.stream()
                .filter(UiEvidenceRequirementResult::promptEligible)
                .map(UiEvidenceRequirementResult::pageId)
                .filter(pageId -> !pageId.isBlank())
                .distinct()
                .count();
    }

    private Set<String> capabilityTokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        if (value == null) {
            return tokens;
        }
        for (String token : value.split("[|,]")) {
            String normalized = normalizeCapability(token);
            if (!normalized.isBlank()) {
                tokens.add(normalized);
            }
        }
        return tokens;
    }

    private String normalizeCapability(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String normalizeRoute(String value) {
        String route = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return route.length() > 1 && route.endsWith("/") ? route.substring(0, route.length() - 1) : route;
    }

    private String bindingReason(BoundSpaBehaviorContract binding, String fallback) {
        if (binding == null || binding.reviewReasons().isEmpty()) {
            return fallback;
        }
        return String.join("; ", binding.reviewReasons());
    }

    private String exactReason(
            BoundSpaBehaviorContract binding,
            List<String> primarySourceTrace,
            List<String> secondarySourceTrace,
            String fallback
    ) {
        String bindingReason = bindingReason(binding, "");
        if (!bindingReason.isBlank()) {
            return bindingReason;
        }
        return java.util.stream.Stream.concat(
                        primarySourceTrace == null ? java.util.stream.Stream.empty() : primarySourceTrace.stream(),
                        secondarySourceTrace == null ? java.util.stream.Stream.empty() : secondarySourceTrace.stream())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private String candidateKey(CandidateLocatorEvidence locator) {
        return locator.strategy() + "|" + locator.value();
    }

    private String verificationKey(TargetedLocatorVerification locator) {
        return locator.strategy() + "|" + locator.value();
    }

    private String promptKey(PromptLocatorEvidence locator) {
        return locator.strategy() + "|" + locator.value();
    }

    private record Stop(String stage, String reason, String remediation) {
    }
}
