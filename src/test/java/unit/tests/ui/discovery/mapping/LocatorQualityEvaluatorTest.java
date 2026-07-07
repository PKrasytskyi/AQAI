package unit.tests.ui.discovery.mapping;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.mapping.LocatorQualityEvaluator;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;

import java.util.List;
import java.util.Map;

public class LocatorQualityEvaluatorTest {

    @Test
    public void passwordFieldRejectsSemanticallyConflictingLoginId() {
        PageElementModel password = element(
                "password",
                "PASSWORD_INPUT",
                "input",
                "password",
                "",
                "",
                "password"
        );
        PageLocatorModel locator = new PageLocatorModel("id", "login", 0.90d, "stable id candidate", true);

        LocatorCandidate candidate = new LocatorQualityEvaluator()
                .evaluate("https://example.test/login", password, locator);

        Assert.assertTrue(candidate.risks().contains("semantic-locator-conflict"));
        Assert.assertTrue(candidate.risks().contains("generic-id"));
        Assert.assertTrue(candidate.stabilityScore() < 0.10d);
        Assert.assertFalse(new LocatorQualityEvaluator().allowed(candidate));
    }

    @Test
    public void matchingNameLocatorReceivesSemanticBoostForUsernameField() {
        PageElementModel username = element(
                "username",
                "INPUT",
                "input",
                "text",
                "username",
                "",
                ""
        );
        PageLocatorModel locator = new PageLocatorModel(
                "name",
                "username",
                0.84d,
                "name attribute candidate",
                true,
                3,
                3,
                true,
                1,
                1,
                "LoginForm"
        );

        LocatorCandidate candidate = new LocatorQualityEvaluator()
                .evaluate("https://example.test/login", username, locator);

        Assert.assertTrue(candidate.stabilityScore() >= 0.88d);
        Assert.assertTrue(candidate.risks().isEmpty());
    }

    @Test
    public void hiddenTokenFieldIsForbiddenForPromptAndPersistence() {
        PageElementModel token = element(
                "token",
                "INPUT",
                "input",
                "hidden",
                "_token",
                "",
                "INPUT",
                false
        );
        PageLocatorModel locator = new PageLocatorModel("name", "_token", 0.84d, "name attribute candidate", true);

        LocatorQualityEvaluator evaluator = new LocatorQualityEvaluator();
        LocatorCandidate candidate = evaluator.evaluate("https://example.test/login", token, locator);

        Assert.assertTrue(candidate.risks().contains("hidden-or-invisible-element"));
        Assert.assertTrue(candidate.risks().contains("security-token-field"));
        Assert.assertTrue(candidate.stabilityScore() < 0.10d);
        Assert.assertFalse(evaluator.allowed(candidate));
    }

    @Test
    public void semanticSpaClassLocatorIsNotTreatedAsDynamicHash() {
        PageElementModel userMenu = new PageElementModel(
                "dashboard:user-menu",
                "BUTTON",
                "USER_MENU_TRIGGER",
                "span",
                "",
                "Yazeed Ali",
                "",
                "",
                "",
                "",
                "",
                "",
                "oxd-userdropdown-tab",
                true,
                true,
                false,
                Map.of("class", "oxd-userdropdown-tab"),
                List.of(),
                null,
                List.<PageActionModel>of(),
                0.90d
        );
        PageLocatorModel locator = new PageLocatorModel(
                "css",
                "span.oxd-userdropdown-tab",
                0.78d,
                "stable semantic class",
                true,
                3,
                3,
                true,
                1,
                1,
                "header"
        );

        LocatorCandidate candidate = new LocatorQualityEvaluator()
                .evaluate("https://example.test/dashboard/index", userMenu, locator);

        Assert.assertFalse(candidate.risks().contains("dynamic-css-hash"));
        Assert.assertTrue(candidate.stabilityScore() >= 0.75d);
    }

    private PageElementModel element(
            String id,
            String technicalType,
            String tag,
            String inputType,
            String name,
            String text,
            String semanticType
    ) {
        return element(id, technicalType, tag, inputType, name, text, semanticType, true);
    }

    private PageElementModel element(
            String id,
            String technicalType,
            String tag,
            String inputType,
            String name,
            String text,
            String semanticType,
            boolean visible
    ) {
        return new PageElementModel(
                "login:" + id,
                technicalType,
                semanticType,
                tag,
                inputType,
                text,
                id,
                name,
                "",
                "",
                "",
                "",
                "",
                visible,
                true,
                false,
                Map.of(),
                List.of(),
                null,
                List.<PageActionModel>of(),
                0.90d
        );
    }
}
