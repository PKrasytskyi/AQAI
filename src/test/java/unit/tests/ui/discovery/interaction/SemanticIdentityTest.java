package unit.tests.ui.discovery.interaction;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.interaction.identity.LocatorEvidenceId;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticActionKey;
import ua.demo.agentlab.ui.discovery.interaction.identity.SemanticElementKey;

public class SemanticIdentityTest {

    @Test
    public void mergesDomOccurrenceSuffixesButKeepsLocatorAlternativesDistinct() {
        SemanticElementKey first = SemanticElementKey.of("dashboard", "state-1", "header", "user-menu-trigger");
        SemanticElementKey duplicate = SemanticElementKey.of("dashboard", "state-1", "header", "user-menu-trigger-2");

        Assert.assertEquals(first, duplicate);
        Assert.assertEquals(new SemanticActionKey(first, "OPEN_MENU"),
                new SemanticActionKey(duplicate, "open-menu"));
        Assert.assertNotEquals(
                LocatorEvidenceId.of(first, "css", "span.user-menu"),
                LocatorEvidenceId.of(first, "xpath", "//span[@role='button']"));
    }
}
