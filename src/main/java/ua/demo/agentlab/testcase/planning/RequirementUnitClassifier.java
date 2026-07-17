package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceCategory;
import ua.demo.agentlab.testcase.governance.RequirementGovernanceClassifier;

import java.util.List;
import java.util.Locale;

class RequirementUnitClassifier {

    private final RequirementGovernanceClassifier governanceClassifier = new RequirementGovernanceClassifier();
    private final ScenarioPageResolver pageResolver;

    RequirementUnitClassifier(ScenarioPageResolver pageResolver) {
        this.pageResolver = pageResolver;
    }

    RequirementUnit classify(NormalizedRequirement requirement) {
        var governance = governanceClassifier.classify(requirement);
        RequirementUnitType type = type(governance.category());
        RequirementCapability capability = capability(requirement);
        RequirementIntent intent = intent(requirement, type, capability);
        return new RequirementUnit(
                requirement,
                type,
                capability,
                intent,
                pageResolver.pageFor(requirement, capability),
                pageResolver.routeFor(requirement, capability)
        );
    }

    private RequirementUnitType type(RequirementGovernanceCategory category) {
        if (category == RequirementGovernanceCategory.EXECUTABLE_FUNCTIONAL) {
            return RequirementUnitType.FUNCTIONAL;
        }
        if (category == RequirementGovernanceCategory.ASSERTION_REQUIREMENT) {
            return RequirementUnitType.ASSERTION;
        }
        if (category == RequirementGovernanceCategory.ROUTE_EXPECTATION) {
            return RequirementUnitType.ROUTE_EXPECTATION;
        }
        if (category == RequirementGovernanceCategory.TEST_DATA_RULE) {
            return RequirementUnitType.DATA_RULE;
        }
        if (category == RequirementGovernanceCategory.OUT_OF_SCOPE) {
            return RequirementUnitType.OUT_OF_SCOPE;
        }
        return RequirementUnitType.POLICY;
    }

    private RequirementCapability capability(NormalizedRequirement requirement) {
        RequirementCapability declared = declaredCapability(requirement);
        if (declared != RequirementCapability.GENERIC) {
            return declared;
        }
        String text = text(requirement);
        if (containsAny(text, "logout", "sign out", "log out")) {
            return RequirementCapability.LOGOUT;
        }
        // Product vocabulary wins over generic authentication wording such as
        // "authenticated application area" in protected business requirements.
        if (containsAny(text, "recruitment", "vacancies", "vacancy", "module")) {
            return RequirementCapability.MODULE_NAVIGATION;
        }
        if (containsAny(text, "search results", "matching vacancy", "displayed vacancy", "displayed job title",
                "displayed hiring manager", "displayed status", "results are displayed")) {
            return RequirementCapability.RESULTS_COLLECTION;
        }
        if (containsAny(text, "search button", "click search", "search")) {
            return RequirementCapability.SEARCH;
        }
        if (containsAny(text, "job title", "hiring manager", "select a vacancy", "select a status",
                "selected filters", "filter")) {
            return RequirementCapability.FILTER;
        }
        if (containsAny(text, "authenticated area", "authenticated route", "successful login state",
                "authenticated user", "authenticated application", "welcome message", "dashboard", "secure area",
                "redirected to the authenticated")) {
            return RequirementCapability.AUTHENTICATED_AREA;
        }
        if (containsAny(text, "login", "sign in", "authenticate", "username", "password", "credentials", "home/login")) {
            return RequirementCapability.AUTHENTICATION;
        }
        if (containsAny(text, "form", "submit", "field", "input")) {
            return RequirementCapability.FORM;
        }
        if (containsAny(text, "navigate", "home page", "open the application", "route", "url", "accessible")) {
            return RequirementCapability.NAVIGATION;
        }
        if (containsAny(text, "list", "table", "collection", "grid")) {
            return RequirementCapability.RECORD_LIST;
        }
        if (containsAny(text, "details", "record", "profile")) {
            return RequirementCapability.RECORD_DETAILS;
        }
        if (containsAny(text, "cart", "basket", "container")) {
            return RequirementCapability.CONTAINER;
        }
        return RequirementCapability.GENERIC;
    }

