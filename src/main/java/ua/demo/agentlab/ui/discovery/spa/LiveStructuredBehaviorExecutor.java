package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.requirements.normalization.model.StructuredAssertionRequirement;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorAssertion;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorStep;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Executes a safe, fully bound structured SPA behavior and verifies its typed postconditions. */
public final class LiveStructuredBehaviorExecutor {

    public SpaBehaviorExecutionResult execute(WebDriver driver, ProjectProfile profile, BoundSpaBehaviorContract contract,
                                              SpaInventoryBundle inventory, SpaInventoryConfig config) {
        List<String> locatorIds = contract.steps().stream().map(BoundSpaBehaviorStep::locatorId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)).stream().toList();
        List<String> actionIds = contract.steps().stream().map(BoundSpaBehaviorStep::actionId).distinct().toList();
        List<String> trace = new ArrayList<>(List.of("spa-behavior:bound-contract=" + contract.requirementId()));
        if (!contract.executable()) {
            return result(contract, "NEEDS_REVIEW", locatorIds, actionIds, contract.reviewReasons(), trace);
        }
        if (driver == null || inventory == null || config == null) {
            return result(contract, "NEEDS_REVIEW", locatorIds, actionIds, List.of("Browser, inventory, or SPA configuration is unavailable."), trace);
        }
        Map<String, CandidateLocatorEvidence> locators = locatorIndex(inventory);
        List<AssertionSnapshot> before = snapshot(driver, contract.assertions(), locators, inventory);
        try {
            for (BoundSpaBehaviorStep step : contract.steps()) {
                if (!allowed(step, contract, config)) {
                    return result(contract, "SKIPPED", locatorIds, actionIds,
                            List.of("Execution policy does not permit " + step.kind() + " for capability " + contract.capability() + "."), trace);
                }
                executeStep(driver, step, locators, inventory);
                trace.add("executed=" + step.kind() + ":" + step.actionId());
            }
        } catch (RuntimeException exception) {
            return result(contract, "FAILED", locatorIds, actionIds, List.of("Bound SPA action failed: " + concise(exception)), trace);
        }
        List<AssertionSnapshot> after = snapshot(driver, contract.assertions(), locators, inventory);
        List<String> failures = verify(driver, profile, contract, before, after, locators);
        return result(contract, failures.isEmpty() ? "PASSED" : "FAILED", locatorIds, actionIds, failures, trace);
    }

    private boolean allowed(BoundSpaBehaviorStep step, BoundSpaBehaviorContract contract, SpaInventoryConfig config) {
        String kind = step.kind().toUpperCase(Locale.ROOT);
        if ("SELECT".equals(kind) || "TYPE".equals(kind)) return config.executeDataActions();
        if (!"CLICK".equals(kind)) return false;
        String capability = contract.capability().replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        boolean destructive = capability.contains("DELETE") || capability.contains("CONFIRM");
        if (destructive) return config.executeSessionEndingActions();
        return config.executeSafeActions() && (!capability.contains("FILTER") || config.executeDataActions());
    }

    private void executeStep(WebDriver driver, BoundSpaBehaviorStep step,
                             Map<String, CandidateLocatorEvidence> locators, SpaInventoryBundle inventory) {
        CandidateLocatorEvidence locator = locators.get(step.locatorId());
        if (locator == null) throw new IllegalStateException("Bound locator is missing: " + step.locatorId());
        WebElement element = resolveElement(driver, locator, inventory);
        switch (step.kind().toUpperCase(Locale.ROOT)) {
            case "SELECT" -> select(element, step.value());
            case "TYPE" -> { element.clear(); element.sendKeys(step.value()); }
            case "CLICK" -> new WebDriverWait(driver, Duration.ofSeconds(8)).until(ExpectedConditions.elementToBeClickable(element)).click();
            default -> throw new IllegalArgumentException("Unsupported structured SPA action: " + step.kind());
        }
    }

    private void select(WebElement element, String value) {
        if ("select".equalsIgnoreCase(element.getTagName())) {
            new Select(element).selectByVisibleText(value);
            return;
        }
        element.click();
        element.sendKeys(value);
        element.sendKeys(Keys.ENTER);
    }

    private List<String> verify(WebDriver driver, ProjectProfile profile, BoundSpaBehaviorContract contract, List<AssertionSnapshot> before,
                                List<AssertionSnapshot> after, Map<String, CandidateLocatorEvidence> locators) {
        Map<String, AssertionSnapshot> beforeByTarget = byTarget(before);
        Map<String, AssertionSnapshot> afterByTarget = byTarget(after);
        List<String> failures = new ArrayList<>();
        for (BoundSpaBehaviorAssertion bound : contract.assertions()) {
            if (!bound.verifiable()) { failures.add(bound.reason()); continue; }
            StructuredAssertionRequirement assertion = bound.assertion();
            String type = normalize(assertion.type());
            AssertionSnapshot current = afterByTarget.get(assertion.target());
            AssertionSnapshot prior = beforeByTarget.get(assertion.target());
            String expected = resolveExpected(assertion.expectedValue(), contract.resolvedData());
            boolean passed = switch (type) {
                case "elementvisible" -> current != null && current.visible();
                case "countgreaterthan" -> current != null && current.count() > integer(expected);
                case "rowvisible", "datastatematches" -> current != null && contains(current.texts(), expected);
                case "resultschanged" -> current != null && !current.equals(prior);
                case "routechanged" -> routeChanged(driver, contract.route(), expected);
                case "authenticatedareavisible" -> profile != null && !profile.loginRoute().isBlank()
                        && !driver.getCurrentUrl().contains(profile.loginRoute());
                default -> false;
            };
            if (!passed) failures.add("Postcondition failed: " + assertion.type() + " target=" + assertion.target());
        }
        return List.copyOf(failures);
    }

    private boolean routeChanged(WebDriver driver, String route, String expected) {
        String url = driver.getCurrentUrl() == null ? "" : driver.getCurrentUrl();
        String expectedRoute = expected.contains("discovery-confirmed") ? "" : expected;
        return !expectedRoute.isBlank() ? url.contains(expectedRoute) : !route.isBlank() && !url.endsWith(route);
    }

    private List<AssertionSnapshot> snapshot(WebDriver driver, List<BoundSpaBehaviorAssertion> assertions,
                                              Map<String, CandidateLocatorEvidence> locators, SpaInventoryBundle inventory) {
        List<AssertionSnapshot> snapshots = new ArrayList<>();
        for (BoundSpaBehaviorAssertion assertion : assertions) {
            if (assertion.locatorId().isBlank()) continue;
            CandidateLocatorEvidence locator = locators.get(assertion.locatorId());
            if (locator == null) continue;
            List<WebElement> elements = resolveElements(driver, locator, inventory);
            snapshots.add(new AssertionSnapshot(assertion.assertion().target(), elements.size(),
                    elements.stream().anyMatch(WebElement::isDisplayed),
                    elements.stream().map(element -> element.getText() == null ? "" : element.getText().trim()).toList()));
        }
        return List.copyOf(snapshots);
    }

    private Map<String, AssertionSnapshot> byTarget(List<AssertionSnapshot> snapshots) {
        Map<String, AssertionSnapshot> result = new LinkedHashMap<>();
        snapshots.forEach(snapshot -> result.put(snapshot.target(), snapshot));
        return result;
    }

    private Map<String, CandidateLocatorEvidence> locatorIndex(SpaInventoryBundle inventory) {
        Map<String, CandidateLocatorEvidence> result = new LinkedHashMap<>();
        inventory.pages().forEach(page -> page.components().forEach(component ->
                component.locators().forEach(locator -> result.putIfAbsent(locator.locatorId(), locator))));
        return result;
    }

    private WebElement resolveElement(WebDriver driver, CandidateLocatorEvidence locator, SpaInventoryBundle inventory) {
        List<WebElement> elements = resolveElements(driver, locator, inventory);
        return elements.stream().filter(WebElement::isDisplayed).findFirst()
                .orElseThrow(() -> new IllegalStateException("No visible element for locator " + locator.locatorId()));
    }

    private List<WebElement> resolveElements(WebDriver driver, CandidateLocatorEvidence locator, SpaInventoryBundle inventory) {
        By by = by(locator.strategy(), locator.value());
        List<WebElement> global = driver.findElements(by);
        if (global.size() == 1 || inventory == null || locator.componentMatchCount() != 1) return global;
        return inventory.pages().stream().flatMap(page -> page.components().stream())
                .filter(component -> locator.componentId().equals(component.componentId()))
                .flatMap(component -> scopedMatches(driver, component.rootLocatorStrategy(), component.rootLocatorValue(), by).stream())
                .toList();
    }

    private List<WebElement> scopedMatches(WebDriver driver, String rootStrategy, String rootValue, By child) {
        if (rootStrategy == null || rootStrategy.isBlank() || rootValue == null || rootValue.isBlank()) return List.of();
        return driver.findElements(by(rootStrategy, rootValue)).stream().flatMap(root -> root.findElements(child).stream()).toList();
    }

    private By by(String strategy, String value) {
        return switch (strategy == null ? "" : strategy.toLowerCase(Locale.ROOT)) {
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "xpath" -> By.xpath(value);
            default -> By.cssSelector(value);
        };
    }

    private String resolveExpected(String value, Map<String, String> data) {
        String result = value == null ? "" : value;
        for (Map.Entry<String, String> entry : data.entrySet()) result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        return result;
    }

    private boolean contains(List<String> texts, String expected) {
        if (expected == null || expected.isBlank()) return false;
        List<String> expectedValues = java.util.Arrays.stream(expected.split(";"))
                .map(value -> value.contains("=") ? value.substring(value.indexOf('=') + 1) : value)
                .map(String::trim).filter(value -> !value.isBlank()).toList();
        String actual = String.join(" ", texts);
        return expectedValues.stream().allMatch(actual::contains);
    }

    private int integer(String value) {
        try { return Integer.parseInt(value.trim()); } catch (RuntimeException ignored) { return 0; }
    }

    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private String concise(RuntimeException exception) { return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage().replaceAll("\\s+", " ").trim(); }

    private SpaBehaviorExecutionResult result(BoundSpaBehaviorContract contract, String status, List<String> locators,
                                              List<String> actions, List<String> reasons, List<String> trace) {
        return new SpaBehaviorExecutionResult(contract.requirementId(), contract.capability(), contract.pageId(), contract.route(),
                contract.flowId(), status, locators, actions, reasons, trace);
    }

    private record AssertionSnapshot(String target, int count, boolean visible, List<String> texts) {
        private AssertionSnapshot { texts = texts == null ? List.of() : List.copyOf(texts); }
    }
}
