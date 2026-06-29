package ua.demo.agentlab.templates;

import ua.demo.agentlab.policy.model.FrameworkPolicy;
import ua.demo.agentlab.policy.model.GenerationPolicy;

import java.util.List;

public class DefaultTemplateRegistry implements TemplateRegistry {

    public static final String PAGE_OBJECT_TEMPLATE_ID = "selenium-page-object-template";
    public static final String UI_TEST_TEMPLATE_ID = "selenium-testng-template";

    @Override
    public TemplateDescriptor resolve(GenerationPolicy policy, ProjectContext projectContext) {
        validatePolicy(policy);
        validateProjectContext(projectContext);

        return new TemplateDescriptor(
                "default-template-registry",
                "selenium-default-bundle",
                projectContext.supportPackage(),
                projectContext.generatedPagesPackage(),
                projectContext.generatedTestsPackage(),
                projectContext.basePageType(),
                projectContext.baseTestType(),
                List.of(PAGE_OBJECT_TEMPLATE_ID, UI_TEST_TEMPLATE_ID),
                projectContext.supportTypes()
        );
    }

    private void validatePolicy(GenerationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("GenerationPolicy cannot be null");
        }

        if (policy.frameworkPolicy().uiFramework() != FrameworkPolicy.UiFramework.SELENIUM_JAVA) {
            throw new IllegalStateException(
                    "DefaultTemplateRegistry currently supports only SELENIUM_JAVA but got: "
                            + policy.frameworkPolicy().uiFramework()
            );
        }

        if (policy.frameworkPolicy().testStyle() != FrameworkPolicy.TestStyle.TESTNG) {
            throw new IllegalStateException(
                    "DefaultTemplateRegistry currently supports only TESTNG but got: "
                            + policy.frameworkPolicy().testStyle()
            );
        }
    }

    private void validateProjectContext(ProjectContext projectContext) {
        if (projectContext == null) {
            throw new IllegalArgumentException("ProjectContext cannot be null");
        }

        if (!projectContext.hasSupportType("BasePage")) {
            throw new IllegalStateException("ProjectContext must expose BasePage support before templates can be used");
        }

        if (!projectContext.hasSupportType("BaseTest")) {
            throw new IllegalStateException("ProjectContext must expose BaseTest support before templates can be used");
        }
    }
}
