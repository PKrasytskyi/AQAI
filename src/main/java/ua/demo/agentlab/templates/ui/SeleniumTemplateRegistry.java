package ua.demo.agentlab.templates.ui;

import ua.demo.agentlab.policy.model.FrameworkPolicy;
import ua.demo.agentlab.policy.model.GenerationPolicy;

public class SeleniumTemplateRegistry {

    public SeleniumTemplateBundle resolve(GenerationPolicy policy) {
        validatePolicy(policy);

        String supportPackage = "ua.demo.agentlab.core.ui";
        String generatedPagesPackage = "pages";
        String generatedTestPackage = "tests.ui";

        return new SeleniumTemplateBundle(
                "selenium-default-bundle",
                supportPackage,
                generatedPagesPackage,
                generatedTestPackage
        );
    }

    private void validatePolicy(GenerationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("GenerationPolicy cannot be null");
        }

        if (policy.frameworkPolicy().uiFramework() != FrameworkPolicy.UiFramework.SELENIUM_JAVA) {
            throw new IllegalStateException("SeleniumTemplateRegistry support only SELENIUM_JAVA but got:"
                    + policy.frameworkPolicy().uiFramework());
        }

        if (policy.frameworkPolicy().testStyle() != FrameworkPolicy.TestStyle.TESTNG) {
            throw new IllegalStateException("SeleniumTemplateRegistry currently support only TESTNG but got:"
                    + policy.frameworkPolicy().testStyle());
        }
    }
}
