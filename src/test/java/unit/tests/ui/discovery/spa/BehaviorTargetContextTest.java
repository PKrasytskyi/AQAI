package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.BehaviorTargetContext;

public class BehaviorTargetContextTest {

    @Test
    public void parsesCompactContextWithoutLeakingAdjacentFields() {
        BehaviorTargetContext context = BehaviorTargetContext.parse(
                "targetRoute: /dashboard/index pageCapability: AUTHENTICATED_AREA "
                        + "componentCapability: FORM, CONTENT sourceRoute: /auth/login");

        Assert.assertEquals(context.value("targetRoute"), "/dashboard/index");
        Assert.assertEquals(context.value("pageCapability"), "AUTHENTICATED_AREA");
        Assert.assertEquals(context.value("componentCapability"), "FORM, CONTENT");
        Assert.assertEquals(context.value("sourceRoute"), "/auth/login");
    }

    @Test
    public void keepsFirstConfirmedValueWhenContextContainsDuplicateKey() {
        BehaviorTargetContext context = BehaviorTargetContext.parse(
                "sourceRoute: /confirmed/source\nsourceRoute: /stale/source\ntargetRoute: /target");

        Assert.assertEquals(context.value("sourceRoute"), "/confirmed/source");
        Assert.assertEquals(context.value("targetRoute"), "/target");
    }
}
