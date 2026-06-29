package ua.demo.agentlab.templates;

import ua.demo.agentlab.templates.ui.SeleniumTemplateBundle;

import java.util.List;

public record TemplateDescriptor(
        String registryId,
        String bundleId,
        String supportPackage,
        String generatedPagesPackage,
        String generatedTestsPackage,
        String basePageType,
        String baseTestType,
        List<String> templateIds,
        List<String> supportTypes
) {

    public TemplateDescriptor {
        if (registryId == null || registryId.isBlank()) {
            throw new IllegalArgumentException("registryId cannot be blank");
        }
        if (bundleId == null || bundleId.isBlank()) {
            throw new IllegalArgumentException("bundleId cannot be blank");
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

        templateIds = templateIds == null ? List.of() : List.copyOf(templateIds);
        supportTypes = supportTypes == null ? List.of() : List.copyOf(supportTypes);
    }

    public SeleniumTemplateBundle toBundle() {
        return new SeleniumTemplateBundle(
                bundleId,
                supportPackage,
                generatedPagesPackage,
                generatedTestsPackage
        );
    }
}
