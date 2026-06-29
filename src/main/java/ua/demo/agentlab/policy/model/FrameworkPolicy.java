package ua.demo.agentlab.policy.model;

public record FrameworkPolicy(
        UiFramework uiFramework,
        ApiFramework apiFramework,
        TestStyle testStyle,
        boolean usePageObjectModel,
        boolean useLayeredTests,
        boolean generateBasePage,
        boolean generateBaseTest
) {

    public enum UiFramework {
        PLAYWRIGHT_JAVA,
        SELENIUM_JAVA
    }

    public enum ApiFramework {
        REST_ASSURED,
        PLAYWRIGHT_API,
        JAVA_HTTP_CLIENT
    }

    public enum TestStyle {
        JUNIT5,
        TESTNG
    }
}
