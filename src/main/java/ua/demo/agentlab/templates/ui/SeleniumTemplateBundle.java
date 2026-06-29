package ua.demo.agentlab.templates.ui;

public record SeleniumTemplateBundle(
        String bundleId,
        String supportPackage,
        String generatedPagesPackage,
        String generatedTestsPackage
) {

    public SeleniumTemplateBundle {
        bundleId = requireText(bundleId, "bundleId");
        supportPackage = requireText(supportPackage, "supportPackage");
        generatedPagesPackage = requireText(generatedPagesPackage, "generatedPagesPackage");
        generatedTestsPackage = requireText(generatedTestsPackage, "generatedTestsPackage");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }

        return value.trim();
    }
}
