package ua.demo.agentlab.templates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultProjectContextScanner implements ProjectContextScanner {

    private static final String SUPPORT_PACKAGE = "ua.demo.agentlab.core.ui";
    private static final String GENERATED_PAGES_PACKAGE = "ua.demo.agentlab.ui.generated.pages";
    private static final String GENERATED_TESTS_PACKAGE = "ua.demo.agentlab.ui.generated.tests";

    private final Path workspaceRoot;

    public DefaultProjectContextScanner(Path workspaceRoot) {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    @Override
    public ProjectContext scan() {
        List<String> supportTypes = new ArrayList<>();

        registerIfExists(supportTypes, "BasePage", "src/main/java/ua/demo/agentlab/core/ui/BasePage.java");
        registerIfExists(supportTypes, "BaseTest", "src/main/java/ua/demo/agentlab/core/ui/BaseTest.java");
        registerIfExists(
                supportTypes,
                "ElementActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/ElementActions.java"
        );
        registerIfExists(
                supportTypes,
                "DropdownActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/DropdownActions.java"
        );
        registerIfExists(
                supportTypes,
                "AlertActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/AlertActions.java"
        );
        registerIfExists(
                supportTypes,
                "FrameActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/FrameActions.java"
        );
        registerIfExists(
                supportTypes,
                "WindowActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/WindowActions.java"
        );
        registerIfExists(
                supportTypes,
                "NavigationActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/NavigationActions.java"
        );
        registerIfExists(
                supportTypes,
                "JavascriptActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/JavascriptActions.java"
        );
        registerIfExists(
                supportTypes,
                "AdvancedUserActions",
                "src/main/java/ua/demo/agentlab/core/ui/actions/AdvancedUserActions.java"
        );
        registerIfExists(
                supportTypes,
                "WaitActions",
                "src/main/java/ua/demo/agentlab/core/ui/wait/WaitActions.java"
        );
        registerIfExists(
                supportTypes,
                "TestDataProvider",
                "src/main/java/ua/demo/agentlab/core/data/TestDataProvider.java"
        );
        registerIfExists(
                supportTypes,
                "PropertiesTestDataProvider",
                "src/main/java/ua/demo/agentlab/core/data/PropertiesTestDataProvider.java"
        );
        registerIfExists(
                supportTypes,
                "UserCredentials",
                "src/main/java/ua/demo/agentlab/core/data/UserCredentials.java"
        );
        registerIfExists(
                supportTypes,
                "ScenarioData",
                "src/main/java/ua/demo/agentlab/core/data/ScenarioData.java"
        );
        registerIfExists(
                supportTypes,
                "UiRuntimeConfig",
                "src/main/java/ua/demo/agentlab/core/config/UiRuntimeConfig.java"
        );
        registerIfExists(
                supportTypes,
                "PropertiesUiRuntimeConfig",
                "src/main/java/ua/demo/agentlab/core/config/PropertiesUiRuntimeConfig.java"
        );
        registerIfExists(
                supportTypes,
                "DriverFactory",
                "src/main/java/ua/demo/agentlab/core/ui/driver/DriverFactory.java"
        );
        registerIfExists(
                supportTypes,
                "DefaultDriverFactory",
                "src/main/java/ua/demo/agentlab/core/ui/driver/DefaultDriverFactory.java"
        );

        boolean hasTestDataLayer =
                supportTypes.contains("TestDataProvider")
                        && supportTypes.contains("PropertiesTestDataProvider");

        return new ProjectContext(
                workspaceRoot,
                SUPPORT_PACKAGE,
                GENERATED_PAGES_PACKAGE,
                GENERATED_TESTS_PACKAGE,
                "ua.demo.agentlab.core.ui.BasePage",
                "ua.demo.agentlab.core.ui.BaseTest",
                supportTypes,
                hasTestDataLayer
        );
    }

    private void registerIfExists(List<String> supportTypes, String simpleName, String relativePath) {
        if (Files.exists(workspaceRoot.resolve(relativePath))) {
            supportTypes.add(simpleName);
        }
    }
}
