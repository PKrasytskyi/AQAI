package ua.demo.agentlab.ui.discovery.selenium.crawler;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredInteractiveElement;

import java.util.List;

public class RequirementNavigationTargetSelectorTest {

    @Test
    public void prioritizesRequirementMatchedAuthenticatedNavigationLink() {
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/vacancies.md",
                List.of(new NormalizedRequirement(
                        "REQ-001", "User can open the Recruitment module", "Open Recruitment and Vacancies.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/vacancies.md", 1, 1, "")
                )), List.of(), List.of()
        );
        List<DiscoveredInteractiveElement> links = List.of(
                link("Admin", "/web/index.php/admin/viewAdminModule"),
                link("Recruitment", "/web/index.php/recruitment/viewRecruitmentModule")
        );

        List<DiscoveredInteractiveElement> selected = new RequirementNavigationTargetSelector().select(links, requirements);

        Assert.assertFalse(selected.isEmpty());
        Assert.assertEquals(selected.get(0).visibleText(), "Recruitment");
    }

    @Test
    public void selectsRequirementMatchedCollapsedSpaNavigationLinkForTargetedDiscovery() {
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/vacancies.md",
                List.of(new NormalizedRequirement(
                        "REQ-001", "User can open the Recruitment module", "Open Recruitment.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/vacancies.md", 1, 1, "")
                )), List.of(), List.of()
        );
        DiscoveredInteractiveElement collapsedNavigation = new DiscoveredInteractiveElement(
                "link", "Recruitment", "", "", "",
                "/web/index.php/recruitment/viewRecruitmentModule", true, false,
                new LocatorHint("Recruitment", "partialLinkText", "Recruitment")
        );

        List<DiscoveredInteractiveElement> selected = new RequirementNavigationTargetSelector().select(
                List.of(collapsedNavigation), requirements
        );

        Assert.assertEquals(selected, List.of(collapsedNavigation));
    }

    @Test
    public void prioritizesModuleRootBeforeChildNavigationLinks() {
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/vacancies.md",
                List.of(new NormalizedRequirement(
                        "REQ-001", "User can open the Recruitment module", "Open Recruitment and then vacancies.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/vacancies.md", 1, 1, "")
                )), List.of(), List.of()
        );

        List<DiscoveredInteractiveElement> selected = new RequirementNavigationTargetSelector().select(List.of(
                link("Candidates", "/web/index.php/recruitment/viewCandidates"),
                link("Recruitment", "/web/index.php/recruitment/viewRecruitmentModule"),
                link("Vacancies", "/web/index.php/recruitment/viewJobVacancy")
        ), requirements);

        Assert.assertEquals(selected.get(0).visibleText(), "Recruitment");
    }

    @Test
    public void fieldRequirementDoesNotSelectUnrelatedChangePasswordNavigation() {
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/login.md",
                List.of(new NormalizedRequirement(
                        "REQ-002", "User can enter a password", "Enter a valid password into the password field.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/login.md", 2, 2, "")
                )), List.of(), List.of()
        );

        List<DiscoveredInteractiveElement> selected = new RequirementNavigationTargetSelector().select(
                List.of(link("Change Password", "/pim/updatePassword")), requirements);

        Assert.assertTrue(selected.isEmpty());
    }

    @Test
    public void genericOpenAuthenticationPageDoesNotSelectPasswordRelatedNavigation() {
        NormalizedRequirementBundle requirements = new NormalizedRequirementBundle(
                "requirements/login.md",
                List.of(new NormalizedRequirement(
                        "REQ-001", "Authentication form is ready",
                        "Open the target page and verify username, password, and submit controls.", "",
                        true, false, List.of("functional-requirements"),
                        new SourceReference("requirements/login.md", 1, 1, ""), List.of(),
                        java.util.Map.of("target context", List.of(
                                "pageCapability: AUTHENTICATION",
                                "sourceRoute: project-profile.loginRoute",
                                "targetRoute: project-profile.loginRoute"))
                )), List.of(), List.of()
        );

        List<DiscoveredInteractiveElement> selected = new RequirementNavigationTargetSelector().select(
                List.of(link("Change Password", "/pim/updatePassword")), requirements);

        Assert.assertTrue(selected.isEmpty(),
                "Generic authentication page readiness must not create targeted module navigation");
    }

    private DiscoveredInteractiveElement link(String text, String href) {
        return new DiscoveredInteractiveElement(
                "link", text, "", "", "", href, true, true,
                new LocatorHint(text, "partialLinkText", text)
        );
    }
}
