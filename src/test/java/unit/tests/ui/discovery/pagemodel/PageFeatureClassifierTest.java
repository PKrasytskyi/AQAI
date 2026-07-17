package unit.tests.ui.discovery.pagemodel;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.pagemodel.PageFeatureClassifier;

import java.util.List;

public class PageFeatureClassifierTest {

    private final PageFeatureClassifier classifier = new PageFeatureClassifier();

    @Test
    public void classifiesRecruitmentCandidatesAsRecordListBeforeGenericViewDetails() {
        Assert.assertEquals(
                classifier.classify(List.of(), "/web/index.php/recruitment/viewCandidates", "Recruitment"),
                "catalog"
        );
    }

    @Test
    public void keepsDashboardClassificationAheadOfGenericDetailSignals() {
        Assert.assertEquals(
                classifier.classify(List.of("details-navigation"), "/web/index.php/dashboard/index", "Dashboard"),
                "dashboard"
        );
    }
}
