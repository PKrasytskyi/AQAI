package unit.tests.ai.ui.prompt;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;
import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ai.ui.prompt.AiPageObjectPromptBuilder;
import ua.demo.agentlab.ai.ui.prompt.PromptNormalizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AiPageObjectPromptGoldenTest {

    private final AiPageObjectPromptBuilder builder = new AiPageObjectPromptBuilder();
    private final PromptNormalizer normalizer = new PromptNormalizer();

    @Test
    public void loginPromptMatchesGoldenContract() throws IOException {
        assertGolden("LoginPage", "/login", "openLogin", "golden/page-object/LoginPage.prompt.lines");
    }

    @Test
    public void homePromptMatchesGoldenContract() throws IOException {
        assertGolden("HomePage", "/", "openHome", "golden/page-object/HomePage.prompt.lines");
    }

    @Test
    public void securePromptMatchesGoldenContract() throws IOException {
        assertGolden("SecureAreaPage", "/secure", "openSecureArea", "golden/page-object/SecureAreaPage.prompt.lines");
    }

    @Test
    public void promptDoesNotContainKnownQualityRegressions() {
        String prompt = prompt("LoginPage", "/login", "openLogin");
        String normalized = normalizer.normalize(prompt);

        Assert.assertFalse(normalized.contains("expectedValue=null"), "Prompt must not leak null expected values");
        Assert.assertFalse(normalized.contains("sameOrigin=false"), "Prompt must not expose external-origin locator facts");
        Assert.assertFalse(normalized.contains("a[href='http://"), "Prompt must not expose absolute external href locators");
        Assert.assertFalse(normalized.contains("elements.click("), "Prompt must not include Java method bodies");
        Assert.assertFalse(normalized.contains("elements.clearAndType("), "Prompt must not include Java method bodies");
        Assert.assertEquals(count(normalized, "Page capability contract:"), 1, "Prompt must not duplicate capability contract header");
    }

    @Test
    public void baselineSpecIsSummarizedWithoutJavaBodies() {
        AiPageObjectSpec baseline = new AiPageObjectSpec(
                "LoginPage",
                "/login",
                "openLogin",
                List.of(new AiLocatorSpec("loginButton", "login button", "css", "button[type='submit']")),
                List.of(new AiMethodSpec(
                        "void",
                        "clickLoginButton",
                        List.of(),
                        "elements.click(loginButton);",
                        List.of()
                ))
        );

        String prompt = builder.buildForPage(emptyContext(), "LoginPage", List.of(), baseline);

        Assert.assertTrue(prompt.contains("methodSignatures=[void clickLoginButton()]"));
        Assert.assertFalse(prompt.contains("elements.click(loginButton);"));
    }

    private void assertGolden(String pageName, String route, String openMethod, String resource) throws IOException {
        String prompt = normalizer.normalize(prompt(pageName, route, openMethod));
        for (String requiredLine : goldenLines(resource)) {
            Assert.assertTrue(
                    prompt.contains(requiredLine),
                    "Expected normalized prompt for " + pageName + " to contain golden line: " + requiredLine
            );
        }
    }

    private String prompt(String pageName, String route, String openMethod) {
        AiPageObjectSpec baseline = new AiPageObjectSpec(pageName, route, openMethod, List.of(), List.of());
        return builder.buildForPage(emptyContext(), pageName, List.of(), baseline);
    }

    private AiContextPackage emptyContext() {
        return new AiContextPackage(
                "Generate canonical UI Page Object prompts",
                null,
                null,
                null,
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
    }

    private List<String> goldenLines(String resource) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IOException("Missing golden resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
        }
    }

    private int count(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    @Test
    public void promptContainsCurrentPageObjectSchemaVersion() {
        Assert.assertTrue(
                prompt("LoginPage", "/login", "openLogin").contains(LlmOutputSchemaVersion.POM_CONTRACT),
                "Prompt must include the current POM contract schema version"
        );
    }
}
