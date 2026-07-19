package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.LiveComponentFlowVerifier;
import ua.demo.agentlab.ui.discovery.spa.model.*;
import java.util.List;

public class LiveComponentFlowVerifierTest {
    @Test
    public void acceptsChangedResultSnapshot() {
        var verifier=new LiveComponentFlowVerifier();
        var contract=new FlowPostconditionContract(FlowPostconditionType.RESULTS_CHANGED,List.of("rows"),"",true,"");
        var before=List.of(new LiveComponentFlowVerifier.StateSnapshot("rows",1,true,List.of("A")));
        var after=List.of(new LiveComponentFlowVerifier.StateSnapshot("rows",1,true,List.of("B")));
        Assert.assertEquals(verifier.evaluate(contract,before,after).status(),"PASSED");
    }
}
