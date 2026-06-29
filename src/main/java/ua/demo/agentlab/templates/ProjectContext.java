package ua.demo.agentlab.templates;

import java.nio.file.Path;
import java.util.List;

public record ProjectContext(
        Path workspaceRoot,
        String supportPackage,
        String generatedPagesPackage,
        String generatedTestsPackage,
        String basePageType,
        String baseTestType,
        List<String> supportTypes,
        boolean hasTestDataProviderLayer
) {

    public ProjectContext {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("workspaceRoot cannot be null");
        }
        if (supportPackage == null || supportPackage.isBlank()) {
            throw new IllegalArgumentException("supportPackage cannot be blank");
        }
        if (generatedPagesPackage == null || generatedPagesPackage.isBlank()) {
            throw new IllegalArgumentException("generatedPagesPackage cannot be blank");
        }
        if (generatedTestsPackage == null || generatedTestsPackage.isBlank()) {
            throw new IllegalArgumentException("generatedTestsPackage cannot be blank");
        }
        if (basePageType == null || basePageType.isBlank()) {
            throw new IllegalArgumentException("basePageType cannot be blank");
        }
        if (baseTestType == null || baseTestType.isBlank()) {
            throw new IllegalArgumentException("baseTestType cannot be blank");
        }

        workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        supportTypes = supportTypes == null ? List.of() : List.copyOf(supportTypes);
    }

    public boolean hasSupportType(String simpleName) {
        return supportTypes.contains(simpleName);
    }

    public String supportType(String simpleName) {
        return supportPackage + "." + simpleName;
    }
}
