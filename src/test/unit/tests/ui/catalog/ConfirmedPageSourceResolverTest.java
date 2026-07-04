package unit.tests.ui.catalog;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.ConfirmedPageRegistry;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.ConfirmedRouteGuard;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.catalog.PageSource;
import ua.demo.agentlab.ui.catalog.StablePageRegistry;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.ui.discovery.mapping.LocatorPromotionFilter;
import ua.demo.agentlab.ui.discovery.mapping.LocatorStrategy;
import ua.demo.agentlab.ui.discovery.mapping.MappedUiKnowledgeRouteCollisionPolicy;
import ua.demo.agentlab.ui.discovery.mapping.MappedUiKnowledgeRouteFilter;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;
import ua.demo.agentlab.ui.discovery.mapping.model.PageStateHints;
import ua.demo.agentlab.ui.discovery.model.DiscoveredUiPage;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;

import java.util.List;
import java.util.Map;

public class ConfirmedPageSourceResolverTest {

    @Test
    public void profileRoutesAreStrictAndDoNotCreateMissingFallbackPages() {
        ProjectProfile profile = new ProjectProfile(
                "orangehrm",
                "OrangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
                "/auth/login",
                "/auth/login",
                "",
                "/dashboard/index",
                "",
                "",
                "/auth/login",
                "/dashboard/index",
                "",
                "",
                "",
                new OutputProfile("pages", "tests")
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver().resolve(profile);

        Assert.assertTrue(registry.findByPageName("LoginPage").isPresent());
        Assert.assertTrue(registry.findByPageName("DashboardPage").isPresent());
        Assert.assertFalse(registry.findByPageName("RecoveryPage").isPresent());
        Assert.assertFalse(registry.findByPageName("ContainerPage").isPresent());
        Assert.assertFalse(registry.findByPageName("DetailPage").isPresent());
    }

    @Test
    public void duplicateProfileRoutesPreferHighestPriorityCapability() {
        ProjectProfile profile = new ProjectProfile(
                "orangehrm",
                "OrangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
                "/auth/login",
                "/auth/login",
                "",
                "/dashboard/index",
                "",
                "",
                "/auth/login",
                "/dashboard/index",
                "/dashboard/index",
                "",
                "",
                new OutputProfile("pages", "tests")
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver().resolve(profile);

        ConfirmedPageCandidate login = registry.findByRoute("/auth/login").orElseThrow();
        ConfirmedPageCandidate dashboard = registry.findByRoute("/dashboard/index").orElseThrow();
        Assert.assertEquals(login.pageName(), "LoginPage");
        Assert.assertEquals(login.capability(), PageCapability.AUTHENTICATION);
        Assert.assertEquals(dashboard.pageName(), "DashboardPage");
        Assert.assertEquals(dashboard.capability(), PageCapability.DASHBOARD);
    }

    @Test
    public void requirementRouteCanConfirmPageCandidateWithoutProfileFallback() {
        ProjectProfile profile = new ProjectProfile(
                "demo",
                "Demo",
                "https://example.test",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile("pages", "tests")
        );
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/demo.md",
                List.of(new NormalizedRequirement(
                        "REQ-001",
                        "Login route is available",
                        "User can open /auth/login and see the authentication form.",
                        "Authentication form is visible.",
                        true,
                        false,
                        List.of("auth", "ui"),
                        new SourceReference("requirements/demo.md", 3, 3, "User can open /auth/login")
                )),
                List.of(),
                List.of()
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver()
                .resolve(profile, requirements, List.of());

        ConfirmedPageCandidate candidate = registry.findByCapability(PageCapability.AUTHENTICATION)
                .orElseThrow();
        Assert.assertEquals(candidate.pageName(), "LoginPage");
        Assert.assertEquals(candidate.route(), "/auth/login");
        Assert.assertEquals(candidate.source(), PageSource.REQUIREMENT_ROUTE);
    }

    @Test
    public void accountProfileTextDoesNotCreateSyntheticProfileRoute() {
        ProjectProfile profile = emptyProfile();
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/demo.md",
                List.of(new NormalizedRequirement(
                        "REQ-ACCOUNT",
                        "Account / Profile",
                        "Account/profile area shall show user identity or account-related navigation.",
                        "",
                        true,
                        false,
                        List.of("account", "ui"),
                        new SourceReference("requirements/demo.md", 10, 10, "Account/profile area")
                )),
                List.of(),
                List.of()
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver()
                .resolve(profile, requirements, List.of());

        Assert.assertTrue(registry.allPages().isEmpty());
        Assert.assertTrue(registry.findByRoute("/profile").isEmpty());
    }

    @Test
    public void explicitUrlContainsRequirementCanCreateSingleSegmentRoute() {
        ProjectProfile profile = emptyProfile();
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/demo.md",
                List.of(new NormalizedRequirement(
                        "REQ-ROUTE",
                        "Profile route",
                        "Current URL contains /profile after opening the account page.",
                        "Current URL contains /profile",
                        true,
                        false,
                        List.of("account", "ui"),
                        new SourceReference("requirements/demo.md", 11, 11, "Current URL contains /profile")
                )),
                List.of(),
                List.of()
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver()
                .resolve(profile, requirements, List.of());

        ConfirmedPageCandidate candidate = registry.findByRoute("/profile").orElseThrow();
        Assert.assertEquals(candidate.source(), PageSource.REQUIREMENT_ROUTE);
        Assert.assertEquals(candidate.capability(), PageCapability.RECORD_DETAILS);
    }

    @Test
    public void dbStableCacheCandidateCanBeAdaptedIntoCatalog() {
        ConfirmedPageCandidate cached = new ConfirmedPageCandidate(
                "EmployeeListPage",
                "/pim/viewEmployeeList",
                PageCapability.RECORD_LIST,
                PageSource.DB_STABLE_CACHE,
                0.91d,
                List.of("stable page cache")
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver()
                .resolve(null, null, List.of(cached));

        Assert.assertTrue(registry.findByPageName("EmployeeListPage").isPresent());
        Assert.assertEquals(registry.findByPageName("EmployeeListPage").orElseThrow().route(), "/pim/viewEmployeeList");
    }

    @Test
    public void routeEvidenceNamesRecordListPageFromConcreteRoute() {
        ProjectProfile profile = new ProjectProfile(
                "orangehrm",
                "OrangeHRM",
                "https://opensource-demo.orangehrmlive.com/web/index.php",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "/pim/viewEmployeeList",
                "",
                "",
                new OutputProfile("pages", "tests")
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver().resolve(profile);

        ConfirmedPageCandidate candidate = registry.findByCapability(PageCapability.RECORD_LIST).orElseThrow();
        Assert.assertEquals(candidate.pageName(), "EmployeeListPage");
        Assert.assertEquals(candidate.route(), "/pim/viewEmployeeList");
    }

    @Test
    public void discoverySnapshotCanConfirmPageCandidate() {
        UiDiscoverySnapshot snapshot = new UiDiscoverySnapshot(
                "demo",
                "Demo",
                "selenium",
                List.of(new DiscoveredUiPage("DashboardPage", "/dashboard/index", List.of("dashboard"), List.of(), "crawl")),
                List.of()
        );

        ConfirmedPageRegistry registry = new ConfirmedPageSourceResolver()
                .resolve(null, null, snapshot, List.of());

        ConfirmedPageCandidate candidate = registry.findByRoute("/dashboard/index").orElseThrow();
        Assert.assertEquals(candidate.pageName(), "DashboardPage");
        Assert.assertEquals(candidate.source(), PageSource.DISCOVERY_SNAPSHOT);
    }

    @Test
    public void mappedKnowledgeFilterRemovesUnconfirmedRoutesAndEvidence() {
        MappedPage login = mappedPage("login", "LoginPage", "/auth/login");
        MappedPage recovery = mappedPage("recovery", "RecoveryPage", "/forgot-password");
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(login, recovery),
                List.of(),
                List.of(
                        new PageKnowledgeGraphNode("login:node", "page", "LoginPage", "login", Map.of()),
                        new PageKnowledgeGraphNode("recovery:node", "page", "RecoveryPage", "recovery", Map.of())
                ),
                List.of(),
                List.of(
                        new PageKnowledgeVectorDocument("login:doc", "page", "login", "login", "login", List.of()),
                        new PageKnowledgeVectorDocument("recovery:doc", "page", "recovery", "recovery", "recovery", List.of())
                )
        );
        ConfirmedPageRegistry registry = new ConfirmedPageRegistry(List.of(new ConfirmedPageCandidate(
                "LoginPage",
                "/auth/login",
                PageCapability.AUTHENTICATION,
                PageSource.EXPLICIT_PROFILE,
                0.98d,
                List.of("project.route.login")
        )));

        MappedUiKnowledge filtered = new MappedUiKnowledgeRouteFilter()
                .filter(knowledge, new ConfirmedRouteGuard(registry));

        Assert.assertEquals(filtered.pages().size(), 1);
        Assert.assertEquals(filtered.pages().get(0).pageName(), "LoginPage");
        Assert.assertEquals(filtered.graphNodes().size(), 1);
        Assert.assertEquals(filtered.vectorDocuments().size(), 1);
    }

    @Test
    public void routeCollisionPolicyCanonicalizesDuplicateRoutePages() {
        MappedPage detail = mappedPage("detail", "DetailPage", "/dashboard/index");
        MappedPage index = mappedPage("index", "IndexPage", "/dashboard/index");
        MappedUiKnowledge knowledge = new MappedUiKnowledge(
                List.of(detail, index),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        ConfirmedPageRegistry registry = new ConfirmedPageRegistry(List.of(new ConfirmedPageCandidate(
                "DashboardPage",
                "/dashboard/index",
                PageCapability.DASHBOARD,
                PageSource.EXPLICIT_PROFILE,
                0.98d,
                List.of("project.route.authenticated")
        )));

        MappedUiKnowledge resolved = new MappedUiKnowledgeRouteCollisionPolicy()
                .apply(knowledge, new StablePageRegistry(registry));

        Assert.assertEquals(resolved.pages().size(), 1);
        Assert.assertEquals(resolved.pages().get(0).pageName(), "DashboardPage");
        Assert.assertEquals(resolved.pages().get(0).urlPattern(), "/dashboard/index");
    }

    @Test
    public void locatorPromotionKeepsOnlyStableSameOriginUniqueLocators() {
        LocatorCandidate strong = locator("id", "username", 0.92d, true, true, true, List.of());
        LocatorCandidate external = locator("xpath", "//a[normalize-space()='External']", 0.90d, false, true, true, List.of("EXTERNAL_ORIGIN"));
        LocatorCandidate unstable = locator("css", ".login", 0.80d, true, true, false, List.of("UNSTABLE_DISCOVERY"));
        MappedElement element = new MappedElement(
                "username",
                "username field",
                "input",
                "textbox",
                "",
                false,
                true,
                List.of(strong, external, unstable),
                List.of("type"),
                0.9d
        );
        MappedPage page = new MappedPage(
                "login",
                "LoginPage",
                "authentication",
                "/auth/login",
                "/auth/login",
                "Login",
                List.of(),
                List.of(element),
                List.of(),
                List.of(),
                List.of(),
                new PageStateHints(false, false, false, false, false, false),
                "",
                ""
        );

        MappedUiKnowledge filtered = new LocatorPromotionFilter().filterForPersistence(new MappedUiKnowledge(
                List.of(page),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));

        List<LocatorCandidate> candidates = filtered.pages().get(0).elements().get(0).locatorCandidates();
        Assert.assertEquals(candidates.size(), 1);
        Assert.assertEquals(candidates.get(0).value(), "username");
    }

    private LocatorCandidate locator(
            String strategy,
            String value,
            double score,
            boolean sameOrigin,
            boolean unique,
            boolean stable,
            List<String> risks
    ) {
        return new LocatorCandidate(
                LocatorStrategy.from(strategy),
                value,
                score,
                "test",
                "",
                "",
                "",
                "",
                "example.test",
                sameOrigin,
                unique,
                stable,
                risks
        );
    }

    private ProjectProfile emptyProfile() {
        return new ProjectProfile(
                "demo",
                "Demo",
                "https://example.test",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                new OutputProfile("pages", "tests")
        );
    }

    private MappedPage mappedPage(String pageId, String pageName, String route) {
        return new MappedPage(
                pageId,
                pageName,
                "generic",
                route,
                route,
                pageName,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new PageStateHints(false, false, false, false, false, false),
                "",
                ""
        );
    }
}
