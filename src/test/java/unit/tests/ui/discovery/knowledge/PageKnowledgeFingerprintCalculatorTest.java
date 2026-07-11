package unit.tests.ui.discovery.knowledge;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeFingerprintCalculator;

import java.util.List;

public class PageKnowledgeFingerprintCalculatorTest {

    @Test
    public void fingerprintIgnoresDynamicTextAndActionDescriptionsWhenStableLocatorsMatch() {
        PageKnowledgeFingerprintCalculator calculator = new PageKnowledgeFingerprintCalculator();

        String first = calculator.fingerprint(dashboardPage(
                "time-at-work-09-10",
                "Time at Work 09:10",
                "Open profile menu at 09:10"
        ));
        String second = calculator.fingerprint(dashboardPage(
                "time-at-work-09-45",
                "Time at Work 09:45",
                "Open profile menu at 09:45"
        ));

        Assert.assertEquals(second, first);
    }

    @Test
    public void fingerprintChangesWhenStableLocatorSurfaceChanges() {
        PageKnowledgeFingerprintCalculator calculator = new PageKnowledgeFingerprintCalculator();

        String first = calculator.fingerprint(dashboardPage(
                "menu",
                "Dashboard",
                "Open profile menu"
        ));
        String changed = calculator.fingerprint(new MappedPage(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "DASHBOARD",
                "https://example.test/web/index.php/dashboard/index",
                "/dashboard/index",
                "OrangeHRM",
                List.of(),
                List.of(element("different-menu", "User Menu", css("button[data-test='user-menu']"))),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        ));

        Assert.assertNotEquals(changed, first);
    }

    @Test
    public void fingerprintIgnoresTransientDropdownMenuItems() {
        PageKnowledgeFingerprintCalculator calculator = new PageKnowledgeFingerprintCalculator();

        String closedMenu = calculator.fingerprint(dashboardPage(
                "menu",
                "Dashboard",
                "Open profile menu"
        ));
        String openedMenu = calculator.fingerprint(new MappedPage(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "DASHBOARD",
                "https://example.test/web/index.php/dashboard/index",
                "/dashboard/index",
                "OrangeHRM",
                List.of(),
                List.of(
                        element("menu", "User Menu", css("span.oxd-userdropdown-tab")),
                        element("logout", "Logout", css("a[href='/web/index.php/auth/logout']")),
                        element("support", "Support", css("a[href='/web/index.php/help/support']")),
                        element("dropdown-item", "About", css("a.oxd-userdropdown-link"))
                ),
                List.of(),
                List.of(),
                List.of(),
                null,
                "",
                ""
        ));

        Assert.assertEquals(openedMenu, closedMenu);
    }

    private MappedPage dashboardPage(String dynamicId, String dynamicText, String actionDescription) {
        return new MappedPage(
                "web-index-php-dashboard-index",
                "DashboardPage",
                "DASHBOARD",
                "https://example.test/web/index.php/dashboard/index",
                "/dashboard/index",
                "OrangeHRM",
                List.of(),
                List.of(
                        element(dynamicId, dynamicText, css("span.oxd-userdropdown-tab")),
                        element("volatile-widget", "Random widget " + dynamicText, lowScoreCss("div.dynamic-widget"))
                ),
                List.of(),
                List.of(new MappedAction(
                        "action-" + dynamicId,
                        "openUserMenu",
                        "CLICK",
                        dynamicId,
                        "",
                        actionDescription,
                        0.90d
                )),
                List.of(),
                null,
                "",
                ""
        );
    }

    private MappedElement element(String id, String text, LocatorCandidate locator) {
        return new MappedElement(
                id,
                text,
                "BUTTON",
                "button",
                text,
                true,
                true,
                List.of(locator),
                List.of("CLICK"),
                0.90d
        );
    }

    private LocatorCandidate css(String value) {
        return new LocatorCandidate(
                LocatorStrategy.CSS,
                value,
                0.82d,
                "test",
                "button",
                "",
                "",
                "",
                "example.test",
                true,
                true,
                true,
                List.of()
        );
    }

    private LocatorCandidate lowScoreCss(String value) {
        return new LocatorCandidate(
                LocatorStrategy.CSS,
                value,
                0.40d,
                "test",
                "generic",
                "",
                "",
                "",
                "example.test",
                true,
                true,
                false,
                List.of("low-score")
        );
    }
}
