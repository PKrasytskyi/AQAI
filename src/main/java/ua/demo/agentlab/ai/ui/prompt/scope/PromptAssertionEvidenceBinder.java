package ua.demo.agentlab.ai.ui.prompt.scope;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Binds a business assertion to confirmed executable page evidence. */
public final class PromptAssertionEvidenceBinder {

    public Binding bind(AssertionContract assertion, String pageRoute, List<PromptReadyLocator> locators) {
        if (assertion == null) {
            return new Binding(null, "assertion contract is missing");
        }
        String type = assertion.type().name();
        String trace = "confirmed-catalog:" + assertion.sourceLine();
        if (isRoutePostcondition(type)) {
            if (pageRoute == null || pageRoute.isBlank()) {
                return new Binding(prompt(assertion, type, assertion.expectedValue(), "", trace),
                        type + " has no confirmed target route");
            }
            return new Binding(prompt(assertion, "URL_CONTAINS", pageRoute, "",
                    trace + "; normalized-from:" + type), "");
        }
        if ("ELEMENT_VISIBLE".equals(type)) {
            return bindElementVisibility(assertion, trace, locators);
        }
        if ("AUTHENTICATED_AREA_VISIBLE".equals(type)) {
            if (describesConcreteElementState(assertion.expectedValue())) {
                return bindElementVisibility(assertion, trace + "; normalized-from:" + type, locators);
            }
            if (pageRoute != null && !pageRoute.isBlank()) {
                return new Binding(prompt(assertion, "URL_CONTAINS", pageRoute, "",
                        trace + "; normalized-from:" + type), "");
            }
        }
        return new Binding(prompt(assertion, type, assertion.expectedValue(), "", trace), "");
    }

    private Binding bindElementVisibility(
            AssertionContract assertion,
            String trace,
            List<PromptReadyLocator> locators
    ) {
        String target = resolveTarget(assertion.expectedValue(), locators);
        String concreteExpectedValue = target.isBlank() ? assertion.expectedValue() : target;
        PromptReadyAssertion ready = prompt(assertion, "ELEMENT_VISIBLE", concreteExpectedValue, target, trace);
        return target.isBlank()
                ? new Binding(ready, "ELEMENT_VISIBLE(" + assertion.expectedValue()
                + ") has no confirmed target locator")
                : new Binding(ready, "");
    }

    private boolean isRoutePostcondition(String type) {
        return "AUTHENTICATED_AREA_ABSENT".equals(type) || "AUTHENTICATION_SUCCEEDED".equals(type);
    }

    private PromptReadyAssertion prompt(
            AssertionContract assertion,
            String type,
            String expectedValue,
            String targetLocatorId,
            String trace
    ) {
        return new PromptReadyAssertion(type, expectedValue, assertion.ownerPage(), targetLocatorId,
                trace, assertion.confidence());
    }

    private String resolveTarget(String expectedValue, List<PromptReadyLocator> locators) {
        String expected = normalize(expectedValue);
        String requiredSubject = requiredSubject(expected);
        return safe(locators).stream()
                .filter(locator -> !locator.id().isBlank())
                .filter(locator -> requiredSubject.isBlank()
                        || semanticEvidence(locator).contains(requiredSubject))
                .map(locator -> new Match(locator.id(), matchScore(expected, locator)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(Match::score).reversed().thenComparing(Match::locatorId))
                .map(Match::locatorId)
                .findFirst()
                .orElse("");
    }

    private String requiredSubject(String expected) {
        if (containsAny(expected, "logout", "sign out")) return "logout";
        if (containsAny(expected, "username")) return "username";
        if (containsAny(expected, "password")) return "password";
        if (containsAny(expected, "heading", "title")) return "heading";
        if (containsAny(expected, "login button", "submit button")) return "login";
        return "";
    }

    private String semanticEvidence(PromptReadyLocator locator) {
        return normalize(locator.id() + " " + locator.elementName() + " " + locator.role());
    }

    private boolean describesConcreteElementState(String value) {
        String text = normalize(value);
        return containsAny(text, "logout", "sign out", "user menu", "heading", "button", "field", "control")
                && containsAny(text, "visible", "enabled", "open", "display");
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) if (text.contains(fragment)) return true;
        return false;
    }

    private int matchScore(String expected, PromptReadyLocator locator) {
        String id = normalize(locator.id());
        String element = normalize(locator.elementName());
        int score = tokenMatch(expected, id) ? 3 : 0;
        if (tokenMatch(expected, element)) score = Math.max(score, 2);
        String role = normalize(locator.role());
        if (!role.isBlank() && tokenMatch(expected, role)) score++;
        return score;
    }

    private boolean tokenMatch(String text, String candidate) {
        if (text.isBlank() || candidate.isBlank()) return false;
        if (text.contains(candidate)) return true;
        return java.util.Arrays.stream(candidate.split(" "))
                .filter(token -> token.length() >= 4)
                .anyMatch(text::contains);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim().toLowerCase(Locale.ROOT);
    }

    private List<PromptReadyLocator> safe(List<PromptReadyLocator> locators) {
        return locators == null ? List.of() : locators;
    }

    public record Binding(PromptReadyAssertion assertion, String coverageGap) {
        public Binding {
            coverageGap = coverageGap == null ? "" : coverageGap.trim();
        }
    }

    private record Match(String locatorId, int score) {
    }
}
