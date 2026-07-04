package unit.tests.ui.discovery.knowledge;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.knowledge.MappedUiKnowledgeCurator;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeRaw;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;

public class MappedUiKnowledgeCuratorTest {

    @Test
    public void curatedKnowledgeKeepsOnlyPromotedLocatorsAndExplainsExcludedEvidence() {
        MappedUiKnowledge rawKnowledge = new MappedUiKnowledge(
                List.of(pageWithStrongAndExternalLocator()),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        MappedUiKnowledgeCurated curated = new MappedUiKnowledgeCurator()
                .curate(new MappedUiKnowledgeRaw(rawKnowledge, List.of("test-raw")));

        List<LocatorCandidate> locators = curated.knowledge().pages().get(0).elements().get(0).locatorCandidates();
        Assert.assertEquals(locators.size(), 1);
        Assert.assertEquals(locators.get(0).value(), "#username");
        Assert.assertTrue(curated.excludedEvidence().stream()
                .anyMatch(evidence -> evidence.value().contains("http://external.example/login")
                        && evidence.reason().contains("external-origin")));
    }

    private MappedPage pageWithStrongAndExternalLocator() {
        LocatorCandidate stable = new LocatorCandidate(
                LocatorStrategy.CSS,
                "#username",
                0.90d,
                "test",
                "textbox",
                "Username",
                "",
                "",
                "example.test",
                true,
                true,
                true,
                List.of()
        );
        LocatorCandidate external = new LocatorCandidate(
                LocatorStrategy.XPATH,
                "//a[@href='http://external.example/login']",
                0.80d,
                "test",
                "link",
                "External Login",
                "External Login",
                "http://external.example/login",
                "external.example",
                false,
                true,
                true,
                List.of("EXTERNAL_ORIGIN")
        );
        return new MappedPage(
                "login",
                "LoginPage",
                "authentication",
                "https://example.test/login",
                "/login",
                "Login",
                List.of(),
                List.of(new MappedElement(
                        "username",
                        "Username",
                        "input",
                        "textbox",
                        "",
                        false,
                        true,
                        List.of(stable, external),
                        List.of("type"),
                        0.95d
                )),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        );
    }
}
