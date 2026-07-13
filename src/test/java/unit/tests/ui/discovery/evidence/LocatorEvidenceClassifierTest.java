package unit.tests.ui.discovery.evidence;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceClassifier;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.List;

public class LocatorEvidenceClassifierTest {

    private final LocatorEvidenceClassifier classifier = new LocatorEvidenceClassifier();

    @Test
    public void doesNotConfirmLocatorObservedInOnlyOneDiscoveryRun() {
        PageLocatorModel locator = new PageLocatorModel(
                "name", "username", 0.90d, "form field", true,
                1, 1, true, 1, 1, "LoginForm"
        );

        Assert.assertFalse(locator.stableAcrossRuns());
        Assert.assertEquals(classifier.classify(locator), LocatorEvidenceType.CANDIDATE_LOCATOR);
    }

    @Test
    public void doesNotConfirmNonUniqueBrowserLocatorEvenWhenItsModelFlagIsUnique() {
        PageLocatorModel locator = new PageLocatorModel(
                "css", "button[type='submit']", 0.90d, "submit", true,
                3, 3, true, 4, 4, "LoginForm"
        );

        Assert.assertEquals(classifier.classify(locator), LocatorEvidenceType.CANDIDATE_LOCATOR);
    }

    @Test
    public void confirmsLocatorOnlyWithRepeatedAndBrowserVerifiedEvidence() {
        PageLocatorModel locator = new PageLocatorModel(
                "name", "username", 0.90d, "form field", true,
                3, 3, true, 1, 1, "LoginForm"
        );

        Assert.assertEquals(classifier.classify(locator), LocatorEvidenceType.CONFIRMED_LOCATOR);
    }

    @Test
    public void doesNotPromoteScopedLocatorWithoutRepeatedDiscoveryEvidence() {
        ScopedLocatorCandidate unstable = scopedLocator(false);
        ScopedLocatorCandidate stable = scopedLocator(true);

        Assert.assertEquals(classifier.classify(unstable), LocatorEvidenceType.CANDIDATE_LOCATOR);
        Assert.assertEquals(classifier.classify(stable), LocatorEvidenceType.CONFIRMED_LOCATOR);
    }

    private ScopedLocatorCandidate scopedLocator(boolean stableAcrossRuns) {
        return new ScopedLocatorCandidate(
                "login", "LoginForm", "username", "name", "username",
                1, 1, true, true,
                1.0d, 0.90d, 0.90d, 0.90d, 0.90d,
                List.of(), stableAcrossRuns, LocatorEvidenceType.CANDIDATE_LOCATOR
        );
    }
}
