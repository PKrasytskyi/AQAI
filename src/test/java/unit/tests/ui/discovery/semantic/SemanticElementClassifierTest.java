package unit.tests.ui.discovery.semantic;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.semantic.SemanticElementClassifier;

import java.util.List;
import java.util.Map;

public class SemanticElementClassifierTest {

    private final SemanticElementClassifier classifier = new SemanticElementClassifier();

    @Test
    public void visibleTextDoesNotChangeStructuralControlType() {
        Assert.assertEquals(classifier.classify(element("h3", "", "Checkboxes")), "HEADING");
        Assert.assertEquals(classifier.classify(element("h3", "", "Dropdown List")), "HEADING");
        Assert.assertEquals(classifier.classify(element("h3", "", "File Uploader")), "HEADING");
    }

    @Test
    public void inputTypeDefinesExecutableControlType() {
        Assert.assertEquals(classifier.classify(element("input", "checkbox", "Checkbox 1")), "CHECKBOX");
        Assert.assertEquals(classifier.classify(element("input", "file", "Choose file")), "FILE_INPUT");
        Assert.assertEquals(classifier.classify(element("select", "", "Dropdown")), "SELECT");
    }

    private PageElementModel element(String tag, String inputType, String text) {
        return new PageElementModel("element", tag, "", tag, inputType, text, "", "", "", "", "",
                "", "", true, true, false, Map.of(), List.of(), null, List.of(), 0.9d);
    }
}
