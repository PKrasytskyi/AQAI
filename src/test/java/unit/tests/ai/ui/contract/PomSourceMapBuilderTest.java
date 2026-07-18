package unit.tests.ai.ui.contract;

import ua.demo.agentlab.ui.generated.PomSourceMap;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.*;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import java.util.List;

class PomSourceMapBuilderTest {
    @Test void mapsMethodAndFieldFromContractWithoutInspectingGeneratedJavaText() {
        PomLocatorSpec locator = new PomLocatorSpec("username","username","name","username","input",.9d);
        PomActionSpec action = new PomActionSpec("enterUsername", List.of(new AiMethodParameterSpec("String","username")),
                List.of(new PomStepSpec(PomStepAction.CLEAR_AND_TYPE,"username","username","","")));
        PomContractSpec contract = new PomContractSpec("pom-contract-v1", new PomPageSpec("LoginPage","/login","AUTHENTICATION","openLogin"),
                List.of(locator),List.of(),List.of(action),List.of(),List.of(),List.of());
        PomSourceMap map = new PomSourceMapBuilder().build(List.of(contract), List.of(new GeneratedSourceFile("pages","LoginPage","LoginPage.java","unrelated")));
        Assert.assertEquals(map.entries().get(0).fields().get(0).fieldName(), "usernameInput");
        Assert.assertEquals(map.entries().get(0).fields().get(0).pomLocatorId(), "username");
        Assert.assertEquals(map.entries().get(0).methods().get(0).pomLocatorIds(), List.of("username"));
    }
}
