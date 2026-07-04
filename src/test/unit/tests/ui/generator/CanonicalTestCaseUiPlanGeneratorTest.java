package unit.tests.ui.generator;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.generator.CanonicalTestCaseUiPlanGenerator;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.List;

public class CanonicalTestCaseUiPlanGeneratorTest {

    @Test
    public void pageNamesContainScenarioOwnersOnly() {
        CanonicalTestCase testCase = new CanonicalTestCase(
                "REQ-021",
                "Page identity signal is visible",
                List.of("REQ-021"),
                List.of("Open HomePage", "Inspect collection"),
                List.of(new UiOperationIntent(UiOperationKind.INSPECT_COLLECTION, "ListPage", null)),
                List.of(new AssertionIntent(AssertionIntentKind.CONTENT_VISIBLE, "Page identity signal is visible")),
                List.of("ListPage"),
                new UiScenarioPrerequisite("HomePage", "/", false, List.of("Open HomePage")),
                "flow-REQ-021",
                "INSPECT_COLLECTION",
                "HomePage",
                "HomePage",
                "/",
                "/",
                "Open HomePage",
                UiAssertionProfile.BASIC,
                List.of("Inspect collection"),
                List.of("Page identity signal is visible"),
                List.of(),
                "requirements/demo.md [L21]"
        );
        WorkflowState state = new WorkflowState(
                "Generate UI plan",
                new RequirementInput(SourceType.FILE, "requirements/demo.md")
        );
        state.setCanonicalTestCaseBundle(new CanonicalTestCaseBundle(
                "requirements/demo.md",
                "HomePage",
                List.of("HomePage", "ListPage"),
                List.of(testCase)
        ));

        UiTestPlan plan = new CanonicalTestCaseUiPlanGenerator().generate(state);

        Assert.assertTrue(plan.pageNames().contains("HomePage"));
        Assert.assertFalse(
                plan.pageNames().contains("ListPage"),
                "Operation target pages without scenario ownership must not become POM generation targets"
        );
    }
}
