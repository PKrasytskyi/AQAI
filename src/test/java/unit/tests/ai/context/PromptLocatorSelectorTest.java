package unit.tests.ai.context;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.PromptLocatorSelector;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;

import java.util.List;

public class PromptLocatorSelectorTest {

    @Test
    public void selectorPrefersSemanticFieldLocatorOverGenericId() {
        LocatorCandidate genericId = locator(LocatorStrategy.ID, "login", 0.88d, List.of("generic-id"));
        LocatorCandidate semanticName = locator(LocatorStrategy.NAME, "password", 0.82d, List.of());

        LocatorCandidate selected = new PromptLocatorSelector()
                .select(List.of(genericId, semanticName), "password", "password input", "")
                .orElseThrow();

        Assert.assertEquals(selected.value(), "password");
    }

    @Test
    public void selectorIgnoresExternalLocators() {
        LocatorCandidate external = new LocatorCandidate(
                LocatorStrategy.XPATH,
                "//a[normalize-space()='External']",
                0.90d,
                "test",
                "link",
                "External",
                "External",
                "https://external.example",
                "external.example",
                false,
                true,
                true,
                List.of("external-origin")
        );
        LocatorCandidate sameOrigin = locator(LocatorStrategy.CSS, "button[type='submit']", 0.78d, List.of());

        LocatorCandidate selected = new PromptLocatorSelector()
                .select(List.of(external, sameOrigin), "login button", "button", "Login")
                .orElseThrow();

        Assert.assertEquals(selected.value(), "button[type='submit']");
    }

    @Test
    public void selectorIgnoresHiddenSecurityTokenLocators() {
        LocatorCandidate token = locator(
                LocatorStrategy.NAME,
                "_token",
                0.95d,
                List.of("hidden-or-invisible-element", "security-token-field")
        );
        LocatorCandidate submit = locator(LocatorStrategy.CSS, "button[type='submit']", 0.78d, List.of());

        LocatorCandidate selected = new PromptLocatorSelector()
                .select(List.of(token, submit), "login button", "button", "Login")
                .orElseThrow();

        Assert.assertEquals(selected.value(), "button[type='submit']");
    }

    private LocatorCandidate locator(LocatorStrategy strategy, String value, double score, List<String> risks) {
        return new LocatorCandidate(
                strategy,
                value,
                score,
                "test",
                "",
                value,
                "",
                "",
                "example.test",
                true,
                true,
                true,
                risks
        );
    }
}
