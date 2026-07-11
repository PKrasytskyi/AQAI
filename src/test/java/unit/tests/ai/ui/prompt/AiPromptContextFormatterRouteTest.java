package unit.tests.ai.ui.prompt;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.ui.prompt.AiPromptContextFormatter;
import ua.demo.agentlab.config.OutputProfile;
import ua.demo.agentlab.config.ProjectProfile;

import java.util.List;

public class AiPromptContextFormatterRouteTest {

    @Test
    public void compactContextFormatsConfirmedRoutesByCapability() {
        ProjectProfile profile = new ProjectProfile(
                "demo",
                "Demo App",
                "https://example.test",
                "/",
                "/auth/login",
                "",
                "/dashboard/index",
                "",
                "/records/details",
                "",
                "",
                "/records",
                "",
                "/basket",
                new OutputProfile("pages", "tests")
        );
        AiContextPackage context = new AiContextPackage(
                "Generate one scoped Page Object contract",
                null,
                null,
                profile,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        String formatted = new AiPromptContextFormatter().summarizeCompactContext(context);

        Assert.assertTrue(formatted.contains("AUTHENTICATION=/auth/login"));
        Assert.assertTrue(formatted.contains("DASHBOARD=/dashboard/index"));
        Assert.assertTrue(formatted.contains("RECORD_LIST=/records"));
        Assert.assertTrue(formatted.contains("RECORD_DETAILS=/records/details"));
        Assert.assertTrue(formatted.contains("CONTAINER=/basket"));
        Assert.assertFalse(formatted.contains("login=/auth/login"));
        Assert.assertFalse(formatted.contains("catalog=/records"));
        Assert.assertFalse(formatted.contains("details=/records/details"));
        Assert.assertFalse(formatted.contains("cart=/basket"));
    }
}
