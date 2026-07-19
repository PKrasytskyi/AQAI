package unit.tests.ui.discovery.component;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.component.DynamicCssClassRiskClassifier;

public class DynamicCssClassRiskClassifierTest {

    private final DynamicCssClassRiskClassifier classifier = new DynamicCssClassRiskClassifier();

    @Test
    public void keepsStableSemanticProductClass() {
        Assert.assertFalse(classifier.isDynamic("span.oxd-userdropdown-tab"));
    }

    @Test
    public void rejectsGeneratedHashClasses() {
        Assert.assertTrue(classifier.isDynamic("button.css-1abc23"));
        Assert.assertTrue(classifier.isDynamic("div.button-a1b2c3"));
    }
}
