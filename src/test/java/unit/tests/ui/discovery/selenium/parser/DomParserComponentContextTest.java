package unit.tests.ui.discovery.selenium.parser;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.parser.DomParser;

import java.util.List;
import java.util.Map;

public class DomParserComponentContextTest {

    @Test
    public void retainsNearestAriaContainerForComponentDetection() {
        String html = "<header id='top'><ul role='menu'><li><a href='/logout'>Logout</a></li></ul></header>";
        RawPageSnapshot snapshot = new RawPageSnapshot("https://example.test/dashboard", "", "", html, html,
                "", "", "", "", List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());

        var logout = new DomParser().parse(snapshot).stream()
                .filter(element -> "Logout".equals(element.text())).findFirst().orElseThrow();

        Assert.assertEquals(logout.attributes().get("agentlab.container.role"), "menu");
        Assert.assertTrue(logout.attributes().get("agentlab.dom.ancestry").contains("header#top"));
    }

    @Test
    public void extractsLabelScopedCustomSelectAndDivBasedResultsTable() {
        String html = "<form><div class='field-group'><label>Job Title</label>"
                + "<div class='select-wrapper'><div class='select-input' tabindex='0'>Select</div></div></div>"
                + "<button type='submit'>Search</button></form>"
                + "<div class='results-grid' role='table'><div role='row'><div role='cell'>Engineer</div></div></div>";
        RawPageSnapshot snapshot = new RawPageSnapshot("https://example.test/jobs", "", "", html, html,
                "", "", "", "", List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());

        var elements = new DomParser().parse(snapshot);
        var customSelect = elements.stream()
                .filter(element -> "custom-select".equals(element.attributes().get("agentlab.field.kind")))
                .findFirst().orElseThrow();
        var results = elements.stream().filter(element -> "table".equals(element.role()))
                .findFirst().orElseThrow();

        Assert.assertEquals(customSelect.attributes().get("agentlab.field.label"), "Job Title");
        Assert.assertTrue(customSelect.attributes().get("agentlab.field.locator.xpath").contains("Job Title"));
        Assert.assertEquals(results.attributes().get("agentlab.container.key"), "role=table:tag=div");
    }
}