    private RequirementIntent intent(NormalizedRequirement requirement, RequirementUnitType type, RequirementCapability capability) {
        String text = text(requirement);
        if (hasTag(requirement, "structured-requirement")) {
            if (capability == RequirementCapability.MODULE_NAVIGATION) return RequirementIntent.MODULE_NAVIGATION;
            if (capability == RequirementCapability.FILTER) return RequirementIntent.FILTER;
            if (capability == RequirementCapability.SEARCH) return RequirementIntent.SEARCH;
            if (capability == RequirementCapability.RESULTS_COLLECTION) return RequirementIntent.INSPECT_CONTENT;
        }
        if (type == RequirementUnitType.ROUTE_EXPECTATION || containsAny(text, "route", "url")) {
            return RequirementIntent.VERIFY_ROUTE;
        }
        if (containsAny(text, "logout", "sign out", "log out")) {
            return containsAny(text, "visible", "see") ? RequirementIntent.VERIFY_LOGOUT_AVAILABLE : RequirementIntent.LOGOUT;
        }
        if (capability == RequirementCapability.MODULE_NAVIGATION
                && containsAny(text, "open", "view", "recruitment", "vacancies")) {
            return RequirementIntent.MODULE_NAVIGATION;
        }
        if (containsAny(text, "select a", "select the", "select ")) {
            return RequirementIntent.SELECT_OPTION;
        }
        if (capability == RequirementCapability.SEARCH && containsAny(text, "click", "search")) {
            return RequirementIntent.SEARCH;
        }
        if (capability == RequirementCapability.FILTER && containsAny(text, "apply", "filter", "selected")) {
            return RequirementIntent.FILTER;
        }
        if (containsAny(text, "navigate")) {
            return RequirementIntent.NAVIGATE;
        }
        if (containsAny(text, "open the application", "open target", "open the target", "open the", "home page")) {
            return containsAny(text, "accessible") ? RequirementIntent.VERIFY_PAGE_ACCESSIBLE : RequirementIntent.OPEN_PAGE;
        }
        if (containsAny(text, "visible", "displays", "display", "see", "field is", "button is")) {
            return capability == RequirementCapability.AUTHENTICATED_AREA
                    ? RequirementIntent.VERIFY_AUTHENTICATED_AREA
                    : RequirementIntent.VERIFY_ELEMENT_VISIBLE;
        }
        if (containsAny(text, "submit")) {
            return RequirementIntent.SUBMIT_FORM;
        }
        if (containsAny(text, "enter", "type", "provided by")) {
            return RequirementIntent.ENTER_DATA;
        }
        if (containsAny(text, "login", "authenticate", "valid credentials", "redirected")) {
            return RequirementIntent.AUTHENTICATE;
        }
        if (containsAny(text, "open", "accessible", "home page")) {
            return containsAny(text, "accessible") ? RequirementIntent.VERIFY_PAGE_ACCESSIBLE : RequirementIntent.OPEN_PAGE;
        }
        return RequirementIntent.INSPECT_CONTENT;
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private RequirementCapability declaredCapability(NormalizedRequirement requirement) {
        if (requirement == null || requirement.tags() == null) return RequirementCapability.GENERIC;
        for (String tag : requirement.tags()) {
            String normalized = normalize(tag);
            if (!normalized.startsWith("capability-")) continue;
            String value = normalized.substring("capability-".length()).replace('-', '_');
            try { return RequirementCapability.valueOf(value.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return RequirementCapability.GENERIC; }
        }
        return RequirementCapability.GENERIC;
    }

    private boolean hasTag(NormalizedRequirement requirement, String expected) {
        return requirement != null && requirement.tags() != null
                && requirement.tags().stream().anyMatch(tag -> normalize(tag).equals(normalize(expected)));
    }

    private String text(NormalizedRequirement requirement) {
        if (requirement == null) {
            return "";
        }
        return normalize(requirement.title() + " " + requirement.statement() + " "
                + String.join(" ", requirement.tags() == null ? List.of() : requirement.tags()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
