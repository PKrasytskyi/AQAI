package unit.tests.ui.discovery.semantic;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.semantic.ActionCandidateClassifier;

import java.util.List;
import java.util.Map;

public class ActionCandidateClassifierTest {

    private final ActionCandidateClassifier classifier = new ActionCandidateClassifier();

    @Test
    public void doesNotAssignInteractiveActionsToHeading() {
        var actions = classifier.classify(element("heading", "h3", "", "Checkboxes page", ""), "HEADING");
        Assert.assertTrue(actions.stream().noneMatch(action -> List.of(
                "CHECK", "UNCHECK", "PAGINATE", "SUBMIT_FORM", "CLICK").contains(action.action())));
    }

    @Test
    public void keepsOnlyRoleCompatibleCheckboxAndSubmitActions() {
        var checkboxActions = classifier.classify(element("choice", "input", "checkbox", "", ""), "CHECKBOX");
        Assert.assertEquals(checkboxActions.stream().map(action -> action.action()).sorted().toList(),
                List.of("CHECK", "UNCHECK"));

        var buttonActions = classifier.classify(element("login", "button", "submit", "Login", ""), "BUTTON");
        Assert.assertTrue(buttonActions.stream().anyMatch(action -> action.action().equals("CLICK")));
        Assert.assertTrue(buttonActions.stream().anyMatch(action -> action.action().equals("SUBMIT_FORM")));
        Assert.assertTrue(buttonActions.stream().noneMatch(action -> action.action().equals("TYPE")));
    }

    private PageElementModel element(String id, String tag, String inputType, String text, String role) {
        return new PageElementModel(id, tag, "", tag, inputType, text, id, id, "", "", role,
                "", "", true, true, false, Map.of(), List.of(), null, List.of(), 0.9d);
    }
}
