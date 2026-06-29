package ua.demo.agentlab.ui.discovery;

import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.ui.discovery.model.UiDiscoveryResult;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.ConfirmedPageRegistry;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiFlow;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RuleBasedUiDiscoveryService implements UiDiscoveryService {

    @Override
    public UiDiscoveryResult discover(ProjectProfile projectProfile, NormalizedRequirementBundle requirementBundle) {
        if (projectProfile == null) {
            throw new IllegalArgumentException("projectProfile cannot be null");
        }
        if (requirementBundle == null) {
            throw new IllegalArgumentException("requirementBundle cannot be null");
        }

        ConfirmedPageRegistry confirmedPages =
                new ConfirmedPageSourceResolver().resolve(projectProfile, requirementBundle, List.of());
        Map<String, ConfirmedPageCandidate> definitionsByPage = new LinkedHashMap<>();
        for (ConfirmedPageCandidate candidate : confirmedPages.allPages()) {
            definitionsByPage.put(candidate.pageName(), candidate);
        }

        List<DiscoveredUiFlow> flows = new ArrayList<>();
        Set<String> referencedPageNames = new LinkedHashSet<>();

        for (NormalizedRequirement requirement : requirementBundle.requirements()) {
            if (!requirement.uiRelevant()) {
                continue;
            }

            DiscoveredUiFlow flow = discoverFlow(requirement, confirmedPages);
            flows.add(flow);
            referencedPageNames.add(flow.targetPageName());
            if (flow.sourcePageName() != null && !flow.sourcePageName().isBlank()) {
                referencedPageNames.add(flow.sourcePageName());
            }
        }

        if (referencedPageNames.isEmpty()) {
            referencedPageNames.add("HomePage");
        }

        List<DiscoveredUiPage> pages = referencedPageNames.stream()
                .map(definitionsByPage::get)
                .filter(candidate -> candidate != null)
                .map(candidate -> new DiscoveredUiPage(
                        candidate.pageName(),
                        candidate.route(),
                        inferCapabilities(candidate.capability()),
                        defaultHints(candidate.capability().canonicalPageType()),
                        "Profile and requirement driven discovery seed"
                ))
                .toList();

        UiDiscoverySnapshot snapshot = new UiDiscoverySnapshot(
                projectProfile.profileId(),
                projectProfile.projectName(),
                requirementBundle.source(),
                pages,
                flows
        );
        return new UiDiscoveryResult(snapshot, null);
    }

    private DiscoveredUiFlow discoverFlow(
            NormalizedRequirement requirement,
            ConfirmedPageRegistry confirmedPages
    ) {
        String flowType = detectFlowType(requirement);
        ConfirmedPageCandidate targetPage = resolveTargetPage(flowType, requirement, confirmedPages);
        String targetPageName = targetPage.pageName();
        String sourcePageName = resolveSourcePage(flowType, targetPageName);
        boolean authenticationRequired = requiresAuthentication(flowType, requirement);
        boolean needsEvidence = targetPage.route() == null || targetPage.route().isBlank();

        return new DiscoveredUiFlow(
                "flow-" + requirement.id(),
                requirement.title(),
                flowType,
                sourcePageName,
                resolveRoute(sourcePageName, confirmedPages),
                targetPageName,
                targetPage.route(),
                authenticationRequired,
                needsEvidence
                        ? buildNeedsEvidenceSteps(flowType, targetPageName)
                        : buildStepDescriptions(flowType, targetPageName),
                needsEvidence
                        ? buildNeedsEvidenceOutcomes(flowType, targetPageName)
                        : buildExpectedOutcomes(flowType, targetPageName),
                buildMatchKeywords(requirement),
                List.of(requirement.id())
        );
    }

    private String detectFlowType(NormalizedRequirement requirement) {
        String text = (requirement.title() + " " + requirement.statement()).toLowerCase(Locale.ROOT);

        if (containsAny(text, "forgot", "reset", "recovery", "lookup")) {
            return "RECOVERY";
        }
        if (containsAny(text, "register", "registration", "sign up")) {
            return "CREATE_ENTITY";
        }
        if (containsAny(text, "security", "otp", "mfa", "verification", "challenge")) {
            return "SECURITY_CHALLENGE";
        }
        if (containsAny(text, "details", "record", "transaction", "profile")) {
            return "OPEN_DETAILS";
        }
        if (containsAny(text, "logout", "log out", "sign out")) {
            return "LOGOUT";
        }
        if (containsAny(text, "transfer", "payment", "submit", "checkout", "create", "update", "edit")) {
            return "SUBMIT_FORM";
        }
        if (containsAny(text, "invalid", "failed attempt", "lock", "restricted", "wrong credential")) {
            return "AUTHENTICATE_NEGATIVE";
        }
        if (containsAny(text, "login", "sign in", "authenticate", "authenticated", "valid credential", "valid user")) {
            return "AUTHENTICATE";
        }
        return "NAVIGATE";
    }

    private ConfirmedPageCandidate resolveTargetPage(
            String flowType,
            NormalizedRequirement requirement,
            ConfirmedPageRegistry confirmedPages
    ) {
        PageCapability capability = capabilityForFlow(flowType, requirement);
        if (confirmedPages.findByCapability(capability).isPresent()) {
            return confirmedPages.findByCapability(capability).orElseThrow();
        }
        if (capability == PageCapability.AUTHENTICATED_AREA
                && confirmedPages.findByCapability(PageCapability.DASHBOARD).isPresent()) {
            return confirmedPages.findByCapability(PageCapability.DASHBOARD).orElseThrow();
        }
        String route = routeFromRequirement(requirement);
        return new ConfirmedPageCandidate(
                capability.defaultPageName(),
                route == null ? "" : route,
                capability,
                null,
                route == null || route.isBlank() ? 0.0d : 0.72d,
                List.of("needs confirmed evidence")
        );
    }

    private PageCapability capabilityForFlow(String flowType, NormalizedRequirement requirement) {
        String text = requirement == null ? "" : (requirement.title() + " " + requirement.statement()).toLowerCase(Locale.ROOT);
        return switch (flowType) {
            case "RECOVERY" -> PageCapability.RECOVERY;
            case "CREATE_ENTITY" -> PageCapability.REGISTRATION;
            case "SECURITY_CHALLENGE" -> PageCapability.SECURITY;
            case "SUBMIT_FORM" -> PageCapability.FORM;
            case "AUTHENTICATE", "AUTHENTICATE_NEGATIVE" -> PageCapability.AUTHENTICATION;
            case "LOGOUT" -> PageCapability.AUTHENTICATED_AREA;
            case "OPEN_DETAILS" -> PageCapability.RECORD_DETAILS;
            default -> containsAny(text, "list", "table", "grid", "results", "collection")
                    ? PageCapability.RECORD_LIST
                    : PageCapability.NAVIGATION;
        };
    }

    private String resolveSourcePage(String flowType, String targetPageName) {
        return switch (flowType) {
            case "RECOVERY" -> "LoginPage";
            case "LOGOUT" -> "SecureAreaPage";
            case "AUTHENTICATE", "AUTHENTICATE_NEGATIVE", "SECURITY_CHALLENGE" -> "LoginPage";
            default -> targetPageName;
        };
    }

    private List<String> buildStepDescriptions(String flowType, String targetPageName) {
        return switch (flowType) {
            case "RECOVERY" -> List.of(
                    "Open the recovery entry point",
                    "Navigate to " + targetPageName,
                    "Submit the recovery request"
            );
            case "CREATE_ENTITY" -> List.of(
                    "Open " + targetPageName,
                    "Fill the required fields",
                    "Submit the form"
            );
            case "SECURITY_CHALLENGE" -> List.of(
                    "Authenticate with a valid profile",
                    "Observe the security follow-up step"
            );
            case "SUBMIT_FORM" -> List.of(
                    "Open " + targetPageName,
                    "Provide the required scenario data",
                    "Submit the form"
            );
            case "NAVIGATE" -> List.of(
                    "Open " + targetPageName,
                    "Inspect the available list or summary content"
            );
            case "LOGOUT" -> List.of(
                    "Enter an authenticated area",
                    "Trigger the sign-out action"
            );
            case "AUTHENTICATE_NEGATIVE" -> List.of(
                    "Open LoginPage",
                    "Enter invalid or restricted credentials",
                    "Submit the authentication form"
            );
            default -> List.of(
                    "Open LoginPage",
                    "Enter valid credentials",
                    "Submit the authentication form"
            );
        };
    }

    private List<String> buildNeedsEvidenceSteps(String flowType, String targetPageName) {
        return List.of(
                "Possible intent: " + flowType,
                "Required evidence: explicit route, discovered link, or stable cached page for " + targetPageName,
                "Status: needs evidence"
        );
    }

    private List<String> buildNeedsEvidenceOutcomes(String flowType, String targetPageName) {
        return List.of(
                "No concrete page should be generated for " + targetPageName + " until evidence confirms " + flowType
        );
    }

    private List<String> buildExpectedOutcomes(String flowType, String targetPageName) {
        return switch (flowType) {
            case "RECOVERY" -> List.of(
                    "Recovery flow is reachable",
                    "Recovery confirmation or next-step message is visible"
            );
            case "CREATE_ENTITY" -> List.of(
                    "Submission succeeds",
                    "Success confirmation is visible on " + targetPageName
            );
            case "SECURITY_CHALLENGE" -> List.of(
                    "Security-related behavior is visible",
                    "Application remains in a safe state"
            );
            case "SUBMIT_FORM" -> List.of(
                    "Form submission succeeds",
                    "Confirmation is visible"
            );
            case "NAVIGATE" -> List.of(
                    targetPageName + " is reachable",
                    "At least one representative item or content block is displayed"
            );
            case "LOGOUT" -> List.of(
                    "User leaves the authenticated area",
                    "Authentication is required again"
            );
            case "AUTHENTICATE_NEGATIVE" -> List.of(
                    "Error or validation feedback is visible",
                    "User remains on the authentication flow"
            );
            default -> List.of(
                    "Authentication succeeds",
                    "User reaches an authenticated area"
            );
        };
    }

    private List<String> inferCapabilities(PageCapability capability) {
        return List.of(
                capability.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                capability.canonicalPageType().mappedType()
        );
    }

    private List<String> buildMatchKeywords(NormalizedRequirement requirement) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeywordTokens(keywords, requirement.title());
        addKeywordTokens(keywords, requirement.statement());
        keywords.addAll(requirement.tags());
        return new ArrayList<>(keywords);
    }

    private void addKeywordTokens(Set<String> keywords, String text) {
        for (String token : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (token.length() >= 4) {
                keywords.add(token);
            }
        }
    }

    private boolean requiresAuthenticatedArea(String flowType) {
        return switch (flowType) {
            case "SUBMIT_FORM", "LOGOUT", "SECURITY_CHALLENGE" -> true;
            default -> false;
        };
    }

    private boolean requiresAuthentication(String flowType, NormalizedRequirement requirement) {
        String text = (requirement.title() + " " + requirement.statement()).toLowerCase(Locale.ROOT);
        if (containsAny(text, "without authentication", "without login", "public", "guest")) {
            return false;
        }
        if (containsAny(text, "after login", "authenticated", "authorized", "signed in")) {
            return true;
        }
        return requiresAuthenticatedArea(flowType);
    }

    private String resolveRoute(String pageName, ConfirmedPageRegistry confirmedPages) {
        return confirmedPages.findByPageName(pageName)
                .map(ConfirmedPageCandidate::route)
                .orElse("");
    }

    private String routeFromRequirement(NormalizedRequirement requirement) {
        String text = requirement == null ? "" : requirement.title() + " " + requirement.statement();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/[a-zA-Z0-9/_\\-.]+").matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private List<LocatorHint> defaultHints(CanonicalPageType pageType) {
        return switch (pageType) {
            case AUTHENTICATION -> List.of(
                    new LocatorHint("username input", "css", "input[name='username'], input#username, input[type='email'], input[name*='user' i]"),
                    new LocatorHint("password input", "css", "input[name='password'], input#password, input[type='password']"),
                    new LocatorHint("submit button", "css", "button[type='submit'], input[type='submit']"),
                    new LocatorHint("status message", "css", "#flash, .flash, .success, .error, [role='alert'], [role='status']")
            );
            case REGISTRATION, RECOVERY, FORM -> List.of(
                    new LocatorHint("form field", "css", "input, select, textarea"),
                    new LocatorHint("submit button", "css", "button[type='submit'], input[type='submit']"),
                    new LocatorHint("status message", "css", "#flash, .flash, .success, .error, [role='alert'], [role='status']")
            );
            case AUTHENTICATED_AREA, SECURITY, DASHBOARD -> List.of(
                    new LocatorHint("page title", "css", "h1, h2"),
                    new LocatorHint("status message", "css", "#flash, .flash, .success, [role='status']"),
                    new LocatorHint("logout control", "css", "a[href*='logout'], button[type='submit'], input[type='submit']")
            );
            default -> List.of(
                    new LocatorHint("navigation link", "css", "nav a, header a, main a, .content a"),
                    new LocatorHint("page title", "css", "h1, h2"),
                    new LocatorHint("main content", "css", "main, .main-content, .content, #content")
            );
        };
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
